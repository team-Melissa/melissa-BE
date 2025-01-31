package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.AiProfile;
import com.melissa.diary.domain.User;
import com.melissa.diary.repository.AiProfileRepository;
import com.melissa.diary.repository.ThreadRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.web.dto.ThreadResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.melissa.diary.domain.Thread;

@Slf4j
@Service
@RequiredArgsConstructor // 롬복으로 의존성 주입
public class ThreadService {

    private final ThreadRepository threadRepository;
    private final UserRepository userRepository;
    private final AiProfileRepository aiProfileRepository;
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
}
