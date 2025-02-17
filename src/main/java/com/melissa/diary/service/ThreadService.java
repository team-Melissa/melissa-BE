package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.AiProfile;
import com.melissa.diary.domain.DailyChatLog;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.enums.Role;
import com.melissa.diary.repository.AiProfileRepository;
import com.melissa.diary.repository.DailyChatLogRepository;
import com.melissa.diary.repository.ThreadRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.web.dto.ThreadResponseDTO;
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
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.melissa.diary.domain.Thread;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

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

    public ThreadService(ThreadRepository threadRepository, UserRepository userRepository, AiProfileRepository aiProfileRepository, DailyChatLogRepository dailyChatLogRepository, @Qualifier("aiChatClient") ChatClient chatClient) {
        this.threadRepository = threadRepository;
        this.userRepository = userRepository;
        this.aiProfileRepository = aiProfileRepository;
        this.dailyChatLogRepository = dailyChatLogRepository;
        this.chatClient = chatClient;
    }

    @Transactional
    public ThreadResponseDTO.ThreadResponse createThread(Long userId, Long aiProfileId, int year, int month, int day) {
        // 유저와 AI 프로필 검증
        // 정상적인 유저인지 보호
        User user = getUser(userId);

        AiProfile aiProfile = aiProfileRepository.findById(aiProfileId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));

        if (!aiProfile.getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.PROFILE_FORBIDDEN);
        }

        // 해당 날짜에 이미 존재하는 스레드를 조회하거나, 없으면 생성
        Thread thread = threadRepository.findByUserIdAndYearAndMonthAndDay(userId, year, month, day)
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

        // ====== 유니크 예외 방지를 위한 try-catch ======
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
    public ThreadResponseDTO.ThreadResponse deleteTread(Long userId, int year, int month, int day){
        // 정상적인 유저인지 보호
        User user = getUser(userId);

        // 해당 스레드가 존재하는지 조회
        Thread thread = threadRepository.findByUserIdAndYearAndMonthAndDay(userId, year, month, day)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND));

        // 해당 스레드가 유저의 것이 아니라면 숨겨짐 에러
        if(!thread.getUser().getId().equals(userId)){
            throw new ErrorHandler(ErrorStatus.CALENDAR_FORBIDDEN);
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


     // Thread에 AI 프로필 업데이트 -> 이후 채팅 생성시 변경된 프로필로 생성
    @Transactional
    public void updateThreadAiProfile(Long userId, Long aiProfileId, int year, int month, int day) {
        // 정상적인 유저인지 보호
        User user = getUser(userId);

        // 스레드가져오기
        Thread thread = threadRepository.findByUserIdAndYearAndMonthAndDay(userId, year, month, day)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND));

        // 스레드가 유저의 것인지
        if (!thread.getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_FORBIDDEN);
        }

        // 변경할 AiProfile (기존에 생성되어 있어야함)
        AiProfile aiProfile = aiProfileRepository.findById(aiProfileId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));

        // 변경할 AiProfiel이 유저의 것이어야 함.
        if (!aiProfile.getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.PROFILE_FORBIDDEN);
        }

        // Thread의 AI 프로필을 변경
        thread.setAiProfile(aiProfile);
        threadRepository.save(thread);
    }
    // 실시간 스트리밍
    public Flux<ServerSentEvent<String>> messageToAi(Long userId, int year, int month, int day, String userMessage) {
        // 정상적인 유저인지 보호
        User user = getUser(userId);

        // 프롬프트 생성 -> 좀더 자세히 보면, 여기서 이미 Lazy를 대비해 로드까지 해놓음
        ThreadData threadData = getThreadData(userId, year, month, day, userMessage);
        String promptText = buildAiChatPrompt(userMessage, threadData.getChatHistory(), threadData.getAiProfile());

        StringBuilder aiAnswerBuilder = new StringBuilder();

        // AI 응답을 SSE 이벤트로 매핑하는 Flux
        Flux<ServerSentEvent<String>> aiMessageFlux = chatClient.prompt(promptText)
                .system(sp -> sp.param("system", threadData.getAiProfile().getPromptText()))
                .stream()
                .chatResponse()
                .map(response -> {
                    String partialMessage = response.getResults().get(0).getOutput().getText();
                    aiAnswerBuilder.append(partialMessage);

                    return ServerSentEvent.<String>builder()
                            .id(String.valueOf(System.currentTimeMillis()))
                            .event("aiMessage")
                            .data(partialMessage)
                            .build();
                })
                .doOnError(e -> log.error("AI 응답 스트리밍 중 에러 발생", e))
                .doOnComplete(() -> {
                    String answer = aiAnswerBuilder.toString().replace("null", "").trim();
                    saveAiMessage(answer, threadData);
                });

        // finish 이벤트를 내보내는 Flux (단일 이벤트) : 현성이 요청
        Flux<ServerSentEvent<String>> finishEventFlux = Flux.just(
                ServerSentEvent.<String>builder()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .event("finish")
                        .data("finish")
                        .build()
        );

        // 두 Flux를 순차적으로 연결하여, aiMessageFlux가 완료된 뒤 finish 이벤트를 발행
        return Flux.concat(aiMessageFlux, finishEventFlux);
    }

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        // db에 해당 유저 없으면 에러던지기(탈퇴 보호)
        return userRepository.findById(userId).orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
    }

    private void saveAiMessage(String answer, ThreadData threadData) {
        // 모든 응답이 완료되면 최종 AI 응답을 DB에 저장
        DailyChatLog aiChat = DailyChatLog.builder()
                .role(Role.AI)
                .content(answer)
                .thread(threadData.getThread())
                .aiProfile(threadData.getAiProfile())
                .createdAt(LocalDateTime.now())
                .build();
        dailyChatLogRepository.save(aiChat);
    }


    @Transactional
    public ThreadData getThreadData(Long userId, int year, int month, int day, String userMessage) {
        // 스레드 조회
        com.melissa.diary.domain.Thread thread = threadRepository.findByUserIdAndYearAndMonthAndDay(userId, year, month, day)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND));

        // 스레드 소유자 체크
        if (!thread.getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_FORBIDDEN);
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

    // 기존 채팅 기록과 AI 프로필을 이용해 프롬프트 생성
    private String buildAiChatPrompt(String userMessage, List<DailyChatLog> chatHistory, AiProfile aiProfile) {
        StringBuilder prompt = new StringBuilder();

        // AI 프로필 정보 추가
        prompt.append("너는 사용자의 일기 작성을 돕는 AI야.\n")
                .append("너의 성격: ")
                .append(aiProfile.getFeature1()).append(", ")
                .append(aiProfile.getFeature2()).append(", ")
                .append(aiProfile.getFeature3()).append("\n")
                .append("관련 해시태그: ")
                .append(aiProfile.getHashTag1()).append(", ")
                .append(aiProfile.getHashTag2()).append("\n")
                .append("친근하고 공감할 수 있는 방식으로 답변해줘.\n\n");

        // 기존 채팅 내역 추가 (있다면)
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
        prompt.append("사용자: ")
                .append(userMessage)
                .append("\nAI: ");

        return prompt.toString();
    }

    //해당 날짜(Thread)의 채팅메시지 조회
    @Transactional(readOnly = true)
    public ThreadResponseDTO.ChatListResponse getThreadMessages(Long userId, int year, int month, int day) {
        // db에 해당 유저 없으면 에러던지기(탈퇴 보호)
        User user = userRepository.findById(userId).orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        // Thread 가져오기
        Thread thread = threadRepository.findByUserIdAndYearAndMonthAndDay(userId, year, month, day)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND));

        // Thread가 유저의 것인지 체크
        if (!thread.getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_FORBIDDEN);
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
