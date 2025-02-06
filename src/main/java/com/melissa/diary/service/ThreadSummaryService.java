package com.melissa.diary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melissa.diary.ai.ImageGenerator;
import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.converter.ThreadSummaryConverter;
import com.melissa.diary.domain.*;
import com.melissa.diary.domain.Thread;
import com.melissa.diary.domain.enums.Mood;
import com.melissa.diary.domain.enums.Role;
import com.melissa.diary.repository.ThreadRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.repository.UserSettingRepository;
import com.melissa.diary.web.dto.ThreadSummaryResponseDTO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ThreadSummaryService {

    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final UserSettingRepository userSettingRepository;
    private final ChatClient summaryClient;
    private final ImageGenerator imageGenerator;

    private final ObjectMapper objectMapper = new ObjectMapper();
    public ThreadSummaryService(UserRepository userRepository,
                                ThreadRepository threadRepository,
                                UserSettingRepository userSettingRepository,
                                @Qualifier("summaryClient")
                                ChatClient summaryClient,
                                ImageGenerator imageGenerator) {
        this.userRepository = userRepository;
        this.threadRepository = threadRepository;
        this.userSettingRepository = userSettingRepository;
        this.summaryClient = summaryClient;
        this.imageGenerator = imageGenerator;
    }

    /**
     * 매 정각 실행 – 사용자가 설정한 시간과 현재 시각의 시(hour)가 동일한 경우에만 요약 생성
     * 
     * 변경사항 : 민석이형의 요청으로 스케줄러는 보수적 접근으로 사용 -> 채팅로그는 존재하지만 채팅방을 나가는 유저의 개개인 트리거가 작동하지 않은 경우 추가
     */
    @Scheduled(cron = "0 0 * * * *")
    public void generateDailySummaryForAllUsers() {
        LocalTime currentTime = LocalTime.now();
        List<User> users = userRepository.findAll();
        for (User user : users) {
            UserSetting userSetting = userSettingRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.SETTING_NOT_FOUND));
            LocalTime userSummaryTime = userSetting.getSleepTime().toLocalTime();
            if (currentTime.getHour() == userSummaryTime.getHour()) {
                try {
                    generateDailySummaryForUserScheduled(user.getId());
                } catch (Exception e) {
                    log.error("[ThreadSummary] 요약 생성 실패. userId=" + user.getId(), e);
                }
            }
        }
    }

    /**
     * 특정 유저의 오늘(또는 원하는 날짜)의 Thread에 대해 요약을 생성한다.
     *
     * → DB 조회/데이터 초기화는 짧은 트랜잭션(read-only)으로 처리한 후,
     *    외부 API 호출(LLM, 이미지 생성)은 트랜잭션 외부에서 실행하고,
     *    최종 업데이트는 별도의 신규 트랜잭션(REQUIRES_NEW)에서 처리한다.
     */
    public void generateDailySummaryForUserScheduled(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();
        int hour = now.getHour();
        // 오전 8시 이전이면 전날로 처리
        if (hour < 8) {
            day -= 1;
        }

        // DB에서 스레드와 채팅 로그 초기화 (read-only 트랜잭션로 분리)
        ThreadSummaryData summaryData = fetchThreadSummaryData(userId, year, month, day);
        if (summaryData == null) {
            log.warn("[ThreadSummary] 해당 날짜 스레드가 없습니다. userId={}, {}-{}-{}", userId, year, month, day);
            return;
        }
        Thread thread = summaryData.getThread();
        List<DailyChatLog> logs = summaryData.getLogs();
        if (logs == null || logs.isEmpty()) {
            log.warn("[ThreadSummary] 채팅 로그가 없어 요약 불필요. userId={}, {}-{}-{}", userId, year, month, day);
            return;
        }
        // 이미 요약 내용이 존재하면 스케줄러에서는 실행하지 않도록 수정!!
        if (thread.getSummaryContent() != null && !thread.getSummaryContent().trim().isEmpty()) {
            log.info("[ThreadSummary] 요약 내용이 이미 존재하여 스케줄러 생략. userId={}, {}-{}-{}", userId, year, month, day);
            return;
        }
        String chatLogsForPrompt = buildChatLogsPrompt(logs);
        String prompt = buildSummaryPrompt(chatLogsForPrompt);

        // 외부 LLM 호출 및 이미지 생성 (트랜잭션 외부)
        String llmResponse = callLLMForSummary(prompt);
        String imagePrompt = buildImagePrompt(summaryData.getThread());
        String imageUrl = imageGenerator.genProfileImage(imagePrompt);

        // 신규 트랜잭션에서 스레드 업데이트 (요약 결과 및 이미지 URL 반영)
        updateThreadSummary(thread.getId(), llmResponse, imageUrl);
    }

    /**
     * 유저가 API를 호출하면 즉각 실행되어 무조건 요약 데이터를 덮어씌우고,
     * 최신 스레드를 반환합니다.
     */
    @Transactional
    public ThreadSummaryResponseDTO.dailySummaryResponseDTO generateImmediateSummary(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();
        // 오전 8시 이전이면 전날로 처리
        if (now.getHour() < 8) {
            day -= 1;
        }

        ThreadSummaryData summaryData = fetchThreadSummaryData(userId, year, month, day);
        if (summaryData == null) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND);
        }
        Thread thread = summaryData.getThread();
        List<DailyChatLog> logs = summaryData.getLogs();
        if (logs == null || logs.isEmpty()) {
            throw new ErrorHandler(ErrorStatus.CHAT_NOT_FOUND);
        }

        // 기존 요약 내용과 관계없이 무조건 덮어씌웁니다.
        String chatLogsForPrompt = buildChatLogsPrompt(logs);
        String prompt = buildSummaryPrompt(chatLogsForPrompt);

        String llmResponse = callLLMForSummary(prompt);
        String imagePrompt = buildImagePrompt(thread);
        String imageUrl = imageGenerator.genProfileImage(imagePrompt);

        // 이미지 관련 내용 업데이트
        updateThreadSummary(thread.getId(), llmResponse, imageUrl);

        // 최신 스레드를 재조회하여 반환합니다.
        return getDailySummaryResponseDTO(userId, year, month, day);

    }

    @Transactional(readOnly = true)
    public ThreadSummaryResponseDTO.dailySummaryResponseDTO getDailySummaryResponseDTO(Long userId, int year, int month, int day) {
        Thread thread1 = threadRepository.findByUserIdAndYearAndMonthAndDay(userId, year, month, day)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND));

        ThreadSummaryResponseDTO.dailySummaryResponseDTO response = ThreadSummaryConverter.toSummaryDTO(thread1);

        return response;
    }

    // 스레드 조회로직 트랜잭션 분리
    @Transactional(readOnly = true)
    public ThreadSummaryData fetchThreadSummaryData(Long userId, int year, int month, int day) {
        Thread thread = threadRepository.findByUserIdAndYearAndMonthAndDay(userId, year, month, day)
                .orElse(null);
        if (thread == null) return null;
        // lazy 연관관계 초기화: DailyChatLog 목록 조회
        List<DailyChatLog> logs = thread.getDailyChatLogs();
        return new ThreadSummaryData(thread, logs);
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

                위 대화를 오늘의 채팅로그를 기반으로 그림일기 형식으로 요약해 주세요.
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

    // 이미지 생성 프롬프트
    private String buildImagePrompt(Thread thread) {
        String moodText = thread.getMood() == null ? "HAPPY" : thread.getMood().name();
        String hashtagPart = String.format("#%s #%s",
                thread.getHashtag1() == null ? "" : thread.getHashtag1(),
                thread.getHashtag2() == null ? "" : thread.getHashtag2());
        return """
                그림일기에 들어갈 그림을 그려줘
                NO TEXT!!!
                분위기: %s
                요약 제목: %s
                요약 내용: %s
                해시태그: %s
                """.formatted(
                moodText,
                thread.getSummaryTitle() == null ? "Untitled" : thread.getSummaryTitle(),
                thread.getSummaryContent() == null ? "" : thread.getSummaryContent(),
                hashtagPart
        );
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