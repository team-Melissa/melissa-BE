package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.AiProfile;
import com.melissa.diary.domain.DailyChatLog;
import com.melissa.diary.domain.enums.Role;
import com.melissa.diary.domain.enums.UsageCost;
import com.melissa.diary.repository.AiProfileRepository;
import com.melissa.diary.repository.DailyChatLogRepository;
import com.melissa.diary.repository.ThreadRepository;
import com.melissa.diary.security.JailbreakDetector;
import com.melissa.diary.web.dto.ThreadResponseDTO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.melissa.diary.domain.Thread;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class ThreadServiceV2 {

    private final ThreadRepository threadRepository;
    private final AiProfileRepository aiProfileRepository;
    private final DailyChatLogRepository dailyChatLogRepository;
    private final ChatClient chatClient;
    private final QuotaService quotaService;
    private final JailbreakDetector jailbreakDetector;
    private final UserMemoryService userMemoryService;

    public ThreadServiceV2(ThreadRepository threadRepository, AiProfileRepository aiProfileRepository, DailyChatLogRepository dailyChatLogRepository, @Qualifier("aiChatClient") ChatClient chatClient, QuotaService quotaService, JailbreakDetector jailbreakDetector, UserMemoryService userMemoryService) {
        this.threadRepository = threadRepository;
        this.aiProfileRepository = aiProfileRepository;
        this.dailyChatLogRepository = dailyChatLogRepository;
        this.chatClient = chatClient;
        this.quotaService = quotaService;
        this.jailbreakDetector = jailbreakDetector;
        this.userMemoryService = userMemoryService;
    }

    // V2: 메모리 기반 실시간 스트리밍 채팅
    public Flux<ServerSentEvent<String>> messageToAi(Long userId,
                                                     int year, int month, int day,
                                                     String content) {

        /* 블로킹(JPA) 차감 → 별도 스레드 풀 */
        Mono<Void> quotaMono = Mono.fromRunnable(() ->
                        quotaService.checkAndConsume(userId, UsageCost.CHAT))
                .subscribeOn(Schedulers.boundedElastic()).then();

        /* quotaMono 종료 → AI 스트림 실행 (Flux) */
        return quotaMono.thenMany(
                buildAiStreamV2(userId, year, month, day, content)
        );
    }

    // 웹 테스트용 Non-SSE 메모리 기반 채팅 (동기 방식)
    @Transactional
    public ThreadResponseDTO.ChatResponse messageToAiTest(Long userId,
                                                         int year, int month, int day,
                                                         String content) {
        // 쿼터 차감
        quotaService.checkAndConsume(userId, UsageCost.CHAT);

        // 스레드 데이터 가져오기 (사용자 메시지 저장 포함)
        ThreadData td = getThreadData(userId, year, month, day, content);
        
        // 탈옥 시도 검사
        if (jailbreakDetector.isJailbreakAttempt(content)) {
            String rejectMsg = "죄송합니다. 해당 요청은 처리할 수 없습니다.";
            DailyChatLog aiChat = saveAiMessage(rejectMsg, td);
            
            return ThreadResponseDTO.ChatResponse.builder()
                    .chatId(aiChat.getId())
                    .role("AI")
                    .content(rejectMsg)
                    .createAt(LocalDateTime.now())
                    .aiProfileName(td.getAiProfile().getProfileName())
                    .aiProfileImageS3(td.getAiProfile().getImageS3())
                    .build();
        }
        
        // AI 채팅 프롬프트 생성 (메모리 포함)
        String prompt = buildAiChatPromptV2(content, td.getChatHistory(), td.getAiProfile(), userId);

        try {
            // AI 응답 생성 (동기 방식)
            String aiResponse = chatClient.prompt(prompt)
                    .system(sp -> sp.param("system", td.getAiProfile().getPromptText())
                            .param("q1", td.getAiProfile().getQ1())
                            .param("q2", td.getAiProfile().getQ2())
                            .param("q3", td.getAiProfile().getQ3())
                            .param("q4", td.getAiProfile().getQ4())
                            .param("q5", td.getAiProfile().getQ5())
                            .param("q6", td.getAiProfile().getQ6()))
                    .call()
                    .content();

            // AI 응답 저장
            DailyChatLog aiChat = saveAiMessage(aiResponse, td);

            return ThreadResponseDTO.ChatResponse.builder()
                    .chatId(aiChat.getId())
                    .role("AI")
                    .content(aiResponse)
                    .createAt(LocalDateTime.now())
                    .aiProfileName(td.getAiProfile().getProfileName())
                    .aiProfileImageS3(td.getAiProfile().getImageS3())
                    .build();

        } catch (Exception e) {
            log.error("[ThreadServiceV2] 웹 테스트 채팅 중 오류 발생. userId={}", userId, e);
            String errorMsg = "채팅 처리 중 오류가 발생했습니다: " + e.getMessage();
            DailyChatLog aiChat = saveAiMessage(errorMsg, td);
            
            return ThreadResponseDTO.ChatResponse.builder()
                    .chatId(aiChat.getId())
                    .role("AI")
                    .content(errorMsg)
                    .createAt(LocalDateTime.now())
                    .aiProfileName(td.getAiProfile().getProfileName())
                    .aiProfileImageS3(td.getAiProfile().getImageS3())
                    .build();
        }
    }

    /* ---------- V2: 메모리 기반 플럭스 부분 ---------- */
    private Flux<ServerSentEvent<String>> buildAiStreamV2(Long userId, int year, int month,
                                                        int day, String content) {

        ThreadData td   = getThreadData(userId, year, month, day, content);
        String prompt   = buildAiChatPromptV2(content, td.getChatHistory(), td.getAiProfile(), userId);
        StringBuilder b = new StringBuilder();

        /* 탈옥 시도 검사 */
        if (jailbreakDetector.isJailbreakAttempt(content)) {
            String rejectMsg = "죄송합니다. 해당 요청은 처리할 수 없습니다.";

            Flux<ServerSentEvent<String>> errFlux = Flux.just(
                            ServerSentEvent.<String>builder()
                                    .event("aiMessage")
                                    .data(rejectMsg)
                                    .build()
                    )
                    // 에러 메시지 전송 완료 시점에 저장
                    .doOnComplete(() -> saveAiMessage(rejectMsg, td));

            Flux<ServerSentEvent<String>> finishFlux = Flux.just(
                    ServerSentEvent.<String>builder()
                            .event("finish")
                            .data("finish")
                            .build()
            );

            // 10ms 간격으로 보내기
            return Flux.concat(errFlux, finishFlux)
                    .delayElements(Duration.ofMillis(10));
        }

        Flux<ServerSentEvent<String>> aiFlux = chatClient.prompt(prompt)
                .system(sp -> sp.param("system", td.getAiProfile().getPromptText())
                        .param("q1", td.getAiProfile().getQ1())
                        .param("q2", td.getAiProfile().getQ2())
                        .param("q3", td.getAiProfile().getQ3())
                        .param("q4", td.getAiProfile().getQ4())
                        .param("q5", td.getAiProfile().getQ5())
                        .param("q6", td.getAiProfile().getQ6()))
                .stream()
                .chatResponse()
                .map(r -> {
                    String part = r.getResults().get(0).getOutput().getText();
                    b.append(part);
                    return ServerSentEvent.<String>builder()
                            .event("aiMessage").data(part).build();
                })
                .doOnComplete(() -> saveAiMessage(b.toString().trim(), td))
                .onErrorResume(e -> Flux.just(ServerSentEvent.<String>builder()
                        .event("error").data("SSE 오류: " + e.getMessage()).build()));

        /* finish 이벤트 붙여서 반환 */
        Flux<ServerSentEvent<String>> finish = Flux.just(
                ServerSentEvent.<String>builder().event("finish").data("finish").build());

        return Flux.concat(aiFlux, finish);
    }

    private DailyChatLog saveAiMessage(String answer, ThreadData threadData) {
        // "null" 문자열을 제거
        String cleanAnswer = answer.replace("null", "").trim();

        DailyChatLog aiChat = DailyChatLog.builder()
                .role(Role.AI)
                .content(cleanAnswer)
                .thread(threadData.getThread())
                .aiProfile(threadData.getAiProfile())
                .createdAt(LocalDateTime.now())
                .build();

        return dailyChatLogRepository.save(aiChat);
    }

    @Transactional
    public ThreadData getThreadData(Long userId, int year, int month, int day, String content) {
        // 스레드 조회
        com.melissa.diary.domain.Thread thread = threadRepository.findByUserIdAndYearAndMonthAndDay(userId, year, month, day)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND));

        // 스레드 소유자 체크
        if (!thread.getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_FORBIDDEN);
        }

        // AI 프로필 및 채팅 내역 가져오기
        AiProfile aiProfile = thread.getAiProfile();
        // 최근 사용 시각 업데이트
        aiProfile.setLastUsedAt(java.time.LocalDateTime.now());
        aiProfileRepository.save(aiProfile);
        List<DailyChatLog> chatHistory = thread.getDailyChatLogs();

        // 사용자 메시지 저장
        DailyChatLog userLog = DailyChatLog.builder()
                .role(Role.USER)
                .content(content)
                .thread(thread)
                .aiProfile(aiProfile)
                .createdAt(LocalDateTime.now())
                .build();
        dailyChatLogRepository.save(userLog);

        return new ThreadData(thread, aiProfile, chatHistory);
    }

    // V2: 메모리 기반 프롬프트 생성
    private String buildAiChatPromptV2(String userMessage, List<DailyChatLog> chatHistory, AiProfile aiProfile, Long userId) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("너는 아래와 같은 성격을 지녔어. 새 사용자의 입력을 이 성격을 기반으로 생성해야해 : \n");
        prompt.append(aiProfile.getPromptText());

        // V2: 주제 변경 감지 및 메모리 포함 여부 결정
        String todayConversation = buildTodayConversationSummary(chatHistory);
        boolean shouldIncludeMemory = shouldIncludeUserMemory(userId, todayConversation, userMessage);
        
        if (shouldIncludeMemory) {
            String userMemory = getUserMemoryContent(userId);
            if (userMemory != null && !userMemory.trim().isEmpty()) {
                prompt.append("\n\n=== 사용자에 대해 알고 있는 기억 ===\n");
                prompt.append(userMemory);
                prompt.append("\n=== 기억 끝 ===\n\n");
                prompt.append("위 기억을 참고하여 자연스럽게 대화하되, 모든 답변에 기억을 언급할 필요는 없어. 적절할 때만 활용해.");
            }
        }

        // * 시스템 메시지에 위 프로필 정보들을 모두 적었음. 이제는 채팅내역을 기반으로 다음 대화내용을 알려달라고 하면됨.
        prompt.append("""
                기존의 대화 기록을 줄게. 너는 너의 성격을 기반으로 사용자 입력에 알맞는 적절한 다음 답변을 생성해줘.
                """);

        if (aiProfile.getQ2().contains("짧")){ // 답변 길이에 더 강력한 rule 프롬프트에 추가 적용
            prompt.append("""
                답변은 한글 문자 수 기준, 공백 포함 최대 40자로 작성해줘.
                """);
        } else {
            prompt.append("""
                    답변은 한글 문자 수 기준, 공백 포함 최대 150자로 작성해줘.
                    """);
        }

        // 기존 채팅 내역 추가
        if (!chatHistory.isEmpty()) {
            prompt.append("대화 기록:\n");
            for (DailyChatLog log : chatHistory) {
                prompt.append(log.getRole().name())
                        .append(": ")
                        .append(log.getContent())
                        .append("\n");
            }
        }

        // 새 사용자 입력 추가
        prompt.append("사용자 입력: ")
                .append(userMessage)
                .append("\nAI: ");

        return prompt.toString();
    }
    
    /**
     * V2: 오늘의 대화 내용을 요약해서 문자열로 변환
     */
    private String buildTodayConversationSummary(List<DailyChatLog> chatHistory) {
        if (chatHistory == null || chatHistory.isEmpty()) {
            return "";
        }
        
        StringBuilder conversation = new StringBuilder();
        
        // 시간 순으로 정렬하여 대화 내용 구성
        chatHistory.stream()
                .sorted(Comparator.comparing(DailyChatLog::getCreatedAt))
                .forEach(log -> {
                    String rolePrefix = Role.USER.equals(log.getRole()) ? "[사용자]" : "[AI]";
                    conversation.append(rolePrefix)
                            .append(" ")
                            .append(log.getContent())
                            .append("\n");
                });
        
        return conversation.toString().trim();
    }
    
    /**
     * V2: 메모리 포함 여부 결정
     */
    private boolean shouldIncludeUserMemory(Long userId, String todayConversation, String currentMessage) {
        // 메모리가 없으면 포함하지 않음
        if (!userMemoryService.hasMemoryContent(userId)) {
            return false;
        }
        
        // 주제 변경이 감지되면 메모리 포함
        return userMemoryService.detectTopicChange(todayConversation, currentMessage);
    }
    
    /**
     * V2: 사용자 메모리 내용 조회
     */
    private String getUserMemoryContent(Long userId) {
        try {
            var userMemory = userMemoryService.getUserMemoryReadOnly(userId);
            return userMemory != null ? userMemory.getMemoryContent() : null;
        } catch (Exception e) {
            log.error("[ThreadServiceV2] 사용자 메모리 조회 중 오류 발생. userId={}", userId, e);
            return null;
        }
    }

    @Getter
    protected static class ThreadData {
        private final Thread thread;
        private final AiProfile aiProfile;
        private final List<DailyChatLog> chatHistory;

        public ThreadData(com.melissa.diary.domain.Thread thread, AiProfile aiProfile, List<DailyChatLog> chatHistory) {
            this.thread = thread;
            this.aiProfile = aiProfile;
            this.chatHistory = chatHistory;
        }
    }
}