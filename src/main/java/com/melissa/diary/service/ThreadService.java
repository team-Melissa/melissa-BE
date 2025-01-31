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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
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
@RequiredArgsConstructor // 롬복으로 의존성 주입
public class ThreadService {

    private final ThreadRepository threadRepository;
    private final UserRepository userRepository;
    private final AiProfileRepository aiProfileRepository;
    private final DailyChatLogRepository dailyChatLogRepository;
    private final ChatClient chatClient;

    @Transactional
    public ThreadResponseDTO.ThreadResponse creatTread(Long userId, Long aiProfileId ,int year, int month, int day){
        // 유저 존재하는지 체크 없으면 유저 없다고 리턴
        User user = userRepository.findById(userId).orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        // ai 프로필 존재하는지 체크 없으면 없다고 리턴
        AiProfile aiProfile = aiProfileRepository.findById(aiProfileId).orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));

        // ai 프로필이 유저의 것인지 체크
        if (!aiProfile.getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.PROFILE_FORBIDDEN);
        }
        
        // 해당 유저 id와 연월일 정보로 기존 쓰레드 있으면 리턴
        Thread thread = threadRepository.findByUserIdAndYearAndMonthAndDay(userId, year, month, day).orElseGet(() -> {
            // 없으면 새로운 스레드 생성해서 리턴
            Thread newThread = Thread.builder()
                    .user(user)
                    .aiProfile(aiProfile)
                    .year(year)
                    .month(month)
                    .day(day)
                    .build();
            return threadRepository.save(newThread);
        });

        // Ai Profile의 first Chat을 DailyChatLog에 저장
        DailyChatLog firstChat = DailyChatLog.builder()
                .role(Role.AI)
                .content(aiProfile.getFirstChat())
                .thread(thread)
                .aiProfile(aiProfile)
                .createdAt(LocalDateTime.now())
                .build();
        dailyChatLogRepository.save(firstChat);

        // 스레드 객체를 DTO로 변환해서 리턴
        return ThreadResponseDTO.ThreadResponse.builder()
                .threadId(thread.getId())
                .year(thread.getYear())
                .month(thread.getMonth())
                .day(thread.getDay())
                .build();
    }

    @Transactional
    public ThreadResponseDTO.ThreadResponse deleteTread(Long userId, int year, int month, int day){
        // 유저 존재하는지 체크 없으면 예외 발생
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

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
        // 유저가져오기
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

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

    // 단순화를 위해 동기 방식의 DB 호출(블록킹)과 reactive SSE 스트림을 혼합한 예시입니다.
    public Flux<ServerSentEvent<String>> messageToAi(Long userId, int year, int month, int day, String userMessage) {
        // 1. DB에서 스레드, AI 프로필, 채팅 기록을 조회하고 사용자 메시지를 저장 (블록킹 호출)
        Thread thread = threadRepository.findByUserIdAndYearAndMonthAndDay(userId, year, month, day)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND));
        AiProfile aiProfile = thread.getAiProfile();
        List<DailyChatLog> chatHistory = thread.getDailyChatLogs();

        // 사용자 메시지를 저장
        dailyChatLogRepository.save(
                DailyChatLog.builder()
                        .role(Role.USER)
                        .content(userMessage)
                        .thread(thread)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        // 2. 프롬프트 생성 (기존 채팅 기록과 AI 프로필을 포함)
        String promptText = buildAiChatPrompt(userMessage, chatHistory, aiProfile);

        // AI 응답을 누적할 StringBuilder
        StringBuilder aiAnswerBuilder = new StringBuilder();

        // 3. OpenAI 호출 및 SSE 이벤트 생성
        return chatClient.prompt(new Prompt(
                        promptText,
                        OpenAiChatOptions.builder()
                                .model(OpenAiApi.ChatModel.GPT_4_O_MINI)
                                .temperature(0.4)
                                .build()
                ))
                .stream()
                .chatResponse()  // ChatResponse Flux 반환
                .map(response -> {
                    // 부분 응답 추출 (예시: 첫 번째 결과 사용)
                    String partialMessage = response.getResults().get(0).getOutput().getText();
                    aiAnswerBuilder.append(partialMessage);

                    // SSE 이벤트 생성
                    return ServerSentEvent.<String>builder()
                            .id(String.valueOf(System.currentTimeMillis()))
                            .event("aiMessage")
                            .data(partialMessage)
                            .build();
                })
                .doOnComplete(() -> {
                    // 모든 응답이 완료되면 최종 AI 응답을 DB에 저장
                    DailyChatLog aiChat = DailyChatLog.builder()
                            .role(Role.AI)
                            .content(aiAnswerBuilder.toString())
                            .thread(thread)
                            .aiProfile(aiProfile)
                            .createdAt(LocalDateTime.now())
                            .build();
                    dailyChatLogRepository.save(aiChat);
                })
                .doOnError(e -> log.error("AI 응답 스트리밍 중 에러 발생", e));
    }

    // 기존 채팅 기록과 AI 프로필을 이용해 프롬프트를 만드는 단순한 메서드
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
        // Thread 가져오기
        Thread thread = threadRepository.findByUserIdAndYearAndMonthAndDay(userId, year, month, day)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND));

        // Thread가 유저의 것인지 체크
        if (!thread.getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_FORBIDDEN);
        }

        // Thread에 종속된 모든 채팅 로그
        List<DailyChatLog> chatLogs = thread.getDailyChatLogs();

        // 정렬 (ID 오름차순 or 별도의 createdAt 오름차순)
        chatLogs.sort(Comparator.comparing(DailyChatLog::getCreatedAt));

        // DTO 매핑
        List<ThreadResponseDTO.ChatResponse> mappedChats = chatLogs.stream()
                .map(log -> ThreadResponseDTO.ChatResponse.builder()
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

}
