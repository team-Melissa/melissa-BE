package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.AiProfile;
import com.melissa.diary.domain.DailyChatLog;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.enums.Role;
import com.melissa.diary.domain.enums.UsageCost;
import com.melissa.diary.repository.AiProfileRepository;
import com.melissa.diary.repository.DailyChatLogRepository;
import com.melissa.diary.repository.ThreadRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.security.JailbreakDetector;
import com.melissa.diary.web.dto.ThreadResponseDTO;
import lombok.Getter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
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
import java.util.Optional;

@Slf4j
@Service
public class ThreadService {

    private static final int CHAT_HISTORY_LIMIT = 15;
    private static final List<String> CHAT_ERROR_KEYWORDS = List.of("채팅 처리 중 오류", "sse 오류", "sse error");

    private final ThreadRepository threadRepository;
    private final UserRepository userRepository;
    private final AiProfileRepository aiProfileRepository;
    private final DailyChatLogRepository dailyChatLogRepository;
    private final ChatClient chatClient;
    private final QuotaService quotaService;
    private final UserMemoryService userMemoryService;
    private final JailbreakDetector jailbreakDetector;

    public ThreadService(ThreadRepository threadRepository, UserRepository userRepository, 
                        AiProfileRepository aiProfileRepository, DailyChatLogRepository dailyChatLogRepository, 
                        @Qualifier("aiChatClient") ChatClient chatClient, QuotaService quotaService, 
                        JailbreakDetector jailbreakDetector, UserMemoryService userMemoryService) {
        this.threadRepository = threadRepository;
        this.userRepository = userRepository;
        this.aiProfileRepository = aiProfileRepository;
        this.dailyChatLogRepository = dailyChatLogRepository;
        this.chatClient = chatClient;
        this.quotaService = quotaService;
        this.jailbreakDetector = jailbreakDetector;
        this.userMemoryService = userMemoryService;
    }

    @Transactional
    public ThreadResponseDTO.ThreadResponse createThread(Long userId, Long aiProfileId, int year, int month, int day) {
        // 날짜 유효성 검증
        if (!isValidDate(year, month, day)) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_INVALID_DATE);
        }
        
        // User 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        // AI 프로필 검증
        AiProfile aiProfile = aiProfileRepository.findById(aiProfileId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));

        // 해당 날짜와 AI 프로필에 이미 존재하는 스레드를 조회하거나, 없으면 생성
        Thread thread = threadRepository.findByUserIdAndAiProfileIdAndYearAndMonthAndDay(userId, aiProfileId, year, month, day)
                .orElseGet(() -> createNewThread(user, aiProfile, year, month, day));

        // 스레드 객체를 DTO로 변환하여 반환
        return ThreadResponseDTO.ThreadResponse.builder()
                .threadId(thread.getId())
                .year(thread.getYear())
                .month(thread.getMonth())
                .day(thread.getDay())
                .build();
    }
    
    private Thread createNewThread(User user, AiProfile aiProfile, int year, int month, int day) {
        // 스레드 생성
        Thread newThread = Thread.builder()
                .user(user)
                .aiProfile(aiProfile)
                .year(year)
                .month(month)
                .day(day)
                .build();

        // 유니크 예외 방지를 위한 try-catch
        try {
            threadRepository.save(newThread);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 이미 같은 (user, year, month, day)로 insert가 들어갔을 경우
            throw new ErrorHandler(ErrorStatus.THREAD_ALREADY_ENROLL);
        }

        // AI 프로필의 첫 채팅 저장
        DailyChatLog firstChat = DailyChatLog.builder()
                .role(Role.AI)
                .content(aiProfile.getFirstChat())
                .thread(newThread)
                .aiProfile(aiProfile)
                .createdAt(LocalDateTime.now())
                .build();
        dailyChatLogRepository.save(firstChat);

        return newThread;
    }

    @Transactional
    public ThreadResponseDTO.ThreadResponse deleteTread(Long userId, Long aiProfileId, int year, int month, int day){
        // 정상적인 유저인지 보호
        getUser(userId);

        // 해당 스레드가 존재하는지 조회
        Thread thread = threadRepository.findByUserIdAndAiProfileIdAndYearAndMonthAndDay(userId, aiProfileId, year, month, day)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND));

        // 해당 스레드가 유저의 것이 아니라면 권한 에러
        if(!thread.getUser().getId().equals(userId)){
            throw new ErrorHandler(ErrorStatus.THREAD_FORBIDDEN);
        }

        ThreadResponseDTO.ThreadResponse response = ThreadResponseDTO.ThreadResponse.builder()
                .threadId(thread.getId())
                .year(thread.getYear())
                .month(thread.getMonth())
                .day(thread.getDay())
                .build();

        // 스레드 삭제
        threadRepository.delete(thread);

        return response;
    }

    /**
     * v1: 기본 SSE 스트리밍 채팅 (UserMemory 미적용)
     */
    public Flux<ServerSentEvent<String>> messageToAi(Long userId, Long aiProfileId,
                                                     int year, int month, int day,
                                                     String userMessage) {

        /* 블로킹(JPA) 차감 → 별도 스레드 풀 */
        Mono<Void> quotaMono = Mono.fromRunnable(() ->
                        quotaService.checkAndConsume(userId, UsageCost.CHAT))
                .subscribeOn(Schedulers.boundedElastic()).then();

        /* quotaMono 종료 → AI 스트림 실행 (Flux) */
        return quotaMono.thenMany(
                buildAiStream(userId, aiProfileId, year, month, day, userMessage)
        );
    }
    
    /**
     * v2: UserMemory 기반 SSE 스트리밍 채팅
     */
    public Flux<ServerSentEvent<String>> messageToAiV2(Long userId, Long aiProfileId,
                                                       int year, int month, int day,
                                                       String userMessage) {

        /* 블로킹(JPA) 차감 → 별도 스레드 풀 */
        Mono<Void> quotaMono = Mono.fromRunnable(() ->
                        quotaService.checkAndConsume(userId, UsageCost.CHAT))
                .subscribeOn(Schedulers.boundedElastic()).then();

        /* quotaMono 종료 → AI 스트림 실행 (Flux) */
        return quotaMono.thenMany(
                buildAiStreamV2(userId, aiProfileId, year, month, day, userMessage)
        );
    }

    /* ---------- v1: 기본 플럭스 (UserMemory 미적용) ---------- */
    private Flux<ServerSentEvent<String>> buildAiStream(Long userId, Long aiProfileId, int year, int month,
                                                        int day, String userMessage) {

        ThreadData td   = getThreadData(userId, aiProfileId, year, month, day, userMessage);
        String prompt   = buildAiChatPrompt(userMessage, td.getChatHistory(), td.getAiProfile());
        StringBuilder b = new StringBuilder();

        /* 입력 길이 기반 최대 토큰 계산 */
        int maxTokens = calculateMaxTokens(userMessage);
        log.info("[ThreadService] v1 입력 길이: {}자, 최대 토큰: {}", userMessage.length(), maxTokens);

        /* 탈옥 시도 검사 */
        if (jailbreakDetector.isJailbreakAttempt(userMessage)) {
            String rejectMsg = "죄송합니다. 해당 요청은 처리할 수 없습니다.";

            Flux<ServerSentEvent<String>> errFlux = Flux.just(
                            ServerSentEvent.<String>builder()
                                    .event("aiMessage")
                                    .data(rejectMsg)
                                    .build()
                    )
                    // 에러 메시지 전송 완료 시점에 저장
                    .doOnComplete(() -> 
                        Mono.fromRunnable(() -> saveAiMessage(rejectMsg, td))
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe()
                    );

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

        Flux<ServerSentEvent<String>> aiFlux = chatClient.prompt()
                .system(sp -> sp.param("characterPrompt", td.getAiProfile().getPromptText()))
                .user(prompt)
                .options(OpenAiChatOptions.builder()
                        .maxCompletionTokens(maxTokens)
                        .build())
                .stream()
                .chatResponse()
                .map(r -> {
                    String part = r.getResults().get(0).getOutput().getText();
                    b.append(part);
                    return ServerSentEvent.<String>builder()
                            .event("aiMessage").data(part).build();
                })
                .doOnComplete(() -> 
                    Mono.fromRunnable(() -> saveAiMessage(b.toString().trim(), td))
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe()
                )
                .onErrorResume(e -> Flux.just(ServerSentEvent.<String>builder()
                        .event("error").data("SSE 오류: " + e.getMessage()).build()));

        /* finish 이벤트 붙여서 반환 */
        Flux<ServerSentEvent<String>> finish = Flux.just(
                ServerSentEvent.<String>builder().event("finish").data("finish").build());

        return Flux.concat(aiFlux, finish);
    }
    
    /* ---------- v2: UserMemory 기반 플럭스 ---------- */
    private Flux<ServerSentEvent<String>> buildAiStreamV2(Long userId, Long aiProfileId, int year, int month,
                                                          int day, String userMessage) {

        ThreadData td   = getThreadData(userId, aiProfileId, year, month, day, userMessage);
        
        // v2: UserMemory 포함 프롬프트
        String prompt   = buildAiChatPromptV2(userId, userMessage, td.getChatHistory(), td.getAiProfile());
        StringBuilder b = new StringBuilder();

        /* 입력 길이 기반 최대 토큰 계산 */
        int maxTokens = calculateMaxTokens(userMessage);
        log.info("[ThreadService] v2 입력 길이: {}자, 최대 토큰: {}", userMessage.length(), maxTokens);

        /* 탈옥 시도 검사 */
        if (jailbreakDetector.isJailbreakAttempt(userMessage)) {
            String rejectMsg = "죄송합니다. 해당 요청은 처리할 수 없습니다.";

            Flux<ServerSentEvent<String>> errFlux = Flux.just(
                            ServerSentEvent.<String>builder()
                                    .event("aiMessage")
                                    .data(rejectMsg)
                                    .build()
                    )
                    .doOnComplete(() -> 
                        Mono.fromRunnable(() -> saveAiMessage(rejectMsg, td))
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe()
                    );

            Flux<ServerSentEvent<String>> finishFlux = Flux.just(
                    ServerSentEvent.<String>builder()
                            .event("finish")
                            .data("finish")
                            .build()
            );

            return Flux.concat(errFlux, finishFlux)
                    .delayElements(Duration.ofMillis(10));
        }

        Flux<ServerSentEvent<String>> aiFlux = chatClient.prompt()
                .system(sp -> sp.param("characterPrompt", td.getAiProfile().getPromptText()))
                .user(prompt)
                .options(OpenAiChatOptions.builder()
                        .maxCompletionTokens(maxTokens)
                        .build())
                .stream()
                .chatResponse()
                .map(r -> {
                    String part = r.getResults().get(0).getOutput().getText();
                    b.append(part);
                    return ServerSentEvent.<String>builder()
                            .event("aiMessage").data(part).build();
                })
                .doOnComplete(() -> 
                    Mono.fromRunnable(() -> saveAiMessage(b.toString().trim(), td))
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe()
                )
                .onErrorResume(e -> Flux.just(ServerSentEvent.<String>builder()
                        .event("error").data("SSE 오류: " + e.getMessage()).build()));

        /* finish 이벤트 붙여서 반환 */
        Flux<ServerSentEvent<String>> finish = Flux.just(
                ServerSentEvent.<String>builder().event("finish").data("finish").build());

        return Flux.concat(aiFlux, finish);
    }

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        // db에 해당 유저 없으면 에러던지기(탈퇴 보호)
        return userRepository.findById(userId).orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void saveAiMessage(String answer, ThreadData threadData) {
        // "null" 문자열을 제거
        String cleanAnswer = answer.replace("null", "").trim();

        DailyChatLog aiChat = DailyChatLog.builder()
                .role(Role.AI)
                .content(cleanAnswer)
                .thread(threadData.getThread())
                .aiProfile(threadData.getAiProfile())
                .createdAt(LocalDateTime.now())
                .build();

        dailyChatLogRepository.save(aiChat);
    }

    @Transactional
    public ThreadData getThreadData(Long userId, Long aiProfileId, int year, int month, int day, String userMessage) {
        // aiProfileId 포함하여 스레드 조회 (1.3.0부터는 필수)
        com.melissa.diary.domain.Thread thread = threadRepository.findByUserIdAndAiProfileIdAndYearAndMonthAndDay(userId, aiProfileId, year, month, day)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND));

        // 스레드 소유자 체크
        if (!thread.getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.THREAD_FORBIDDEN);
        }

        // AI 프로필 및 채팅 내역 가져오기
        AiProfile aiProfile = thread.getAiProfile();
        List<DailyChatLog> chatHistory = thread.getDailyChatLogs();

        // 사용자 메시지 저장
        DailyChatLog userLog = DailyChatLog.builder()
                .role(Role.USER)
                .content(userMessage)
                .thread(thread)
                .aiProfile(aiProfile)
                .createdAt(LocalDateTime.now())
                .build();
        dailyChatLogRepository.save(userLog);

        return new ThreadData(thread, aiProfile, chatHistory);
    }

    /**
     * v1: 기본 프롬프트 생성 (UserMemory 미적용)
     */
    private String buildAiChatPrompt(String userMessage, List<DailyChatLog> chatHistory, AiProfile aiProfile) {
        List<DailyChatLog> preparedHistory = sliceRecentChatHistory(chatHistory);
        StringBuilder prompt = new StringBuilder();

        if (!preparedHistory.isEmpty()) {
            prompt.append("최근 대화 기록\n");
            for (DailyChatLog log : preparedHistory) {
                String role = log.getRole() == com.melissa.diary.domain.enums.Role.USER ? "사용자" : "나";
                prompt.append(role)
                        .append(": ")
                        .append(log.getContent())
                        .append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("사용자: ")
                .append(userMessage)
                .append("\n\n나: ");

        return prompt.toString();
    }
    
    /**
     * v2: UserMemory 통합 프롬프트 생성
     * 사용자 장기 기억 포함
     */
    private String buildAiChatPromptV2(Long userId, String userMessage, List<DailyChatLog> chatHistory, AiProfile aiProfile) {
        List<DailyChatLog> preparedHistory = sliceRecentChatHistory(chatHistory);
        StringBuilder prompt = new StringBuilder();

        if (userMemoryService.hasMemoryContent(userId)) {
            log.debug("[ThreadService] v2 모드: UserMemory 포함 시작. userId={}", userId);
            try {
                com.melissa.diary.domain.UserMemory userMemory = userMemoryService.getUserMemoryReadOnly(userId);
                if (userMemory != null && userMemory.getMemoryContent() != null && !userMemory.getMemoryContent().trim().isEmpty()) {
                    prompt.append("사용자 장기 기억\n");
                    prompt.append(userMemory.getMemoryContent().trim()).append("\n\n");
                    log.info("[ThreadService] UserMemory 프롬프트 포함 완료. userId={}", userId);
                }
            } catch (Exception e) {
                log.warn("[ThreadService] UserMemory 조회 실패, 메모리 없이 진행. userId={}", userId, e);
            }
        }

        if (!preparedHistory.isEmpty()) {
            prompt.append("최근 대화 기록\n");
            for (DailyChatLog log : preparedHistory) {
                String role = log.getRole() == com.melissa.diary.domain.enums.Role.USER ? "사용자" : "나";
                prompt.append(role)
                        .append(": ")
                        .append(log.getContent())
                        .append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("사용자: ")
                .append(userMessage)
                .append("\n\n나: ");

        return prompt.toString();
    }
    

    //해당 날짜(Thread)의 채팅메시지 조회
    @Transactional(readOnly = true)
    public ThreadResponseDTO.ChatListResponse getThreadMessages(Long userId, Long aiProfileId, int year, int month, int day) {
        // db에 해당 유저 없으면 에러던지기(탈퇴 보호)
        userRepository.findById(userId).orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        // Thread 가져오기
        Thread thread = threadRepository.findByUserIdAndAiProfileIdAndYearAndMonthAndDay(userId, aiProfileId, year, month, day)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND));

        // Thread가 유저의 것인지 체크
        if (!thread.getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.THREAD_FORBIDDEN);
        }

        // Thread에 종속된 모든 채팅 로그
        List<DailyChatLog> chatLogs = thread.getDailyChatLogs();

        // 정렬 (createdAt 오름차순)
        chatLogs.sort(Comparator.comparing(DailyChatLog::getCreatedAt));

        // DTO 매핑
        List<ThreadResponseDTO.ChatResponse> mappedChats = chatLogs.stream()
                .map(log -> ThreadResponseDTO.ChatResponse.builder()
                        .chatId(log.getId())
                        .role(log.getRole().name())
                        .aiProfileName(Optional.ofNullable(log.getAiProfile())
                                .map(AiProfile::getProfileName)
                                .orElse(""))  // aiProfile이 null이면 빈 문자열
                        .aiProfileImageS3(Optional.ofNullable(log.getAiProfile())
                                .map(AiProfile::getImageS3)
                                .orElse(""))  // aiProfile이 null이면 빈 문자열
                        .content(log.getContent())
                        .createAt(log.getCreatedAt())
                        .build())
                .toList();

        // 최종 Response
        return ThreadResponseDTO.ChatListResponse.builder()
                .aiProfileName(thread.getAiProfile().getProfileName())
                .aiProfileImageS3(thread.getAiProfile().getImageS3())
                .chats(mappedChats)
                .build();
    }
    
    /**
     * v2: 웹 테스트용 Non-SSE 메모리 기반 채팅 (동기 방식)
     * 정성적 평가를 위한 API
     */
    @Transactional
    public ThreadResponseDTO.ChatResponse messageToAiTest(Long userId, Long aiProfileId,
                                                         int year, int month, int day,
                                                         String content) {
        // 쿼터 차감
        quotaService.checkAndConsume(userId, UsageCost.CHAT);

        // 스레드 조회 및 검증 (v2: aiProfileId 포함)
        Thread thread = threadRepository.findByUserIdAndAiProfileIdAndYearAndMonthAndDay(userId, aiProfileId, year, month, day)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND));

        if (!thread.getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.THREAD_FORBIDDEN);
        }

        // AI 프로필 조회
        AiProfile aiProfile = aiProfileRepository.findById(aiProfileId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));
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
        
        // 탈옥 시도 검사
        if (jailbreakDetector.isJailbreakAttempt(content)) {
            String rejectMsg = "죄송합니다. 해당 요청은 처리할 수 없습니다.";
            DailyChatLog aiChat = DailyChatLog.builder()
                    .role(Role.AI)
                    .content(rejectMsg)
                    .thread(thread)
                    .aiProfile(aiProfile)
                    .createdAt(LocalDateTime.now())
                    .build();
            dailyChatLogRepository.save(aiChat);
            
            return ThreadResponseDTO.ChatResponse.builder()
                    .chatId(aiChat.getId())
                    .role("AI")
                    .content(rejectMsg)
                    .createAt(LocalDateTime.now())
                    .aiProfileName(aiProfile.getProfileName())
                    .aiProfileImageS3(aiProfile.getImageS3())
                    .build();
        }
        
        // AI 채팅 프롬프트 생성 (v2: 메모리 포함)
        String prompt = buildAiChatPromptV2(userId, content, chatHistory, aiProfile);

        /* 입력 길이 기반 최대 토큰 계산 */
        int maxTokens = calculateMaxTokens(content);
        log.info("[ThreadService] Test API 입력 길이: {}자, 최대 토큰: {}", content.length(), maxTokens);

        try {
            // AI 응답 생성 (동기 방식)
            String aiResponse = chatClient.prompt()
                    .system(sp -> sp.param("characterPrompt", aiProfile.getPromptText()))
                    .user(prompt)
                    .options(OpenAiChatOptions.builder()
                            .maxCompletionTokens(maxTokens)
                            .build())
                    .call()
                    .content();

            // AI 응답 저장
            String cleanAnswer = aiResponse.replace("null", "").trim();
            DailyChatLog aiChat = DailyChatLog.builder()
                    .role(Role.AI)
                    .content(cleanAnswer)
                    .thread(thread)
                    .aiProfile(aiProfile)
                    .createdAt(LocalDateTime.now())
                    .build();
            dailyChatLogRepository.save(aiChat);

            return ThreadResponseDTO.ChatResponse.builder()
                    .chatId(aiChat.getId())
                    .role("AI")
                    .content(cleanAnswer)
                    .createAt(LocalDateTime.now())
                    .aiProfileName(aiProfile.getProfileName())
                    .aiProfileImageS3(aiProfile.getImageS3())
                    .build();

        } catch (Exception e) {
            log.error("[ThreadService] 웹 테스트 채팅 중 오류 발생. userId={}, aiProfileId={}", userId, aiProfileId, e);
            String errorMsg = "채팅 처리 중 오류가 발생했습니다.";
            DailyChatLog aiChat = DailyChatLog.builder()
                    .role(Role.AI)
                    .content(errorMsg)
                    .thread(thread)
                    .aiProfile(aiProfile)
                    .createdAt(LocalDateTime.now())
                    .build();
            dailyChatLogRepository.save(aiChat);
            
            return ThreadResponseDTO.ChatResponse.builder()
                    .chatId(aiChat.getId())
                    .role("AI")
                    .content(errorMsg)
                    .createAt(LocalDateTime.now())
                    .aiProfileName(aiProfile.getProfileName())
                    .aiProfileImageS3(aiProfile.getImageS3())
                    .build();
        }
    }
    
    /**
     * 날짜 유효성 검증
     */
    private boolean isValidDate(int year, int month, int day) {
        if (month < 1 || month > 12) return false;
        if (day < 1 || day > 31) return false;
        
        // 2월 처리
        if (month == 2) {
            boolean isLeapYear = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
            return day <= (isLeapYear ? 29 : 28);
        }
        
        // 4, 6, 9, 11월은 30일까지
        if (month == 4 || month == 6 || month == 9 || month == 11) {
            return day <= 30;
        }
        
        return true;
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

    private List<DailyChatLog> sliceRecentChatHistory(List<DailyChatLog> chatHistory) {
        if (chatHistory == null || chatHistory.isEmpty()) {
            return List.of();
        }

        List<DailyChatLog> sorted = chatHistory.stream()
                .filter(this::isUsableChatLog)
                .sorted(Comparator.comparing(DailyChatLog::getCreatedAt))
                .toList();

        int size = sorted.size();
        if (size <= CHAT_HISTORY_LIMIT) {
            return sorted;
        }
        return sorted.subList(size - CHAT_HISTORY_LIMIT, size);
    }

    private boolean isUsableChatLog(DailyChatLog log) {
        if (log == null) {
            return false;
        }
        String content = Optional.ofNullable(log.getContent()).orElse("").trim();
        if (content.isEmpty()) {
            return false;
        }
        String lowerContent = content.toLowerCase();
        return CHAT_ERROR_KEYWORDS.stream().noneMatch(keyword -> lowerContent.contains(keyword.toLowerCase()));
    }

    /**
     * 사용자 입력 길이에 따른 최대 토큰 수 계산
     * 입력 길이에 비례하여 응답 길이를 동적으로 조절
     * 
     * @param userInput 사용자 입력 메시지
     * @return OpenAI API maxTokens 파라미터 값
     */
    private int calculateMaxTokens(String userInput) {
        if (userInput == null || userInput.isEmpty()) {
            return 50; // 기본값
        }
        
        int inputLength = userInput.trim().length();
        
        if (inputLength <= 20) {
            return 50; // 짧은 응답 (~30자)
        } else if (inputLength <= 100) {
            return 120; // 중간 응답 (~60~120자)
        } else if (inputLength <= 300) {
            return 200; // 긴 응답 (~120~200자)
        } else {
            return 300; // 매우 긴 응답 (~200~300자)
        }
    }
}
