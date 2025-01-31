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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.melissa.diary.domain.Thread;

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

    // AI 메시지 전송 및 응답 그리고 DB 업데이트
    @Transactional
    public ThreadResponseDTO.ChatResponse sendMockMessageToAi(Long userId, int year, int month, int day, String userMessage) {
        // 스레드 가져오기
        Thread thread = threadRepository.findByUserIdAndYearAndMonthAndDay(userId, year, month, day)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND));

        // 사용자 채팅을 DailyChatLog에 저장
        DailyChatLog userChat = DailyChatLog.builder()
                .role(Role.USER)
                .content(userMessage)
                .thread(thread)
                .aiProfile(null)
                .createdAt(LocalDateTime.now())
                .build();
        dailyChatLogRepository.save(userChat);

        // Mock AI응답 생성 TODO (실제로는 gpt 호출)
        String mockAiText = "안녕하세요! 저는 AI예요. '" + userMessage + "' 라고 하셨군요? 반가워요.";
        DailyChatLog aiChat = DailyChatLog.builder()
                .role(Role.AI)
                .content(mockAiText) // 해당 내용을 업데이트 해야함
                .thread(thread)
                .aiProfile(thread.getAiProfile()) // 현재 Thread의 AiProfile
                .createdAt(LocalDateTime.now())
                .build();
        dailyChatLogRepository.save(aiChat);

        // 4) 응답 DTO 구성
        return ThreadResponseDTO.ChatResponse.builder()
                .aiProfileName(thread.getAiProfile().getProfileName())
                .aiProfileImageS3(thread.getAiProfile().getImageS3())
                .role(aiChat.getRole().name())
                .content(aiChat.getContent())
                .createAt(aiChat.getCreatedAt())
                .build();
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
