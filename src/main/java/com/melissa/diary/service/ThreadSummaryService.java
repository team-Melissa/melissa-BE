package com.melissa.diary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melissa.diary.ai.ImageGenerator;
import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.*;
import com.melissa.diary.domain.Thread;
import com.melissa.diary.domain.enums.Mood;
import com.melissa.diary.domain.enums.Role;
import com.melissa.diary.repository.ThreadRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThreadSummaryService {

    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final UserSettingRepository userSettingRepository;
    private final ChatModel chatModel;
    private final ImageGenerator imageGenerator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 1시간마다 실행! -> 사용자 설정의 시간과 일치한 사용자들만 실행
     */
    @Scheduled(cron = "0 0 * * * *") // 매 정각(00분)이 될 때마다 실행
    @Transactional
    public void generateDailySummaryForAllUsers() {
        LocalTime currentTime = LocalTime.now();

        // 모든 유저 조회
        List<User> users = userRepository.findAll();

        // 각 유저별 요약 스케줄 확인 및 요약 생성
        for (User user : users) {
            // userSetting 가져오기
            UserSetting userSetting = userSettingRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.SETTING_NOT_FOUND));

            LocalTime userSummaryTime = userSetting.getSleepTime().toLocalTime();

            // 사용자가 설정한 요약 시간과 "현재 시간의 시"가 동일하다면 요약 진행
            if (currentTime.getHour() == userSummaryTime.getHour()) {
                try {
                    // 예: 오늘 날짜의 thread를 찾아서, ChatLog 요약
                    generateDailySummaryForUser(user.getId());
                } catch (Exception e) {
                    log.error("[ThreadSummary] 요약 생성 실패. userId=" + user.getId(), e);
                }
            }
        }
    }

    /**
     * 특정 유저의 오늘(또는 원하는 날짜)의 Thread에 대해 요약을 생성한다.
     */
    @Transactional
    public void generateDailySummaryForUser(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();
        int hour = now.getHour();

        // 오전 8시 이전은 전날로
        if (hour < 8) {
            day -= 1;
        }

        // Thread 조회
        Thread thread = threadRepository.findByUserIdAndYearAndMonthAndDay(userId, year, month, day)
                .orElse(null);

        if (thread == null) {
            log.warn("[ThreadSummary] 해당 날짜 스레드가 없습니다. userId={}, {}-{}-{}", userId, year, month, day);
            return;
        }

        // dailyChatLogs를 기반으로 프롬프트 생성
        List<DailyChatLog> logs = thread.getDailyChatLogs();
        
        // 채팅 내용이 없다면 요약 불필요 (AI 비용 절약)
        if (logs == null || logs.isEmpty()) {
            log.warn("[ThreadSummary] 채팅 로그가 없어 요약 불필요. userId={}, {}-{}-{}", userId, year, month, day);
            return;
        }
        // 실제 채팅 기록으로 요약 프롬프트 생성
        String chatLogsForPrompt = buildChatLogsPrompt(logs);

        // 3) 프롬프트 생성
        String prompt = buildSummaryPrompt(chatLogsForPrompt);

        // 4) LLM 호출
        String llmResponse = callLLMForSummary(prompt);

        // 5) 파싱해서 thread에 업데이트
        parseAndUpdateThread(thread, llmResponse);

        // 6) 이미지 프롬프트 생성
        String imagePrompt = buildImagePrompt(thread);

        // 7) 목업 ImageGenerator 로 이미지 url 생성
        String imageUrl = imageGenerator.genProfileImage(imagePrompt);
        thread.setImageUrl(imageUrl);

        // 8) DB 저장
        threadRepository.save(thread);
    }

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

    private String buildSummaryPrompt(String chatLogs) {
        return """
               아래는 오늘의 채팅 로그입니다:
               %s

               위 대화를 요약해 주세요. 
               - summaryTitle(30자 이하)
               - mood(HAPPY, SAD, TIRED, ANGRY, RELAX 중 하나)
               - summaryContent(200자 이하)
               - hashTag1, hashTag2(주제 연관 해시태그)

               아래 JSON 형식으로 꼭 답변해주세요:
               {
                 "summaryTitle":"...",
                 "mood":"...", 
                 "summaryContent":"...",
                 "hashTag1":"...",
                 "hashTag2":"..."
               }
               """.formatted(chatLogs);
    }

    //LLM 호출 (ChatModel 이용) -> 결과를 문자열로 반환
    private String callLLMForSummary(String prompt) {
        ChatResponse response = chatModel.call(
                new Prompt(
                        prompt,
                        OpenAiChatOptions.builder()
                                .model(OpenAiApi.ChatModel.GPT_4_O_MINI)
                                .temperature(0.7)
                                .build()
                )
        );
        // LLM의 응답 텍스트
        return response.getResult().getOutput().getText();
    }


    //LLM 응답(JSON)을 파싱하여 Thread에 summaryTitle, mood, summaryContent 등 업데이트 후 저장
    private void parseAndUpdateThread(Thread thread, String llmResponse) {
        // 1. JSON 파싱
        try {
            int startIndex = llmResponse.indexOf("{");
            int endIndex = llmResponse.lastIndexOf("}");
            if (startIndex == -1 || endIndex == -1) {
                throw new ErrorHandler(ErrorStatus.CALENDAR_PROCESSING_FAILED);
            }
            String jsonContent = llmResponse.substring(startIndex, endIndex + 1);
            JsonNode node = objectMapper.readTree(jsonContent);

            String summaryTitle = node.has("summaryTitle") ? node.get("summaryTitle").asText() : null;
            String moodStr = node.has("mood") ? node.get("mood").asText() : null;
            String summaryContent = node.has("summaryContent") ? node.get("summaryContent").asText() : null;
            String hashTag1 = node.has("hashTag1") ? node.get("hashTag1").asText() : null;
            String hashTag2 = node.has("hashTag2") ? node.get("hashTag2").asText() : null;

            // 2. Mood enum 매핑
            Mood moodEnum = null;
            if (moodStr != null) {
                try {
                    moodEnum = Mood.valueOf(moodStr.toUpperCase().trim()); // HAPPY, SAD 등
                } catch (IllegalArgumentException e) {
                    // 만약 enum에 없는 값이면 해피로 기본설정
                    moodEnum = Mood.HAPPY;
                }
            }

            log.info(moodStr);

            // 3. Thread 업데이트
            thread.setSummaryTitle(summaryTitle);
            thread.setMood(moodEnum);
            thread.setSummaryContent(summaryContent);
            thread.setHashtag1(hashTag1);
            thread.setHashtag2(hashTag2);
            thread.setSummaryCreatedAt(LocalDateTime.now());

            threadRepository.save(thread);

        } catch (IOException e) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_PROCESSING_FAILED);
        }
    }


    // 이미지 생성용 프롬프트 - Thread 객체를 활용해 프롬프트 생성

    private String buildImagePrompt(Thread thread) {
        // "제목 + 무드 + 해시태그 + 요약내용"을 바탕으로 이미지를 프롬프팅
        String moodText = thread.getMood() == null ? "HAPPY" : thread.getMood().name();
        String hashtagPart = String.format("#%s #%s",
                thread.getHashtag1() == null ? "" : thread.getHashtag1(),
                thread.getHashtag2() == null ? "" : thread.getHashtag2());

        return """
               오늘 하루를 표현하는 일러스트 이미지를 만들어줘.
               분위기: %s
               요약 제목: %s
               요약 내용: %s
               해시태그: %s
               카툰 스타일, 파스텔 톤, 밝은 느낌.
               """.formatted(
                moodText,
                thread.getSummaryTitle() == null ? "Untitled" : thread.getSummaryTitle(),
                thread.getSummaryContent() == null ? "" : thread.getSummaryContent(),
                hashtagPart
        );
    }
}