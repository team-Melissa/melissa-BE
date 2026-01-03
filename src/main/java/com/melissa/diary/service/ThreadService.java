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
        StringBuilder prompt = new StringBuilder();

        // Role Definition
        prompt.append("""
                ## Role Definition
                너는 사용자의 하루를 기록하기 위해 대화를 나누는 AI 다이어리 파트너야.
                사용자와 자연스럽게 대화하며 공감해주고, 나중에 일기로 작성할 수 있는 주요 사건, 감정, 생각 등의 정보를 대화 속에서 이끌어내야 해.
                
                """);

        // Persona Configuration
        prompt.append("## Persona Configuration (기본 성격)\n");
        prompt.append("너는 아래의 성격을 완벽하게 연기해야 한다.\n");
        prompt.append("Core Personality: ").append(aiProfile.getPromptText()).append("\n\n");

        // Communication Guidelines
        prompt.append("## Communication Guidelines (대화 지침)\n");
        prompt.append("사용자와의 대화에서 아래 6가지 지침을 반드시 준수하라.\n");
        prompt.append("Tone & Manner (말투): ").append(aiProfile.getQ1()).append("\n");
        prompt.append("Response Length (길이): ").append(aiProfile.getQ2()).append("\n");
        prompt.append("Response Style (답변 방식): ").append(aiProfile.getQ3()).append("\n");
        prompt.append("Questioning Style (질문 방식): ").append(aiProfile.getQ4()).append("\n");
        prompt.append("Intervention Level (개입 정도): ").append(aiProfile.getQ5()).append("\n");
        prompt.append("Humor Usage (유머): ").append(aiProfile.getQ6()).append("\n\n");

        // Operational Rules
        prompt.append("""
                ## Operational Rules
                - 사용자의 감정에 먼저 깊이 공감한 뒤, 일기 작성을 위한 구체적인 내용(누구와, 어디서, 무엇을 했는지 등)을 자연스럽게 물어봐줘.
                - 기계적인 느낌을 주지 말고, 위에서 설정된 '말투'와 '성격'을 유지하며 친구처럼 대화해.
                - 이모지는 답변당 최대 1개만 사용하고, 없어도 괜찮아.
                """);

        // 답변 길이 구체화
        if (aiProfile.getQ2().contains("짧")){ 
            prompt.append("- 답변 길이: UTF-8 기준 100-150바이트 이내 (한글 약 30-50자, 공백 포함), 짧고 간결하게 핵심만 전달해줘.\n\n");
        } else {
            prompt.append("- 답변 길이: UTF-8 기준 300-500바이트 이내 (한글 약 100-170자, 공백 포함), 자연스럽게 대화하되 너무 길지 않게 적당히 끊어줘.\n\n");
        }

        // 기존 채팅 내역 추가
        if (!chatHistory.isEmpty()) {
            prompt.append("## 오늘의 대화 기록\n");
            for (DailyChatLog log : chatHistory) {
                String role = log.getRole() == com.melissa.diary.domain.enums.Role.USER ? "사용자" : "나";
                prompt.append(role)
                        .append(": ")
                        .append(log.getContent())
                        .append("\n");
            }
        }

        // 새 사용자 입력 추가
        prompt.append("\n## 새로운 사용자 메시지\n사용자: ")
                .append(userMessage)
                .append("\n\n나: ");

        return prompt.toString();
    }
    
    /**
     * v2: UserMemory 통합 프롬프트 생성
     * 사용자 장기 기억 포함
     */
    private String buildAiChatPromptV2(Long userId, String userMessage, List<DailyChatLog> chatHistory, AiProfile aiProfile) {
        StringBuilder prompt = new StringBuilder();

        // Role Definition
        prompt.append("""
                ## Role Definition
                너는 사용자의 하루를 기록하기 위해 대화를 나누는 AI 다이어리 파트너야.
                사용자와 자연스럽게 대화하며 공감해주고, 나중에 일기로 작성할 수 있는 주요 사건, 감정, 생각 등의 정보를 대화 속에서 이끌어내야 해.
                
                """);

        // Persona Configuration
        prompt.append("## Persona Configuration (기본 성격)\n");
        prompt.append("너는 아래의 성격을 완벽하게 연기해야 한다.\n");
        prompt.append("Core Personality: ").append(aiProfile.getPromptText()).append("\n\n");

        // Communication Guidelines
        prompt.append("## Communication Guidelines (대화 지침)\n");
        prompt.append("사용자와의 대화에서 아래 6가지 지침을 반드시 준수하라.\n");
        prompt.append("Tone & Manner (말투): ").append(aiProfile.getQ1()).append("\n");
        prompt.append("Response Length (길이): ").append(aiProfile.getQ2()).append("\n");
        prompt.append("Response Style (답변 방식): ").append(aiProfile.getQ3()).append("\n");
        prompt.append("Questioning Style (질문 방식): ").append(aiProfile.getQ4()).append("\n");
        prompt.append("Intervention Level (개입 정도): ").append(aiProfile.getQ5()).append("\n");
        prompt.append("Humor Usage (유머): ").append(aiProfile.getQ6()).append("\n\n");

        // Operational Rules
        prompt.append("""
                ## Operational Rules
                - 사용자의 감정에 먼저 깊이 공감한 뒤, 일기 작성을 위한 구체적인 내용(누구와, 어디서, 무엇을 했는지 등)을 자연스럽게 물어봐줘.
                - 기계적인 느낌을 주지 말고, 위에서 설정된 '말투'와 '성격'을 유지하며 친구처럼 대화해.
                - 이모지는 답변당 최대 1개만 사용하고, 없어도 괜찮아.
                """);

        // 답변 길이 구체화
        if (aiProfile.getQ2().contains("짧")){ 
            prompt.append("- 답변 길이: UTF-8 기준 100-150바이트 이내 (한글 약 30-50자, 공백 포함), 짧고 간결하게 핵심만 전달해줘.\n\n");
        } else {
            prompt.append("- 답변 길이: UTF-8 기준 300-500바이트 이내 (한글 약 100-170자, 공백 포함), 자연스럽게 대화하되 너무 길지 않게 적당히 끊어줘.\n\n");
        }

        // ======== v2: UserMemory 통합 (항상 포함) ========
        // v2 개선: 주제 변경 감지 대신 항상 UserMemory 포함
        if (userMemoryService.hasMemoryContent(userId)) {
            log.debug("[ThreadService] v2 모드: UserMemory 포함 시작. userId={}", userId);
            try {
                com.melissa.diary.domain.UserMemory userMemory = userMemoryService.getUserMemoryReadOnly(userId);
                if (userMemory != null && userMemory.getMemoryContent() != null && !userMemory.getMemoryContent().trim().isEmpty()) {
                    prompt.append("\n=== 사용자에 대해 알고 있는 장기 기억 ===\n");
                    prompt.append(userMemory.getMemoryContent());
                    prompt.append("\n=== 기억 끝 ===\n\n");
                    prompt.append("""
                            ## 기억 활용 가이드
                            - 사용자가 관련 주제를 언급하면 위 기억을 자연스럽게 활용해줘.
                            - "기억하고 있어", "저번에 말했지" 같은 직접적 언급은 피하고, 자연스럽게 녹여서 대화해.
                            - 사용자가 물어보면 기억한 내용을 구체적으로 답변해줘.
                            - 기억에 없는 내용은 솔직하게 모른다고 해도 괜찮아.
                            """);
                    
                    log.info("[ThreadService] UserMemory 프롬프트 포함 완료. userId={}", userId);
                }
            } catch (Exception e) {
                log.warn("[ThreadService] UserMemory 조회 실패, 메모리 없이 진행. userId={}", userId, e);
            }
        }
        // ======== v2 끝 ========

        // 기존 채팅 내역 추가
        if (!chatHistory.isEmpty()) {
            prompt.append("\n## 오늘의 대화 기록\n");
            for (DailyChatLog log : chatHistory) {
                String role = log.getRole() == com.melissa.diary.domain.enums.Role.USER ? "사용자" : "나";
                prompt.append(role)
                        .append(": ")
                        .append(log.getContent())
                        .append("\n");
            }
        }

        // 새 사용자 입력 추가
        prompt.append("\n## 새로운 사용자 메시지\n사용자: ")
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

        try {
            // AI 응답 생성 (동기 방식)
            String aiResponse = chatClient.prompt(prompt)
                    .system(sp -> sp.param("system", aiProfile.getPromptText())
                            .param("q1", aiProfile.getQ1())
                            .param("q2", aiProfile.getQ2())
                            .param("q3", aiProfile.getQ3())
                            .param("q4", aiProfile.getQ4())
                            .param("q5", aiProfile.getQ5())
                            .param("q6", aiProfile.getQ6()))
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
}
