package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.AiProfile;
import com.melissa.diary.domain.Thread;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.enums.UsageCost;
import com.melissa.diary.event.ThreadImageEvent;
import com.melissa.diary.repository.AiProfileRepository;
import com.melissa.diary.repository.ThreadRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.web.dto.ManualDiaryRequestDTO;
import com.melissa.diary.web.dto.ThreadSummaryResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManualDiaryService {

    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final AiProfileRepository aiProfileRepository;
    private final QuotaService quotaService;
    private final ApplicationEventPublisher publisher;

    /**
     * 채팅 없이 사용자가 직접 일기를 작성하는 메서드
     */
    @Transactional
    public ThreadSummaryResponseDTO.dailySummaryResponseDTO createManualDiary(
            Long userId, ManualDiaryRequestDTO.ManualDiaryCreateRequest request) {

        // 날짜 유효성 검증
        if (!isValidDate(request.getYear(), request.getMonth(), request.getDay())) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_INVALID_DATE);
        }

        // 사용자 검증 및 쿼터 체크
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
        quotaService.checkAndConsume(user, UsageCost.SUMMARY);

        // AI 프로필 검증
        AiProfile aiProfile = aiProfileRepository.findById(request.getAiProfileId())
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.AI_PROFILE_NOT_FOUND));

        // AI 프로필 소유자 검증
        if (!aiProfile.getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.AI_PROFILE_FORBIDDEN);
        }

        // 해당 날짜에 이미 Thread가 존재하는지 확인하고, 있다면 덮어쓰기
        Thread existingThread = threadRepository.findByUserIdAndYearAndMonthAndDay(
                userId, request.getYear(), request.getMonth(), request.getDay()).orElse(null);
        
        if (existingThread != null) {
            // 기존 Thread가 있으면 내용을 덮어쓰기
            return updateExistingThread(existingThread, request, user, aiProfile);
        }

        // 새 Thread 생성
        Thread thread = Thread.builder()
                .user(user)
                .aiProfile(aiProfile)
                .year(request.getYear())
                .month(request.getMonth())
                .day(request.getDay())
                .summaryTitle(request.getTitle())
                .mood(request.getMood())
                .summaryContent(request.getContent())
                .hashtag1(request.getHashtag1())
                .hashtag2(request.getHashtag2())
                .summaryCreatedAt(LocalDateTime.now())
                .lastSummaryRequestAt(LocalDateTime.now())
                .build();

        try {
            thread = threadRepository.save(thread);
        } catch (DataIntegrityViolationException e) {
            throw new ErrorHandler(ErrorStatus.THREAD_ALREADY_ENROLL);
        }

        // AI 프로필 최근 사용 시각 업데이트
        aiProfile.setLastUsedAt(LocalDateTime.now());
        aiProfileRepository.save(aiProfile);

        // 이미지 생성 요청 (비동기)
        if (request.getGenerateImage()) {
            publisher.publishEvent(new ThreadImageEvent(thread.getId()));
        }

        // 응답 DTO 생성
        return ThreadSummaryResponseDTO.dailySummaryResponseDTO.builder()
                .year(thread.getYear())
                .month(thread.getMonth())
                .day(thread.getDay())
                .summaryTitle(thread.getSummaryTitle())
                .summaryMood(thread.getMood() != null ? thread.getMood().name() : null)
                .summaryContent(thread.getSummaryContent())
                .hashTag1(thread.getHashtag1())
                .hashTag2(thread.getHashtag2())
                .imageS3(thread.getImageUrl()) // 초기에는 null
                .build();
    }

    /**
     * 기존 수동 작성 일기를 수정하는 메서드
     */
    @Transactional
    public ThreadSummaryResponseDTO.dailySummaryResponseDTO updateManualDiary(
            Long userId, int year, int month, int day,
            ManualDiaryRequestDTO.ManualDiaryUpdateRequest request) {

        // 사용자 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        // 기존 Thread 조회
        Thread thread = threadRepository.findByUserIdAndYearAndMonthAndDay(userId, year, month, day)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND));

        // Thread 소유자 검증
        if (!thread.getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_FORBIDDEN);
        }

        // 채팅 로그 존재 여부와 관계없이 수정 가능 (통합 관리)

        // 1분 이내 중복 요청 차단
        if (thread.getLastSummaryRequestAt() != null &&
                thread.getLastSummaryRequestAt().isAfter(LocalDateTime.now().minusMinutes(1))) {
            throw new ErrorHandler(ErrorStatus.THREAD_TOO_MANY_REQUESTS);
        }

        // 필드 업데이트 (null이 아닌 값만)
        if (request.getTitle() != null) {
            thread.setSummaryTitle(request.getTitle());
        }
        if (request.getMood() != null) {
            thread.setMood(request.getMood());
        }
        if (request.getContent() != null) {
            thread.setSummaryContent(request.getContent());
        }
        if (request.getHashtag1() != null) {
            thread.setHashtag1(request.getHashtag1());
        }
        if (request.getHashtag2() != null) {
            thread.setHashtag2(request.getHashtag2());
        }

        thread.setLastSummaryRequestAt(LocalDateTime.now());
        thread = threadRepository.save(thread);

        // 이미지 재생성 요청 (선택적)
        if (request.getGenerateImage() != null && request.getGenerateImage()) {
            // 쿼터 체크 (이미지 재생성 시)
            quotaService.checkAndConsume(user, UsageCost.SUMMARY);
            publisher.publishEvent(new ThreadImageEvent(thread.getId()));
        }

        // 응답 DTO 생성
        return ThreadSummaryResponseDTO.dailySummaryResponseDTO.builder()
                .year(thread.getYear())
                .month(thread.getMonth())
                .day(thread.getDay())
                .summaryTitle(thread.getSummaryTitle())
                .summaryMood(thread.getMood() != null ? thread.getMood().name() : null)
                .summaryContent(thread.getSummaryContent())
                .hashTag1(thread.getHashtag1())
                .hashTag2(thread.getHashtag2())
                .imageS3(thread.getImageUrl())
                .build();
    }


    /**
     * 기존 Thread를 수동 작성 내용으로 덮어쓰는 메서드
     */
    private ThreadSummaryResponseDTO.dailySummaryResponseDTO updateExistingThread(
            Thread existingThread, ManualDiaryRequestDTO.ManualDiaryCreateRequest request, 
            User user, AiProfile aiProfile) {

        // 1분 이내 중복 요청 차단
        if (existingThread.getLastSummaryRequestAt() != null &&
                existingThread.getLastSummaryRequestAt().isAfter(LocalDateTime.now().minusMinutes(1))) {
            throw new ErrorHandler(ErrorStatus.THREAD_TOO_MANY_REQUESTS);
        }

        // 쿼터 체크 (덮어쓰기도 새로운 요약 생성으로 간주) - 이미 createManualDiary에서 체크했으므로 제거

        // 기존 Thread 내용을 수동 작성 내용으로 덮어쓰기
        existingThread.setAiProfile(aiProfile);  // AI 프로필도 변경 가능
        existingThread.setSummaryTitle(request.getTitle());
        existingThread.setMood(request.getMood());
        existingThread.setSummaryContent(request.getContent());
        existingThread.setHashtag1(request.getHashtag1());
        existingThread.setHashtag2(request.getHashtag2());
        existingThread.setSummaryCreatedAt(LocalDateTime.now());
        existingThread.setLastSummaryRequestAt(LocalDateTime.now());

        // 기존 이미지 URL 초기화 (새로 생성할 예정)
        if (request.getGenerateImage()) {
            existingThread.setImageUrl(null);
        }

        existingThread = threadRepository.save(existingThread);

        // AI 프로필 최근 사용 시각 업데이트
        aiProfile.setLastUsedAt(LocalDateTime.now());
        aiProfileRepository.save(aiProfile);

        // 이미지 생성 요청 (비동기)
        if (request.getGenerateImage()) {
            publisher.publishEvent(new ThreadImageEvent(existingThread.getId()));
        }

        // 응답 DTO 생성
        return ThreadSummaryResponseDTO.dailySummaryResponseDTO.builder()
                .year(existingThread.getYear())
                .month(existingThread.getMonth())
                .day(existingThread.getDay())
                .summaryTitle(existingThread.getSummaryTitle())
                .summaryMood(existingThread.getMood() != null ? existingThread.getMood().name() : null)
                .summaryContent(existingThread.getSummaryContent())
                .hashTag1(existingThread.getHashtag1())
                .hashTag2(existingThread.getHashtag2())
                .imageS3(existingThread.getImageUrl()) // 초기에는 null (이미지 생성 중)
                .build();
    }

    /**
     * 날짜 유효성 검증
     */
    private boolean isValidDate(int year, int month, int day) {
        if (year < 1900 || year > 2100) return false;
        if (month < 1 || month > 12) return false;
        if (day < 1 || day > 31) return false;
        
        // 월별 최대 일수 체크
        int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        
        // 윤년 체크
        if (month == 2 && isLeapYear(year)) {
            return day <= 29;
        }
        
        return day <= daysInMonth[month - 1];
    }
    
    private boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }
}
