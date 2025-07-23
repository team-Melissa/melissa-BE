package com.melissa.diary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.DailyChatLog;
import com.melissa.diary.domain.Thread;
import com.melissa.diary.domain.enums.Mood;
import com.melissa.diary.domain.enums.Role;
import com.melissa.diary.domain.enums.UsageCost;
import com.melissa.diary.event.ThreadImageEvent;
import com.melissa.diary.repository.ThreadRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.web.dto.ThreadSummaryResponseDTO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ThreadSummaryServiceV2 {

    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final ChatClient summaryClient;

    private final QuotaService quotaService;

    private final ApplicationEventPublisher publisher;

    private final ObjectMapper objectMapper = new ObjectMapper();
    public ThreadSummaryServiceV2(UserRepository userRepository,
                                ThreadRepository threadRepository,
                                @Qualifier("summaryClient")
                                ChatClient summaryClient,
                                QuotaService quotaService,
                                ApplicationEventPublisher publisher
    ) {
        this.userRepository = userRepository;
        this.threadRepository = threadRepository;
        this.summaryClient = summaryClient;
        this.quotaService = quotaService;
        this.publisher = publisher;
    }

    // ────────────────────────────────────────────────────────────
    // V2: 요약 즉시, 이미지는 @Async 로 처리
    // ────────────────────────────────────────────────────────────
    @Transactional
    public ThreadSummaryResponseDTO.dailySummaryResponseDTO generateImmediateSummaryV2(
            Long userId, int year, int month, int day) {

        // 권한·쿼터 체크
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
        quotaService.checkAndConsume(user, UsageCost.SUMMARY);

        // Thread + 채팅 로그 조회
        ThreadSummaryServiceV2.ThreadSummaryData data = fetchThreadSummaryData(userId, year, month, day);
        if (data == null) throw new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND);

        Thread thread = data.getThread();
        // 1분 이내 중복 요청 무조건 차단 (lastSummaryRequestAt 기준)
        if (thread.getLastSummaryRequestAt() != null &&
            thread.getLastSummaryRequestAt().isAfter(java.time.LocalDateTime.now().minusMinutes(1))) {
            throw new ErrorHandler(ErrorStatus.THREAD_TOO_MANY_REQUESTS);
        }
        thread.setLastSummaryRequestAt(java.time.LocalDateTime.now());
        List<DailyChatLog> logs = data.getLogs().stream()
                .filter(l -> Role.USER.equals(l.getRole()))
                .collect(Collectors.toList());
        if (logs.size() <= 2) throw new ErrorHandler(ErrorStatus.CHAT_NOT_FOUND);

        // LLM 호출 → 요약 JSON
        String llmResp = callLLMForSummary(
                buildSummaryPrompt(buildChatLogsPrompt(logs)));

        // 요약 JSON 기반 DTO 작성 (imageUrl=null 상태)
        ThreadSummaryResponseDTO.dailySummaryResponseDTO dto =
                buildDtoFromLlm(thread, llmResp);

        // thread 저장 (imageUrl=null)
        updateThreadSummary(thread.getId(), llmResp, null);

        // 이미지 처리 비동기 시작
        publisher.publishEvent(new ThreadImageEvent(thread.getId()));

        // 7) 즉시 응답
        return dto;
    }

    // 스레드 조회로직 트랜잭션 분리
    @Transactional(readOnly = true)
    public ThreadSummaryServiceV2.ThreadSummaryData fetchThreadSummaryData(Long userId, int year, int month, int day) {
        Thread thread = threadRepository.findByUserIdAndYearAndMonthAndDay(userId, year, month, day)
                .orElse(null);
        if (thread == null) return null;
        // lazy 연관관계 초기화: DailyChatLog 목록 조회
        List<DailyChatLog> logs = thread.getDailyChatLogs();
        return new ThreadSummaryServiceV2.ThreadSummaryData(thread, logs);
    }

    // llm 호출 분리
    private String callLLMForSummary(String prompt) {
        return summaryClient.prompt().user(prompt).call().content();
    }

    // LLM 응답(JSON)을 파싱하여 스레드 요약 정보를 업데이트
    private void parseAndUpdateThread(Thread thread, String llmResponse) {
        try {
            int startIndex = llmResponse.indexOf("{");
            int endIndex = llmResponse.lastIndexOf("}");
            if (startIndex == -1 || endIndex == -1) {
                throw new ErrorHandler(ErrorStatus.CALENDAR_PROCESSING_FAILED);
            }
            String jsonContent = llmResponse.substring(startIndex, endIndex + 1);
            JsonNode node = objectMapper.readTree(jsonContent);

            String summaryTitle = node.has("title") ? node.get("title").asText() : null;
            String moodStr = node.has("mood") ? node.get("mood").asText() : null;
            String summaryContent = node.has("story") ? node.get("story").asText() : null;
            String hashTag1 = node.has("hashTag1") ? node.get("hashTag1").asText() : null;
            String hashTag2 = node.has("hashTag2") ? node.get("hashTag2").asText() : null;

            // Mood enum 매핑 (정상 값이 아니면 기본값 HAPPY)
            Mood moodEnum = Mood.HAPPY;
            if (moodStr != null) {
                try {
                    moodEnum = Mood.valueOf(moodStr.toUpperCase().trim());
                } catch (IllegalArgumentException e) {
                    moodEnum = Mood.HAPPY;
                }
            }

            thread.setSummaryTitle(summaryTitle);
            thread.setMood(moodEnum);
            thread.setSummaryContent(summaryContent);
            thread.setHashtag1(hashTag1);
            thread.setHashtag2(hashTag2);
            thread.setSummaryCreatedAt(LocalDateTime.now());
        } catch (IOException e) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_PROCESSING_FAILED);
        }
    }

    // 스레드 업데이트해서 저장하는 로직 트랜잭션 분리
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateThreadSummary(Long threadId, String llmResponse, String imageUrl) {
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND));
        // LLM 응답을 파싱하여 스레드 요약 정보 업데이트
        parseAndUpdateThread(thread, llmResponse);
        thread.setImageUrl(imageUrl);
        threadRepository.save(thread);
    }

    // 채팅로그를 프롬프트에 넣도록 변환
    private String buildChatLogsPrompt(List<DailyChatLog> logs) {
        return logs.stream()
                .map(log -> {
                    if (log.getRole() == Role.USER) {
                        return "[User] " + log.getContent();
                    } else {
                        return "[Assistant] " + log.getContent();
                    }
                })
                .collect(Collectors.joining("\n"));
    }

    // 요약 프롬프트 생성
    private String buildSummaryPrompt(String chatLogs) {
        return """
                오늘의 채팅 로그입니다: %s

                위 대화를 오늘의 채팅로그를 기반으로 일기 형식으로 요약해 주세요.
                - mood(HAPPY, SAD, TIRED, ANGRY, RELAX 중 하나)
                - title(30자 이하, 유쾌하고 흥미로운 표현, 이모티콘 미사용)
                - story(300자 이하, 일기 형식)
                - hashTag1, hashTag2(주제 연관 해시태그)
                                
                아래 JSON 형식으로 꼭 답변해주세요:
                                
                {
                  "mood": "...",
                  "title": "...",
                  "story": "...",
                  "hashTag1": "...",
                  "hashTag2": "..."
                }
                """.formatted(chatLogs);
    }

    /* 요약 JSON → DTO (imageS3 는 null) */
    private ThreadSummaryResponseDTO.dailySummaryResponseDTO buildDtoFromLlm(Thread t, String resp){
        Thread temp = new Thread();          // 임시 객체
        parseAndUpdateThread(temp, resp);    // 동일 로직 재사용
        return ThreadSummaryResponseDTO.dailySummaryResponseDTO.builder()
                .year(t.getYear())
                .month(t.getMonth())
                .day(t.getDay())
                .summaryTitle(temp.getSummaryTitle())
                .summaryContent(temp.getSummaryContent())
                .summaryMood(temp.getMood() == null ? null : temp.getMood().name())
                .hashTag1(temp.getHashtag1())
                .hashTag2(temp.getHashtag2())
                .imageS3(null)               // 처음엔 null
                .build();
    }

    /**
     * 내부 DTO 클래스 – DB에서 조회한 Thread와 채팅 로그를 담기 위함
     */
    @Getter
    protected static class ThreadSummaryData {
        private final Thread thread;
        private final List<DailyChatLog> logs;

        public ThreadSummaryData(Thread thread, List<DailyChatLog> logs) {
            this.thread = thread;
            this.logs = logs;
        }
    }
}
