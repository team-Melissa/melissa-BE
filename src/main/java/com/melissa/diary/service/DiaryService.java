package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.AiProfile;
import com.melissa.diary.domain.Diary;
import com.melissa.diary.domain.Thread;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.enums.Mood;
import com.melissa.diary.domain.enums.UsageCost;
import com.melissa.diary.event.DiaryImageEvent;
import com.melissa.diary.repository.AiProfileRepository;
import com.melissa.diary.repository.DiaryRepository;
import com.melissa.diary.repository.ThreadRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.web.dto.DiaryRequestDTO;
import com.melissa.diary.web.dto.DiaryResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [v1.3.0] Diary 기반 일기 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryService {
    
    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final AiProfileRepository aiProfileRepository;
    private final QuotaService quotaService;
    private final ApplicationEventPublisher publisher;
    
    /**
     * 수동 일기 작성
     * - Thread가 없으면 자동 생성
     * - 하루 최대 3개 제한
     * - 이미지는 DALL-E로 생성 (비동기)
     */
    @Transactional
    public DiaryResponseDTO.DiaryResponse createManualDiary(Long userId, 
                                                           DiaryRequestDTO.ManualDiaryCreateRequest request) {
        // 유저 검증 및 쿼터 체크
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
        quotaService.checkAndConsume(user, UsageCost.SUMMARY);
        
        // AI 프로필 검증
        AiProfile aiProfile = aiProfileRepository.findById(request.getAiProfileId())
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));
        
        // 날짜 유효성 검증
        if (!isValidDate(request.getYear(), request.getMonth(), request.getDay())) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_INVALID_DATE);
        }
        
        // 하루 최대 3개 제한 확인
        int diaryCount = diaryRepository.countByUserIdAndYearAndMonthAndDayAndIsActive(
                userId, request.getYear(), request.getMonth(), request.getDay(), true);
        
        if (diaryCount >= 3) {
            throw new ErrorHandler(ErrorStatus.DIARY_MAX_COUNT_EXCEEDED);
        }
        
        // Thread 조회 또는 생성
        Thread thread = threadRepository.findByUserIdAndAiProfileIdAndYearAndMonthAndDay(
                userId, request.getAiProfileId(), 
                request.getYear(), request.getMonth(), request.getDay())
                .orElseGet(() -> createNewThread(user, aiProfile, 
                        request.getYear(), request.getMonth(), request.getDay()));
        
        // Mood 변환
        Mood mood = null;
        if (request.getMood() != null) {
            try {
                mood = Mood.valueOf(request.getMood().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("[Diary] 유효하지 않은 mood 값: {}", request.getMood());
            }
        }
        
        // 일기 생성
        Diary diary = Diary.builder()
                .user(user)
                .thread(thread)
                .year(request.getYear())
                .month(request.getMonth())
                .day(request.getDay())
                .title(request.getTitle())
                .content(request.getContent())
                .mood(mood)
                .hashtag1(request.getHashtag1())
                .hashtag2(request.getHashtag2())
                .imageUrl(null)  // 초기에는 null, 비동기로 생성
                .version(1)
                .isActive(true)
                .build();
        
        diary = diaryRepository.save(diary);
        
        // 이미지 생성 요청 (비동기)
        if (Boolean.TRUE.equals(request.getGenerateImage())) {
            publisher.publishEvent(new DiaryImageEvent(diary.getId()));
        }
        
        log.info("[Diary] 수동 일기 작성 완료. userId={}, diaryId={}, year={}-{}-{}", 
                userId, diary.getId(), request.getYear(), request.getMonth(), request.getDay());
        
        return buildDiaryResponse(diary);
    }
    
    /**
     * Thread 생성 (일기 작성 시 필요한 경우)
     */
    private Thread createNewThread(User user, AiProfile aiProfile, int year, int month, int day) {
        Thread newThread = Thread.builder()
                .user(user)
                .aiProfile(aiProfile)
                .year(year)
                .month(month)
                .day(day)
                .build();
        
        try {
            return threadRepository.save(newThread);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 동시성 문제로 이미 생성된 경우 조회해서 반환
            return threadRepository.findByUserIdAndAiProfileIdAndYearAndMonthAndDay(
                    user.getId(), aiProfile.getId(), year, month, day)
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.THREAD_ALREADY_ENROLL));
        }
    }
    
    /**
     * 일기 수정
     * - 본인의 일기만 수정 가능
     * - null이 아닌 필드만 업데이트
     */
    @Transactional
    public DiaryResponseDTO.DiaryResponse updateDiary(Long userId, Long diaryId, 
                                                     DiaryRequestDTO.DiaryUpdateRequest request) {
        // 유저 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
        
        // 일기 조회
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.DIARY_NOT_FOUND));
        
        // 권한 검증: 본인의 일기인지 확인
        if (!diary.getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.DIARY_FORBIDDEN);
        }
        
        // 삭제된 일기는 수정 불가
        if (!diary.isActive()) {
            throw new ErrorHandler(ErrorStatus.DIARY_ALREADY_DELETED);
        }
        
        // null이 아닌 필드만 업데이트
        if (request.getTitle() != null) {
            diary.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            diary.setContent(request.getContent());
        }
        if (request.getMood() != null) {
            try {
                diary.setMood(Mood.valueOf(request.getMood().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("[Diary] 유효하지 않은 mood 값: {}", request.getMood());
            }
        }
        if (request.getHashtag1() != null) {
            diary.setHashtag1(request.getHashtag1());
        }
        if (request.getHashtag2() != null) {
            diary.setHashtag2(request.getHashtag2());
        }
        
        // 버전 증가
        diary.setVersion(diary.getVersion() + 1);
        
        diary = diaryRepository.save(diary);
        
        // 이미지 재생성 요청 (선택적)
        if (Boolean.TRUE.equals(request.getGenerateImage())) {
            quotaService.checkAndConsume(user, UsageCost.SUMMARY);
            diary.setImageUrl(null);  // 기존 이미지 초기화
            diaryRepository.save(diary);
            publisher.publishEvent(new DiaryImageEvent(diary.getId()));
        }
        
        log.info("[Diary] 일기 수정 완료. userId={}, diaryId={}, version={}", 
                userId, diaryId, diary.getVersion());
        
        return buildDiaryResponse(diary);
    }
    
    /**
     * 일기 삭제 (소프트 삭제)
     * - isActive = false 처리
     * - 본인의 일기만 삭제 가능
     */
    @Transactional
    public DiaryResponseDTO.DiaryDeleteResponse deleteDiary(Long userId, Long diaryId) {
        // 유저 검증
        userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
        
        // 일기 조회
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.DIARY_NOT_FOUND));
        
        // 권한 검증: 본인의 일기인지 확인
        if (!diary.getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.DIARY_FORBIDDEN);
        }
        
        // 이미 삭제된 일기인지 확인
        if (!diary.isActive()) {
            throw new ErrorHandler(ErrorStatus.DIARY_ALREADY_DELETED);
        }
        
        // 소프트 삭제
        diary.setActive(false);
        diaryRepository.save(diary);
        
        log.info("[Diary] 일기 삭제 완료. userId={}, diaryId={}", userId, diaryId);
        
        return DiaryResponseDTO.DiaryDeleteResponse.builder()
                .diaryId(diaryId)
                .message("일기가 삭제되었습니다.")
                .build();
    }
    
    /**
     * Diary -> DiaryResponse 변환
     */
    private DiaryResponseDTO.DiaryResponse buildDiaryResponse(Diary diary) {
        return DiaryResponseDTO.DiaryResponse.builder()
                .diaryId(diary.getId())
                .threadId(diary.getThread().getId())
                .year(diary.getYear())
                .month(diary.getMonth())
                .day(diary.getDay())
                .title(diary.getTitle())
                .content(diary.getContent())
                .mood(diary.getMood() != null ? diary.getMood().name() : null)
                .hashtag1(diary.getHashtag1())
                .hashtag2(diary.getHashtag2())
                .imageUrl(diary.getImageUrl())
                .version(diary.getVersion())
                .createdAt(diary.getCreatedAt())
                .build();
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
}

