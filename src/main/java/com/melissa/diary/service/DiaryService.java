package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.Diary;
import com.melissa.diary.domain.enums.Mood;
import com.melissa.diary.repository.DiaryRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.web.dto.DiaryRequestDTO;
import com.melissa.diary.web.dto.DiaryResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [v1.3.0] 일기 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryService {
    
    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    
    /**
     * 일기 수정
     * - 본인의 일기만 수정 가능
     * - null이 아닌 필드만 업데이트
     */
    @Transactional
    public DiaryResponseDTO.DiaryResponse updateDiary(Long userId, Long diaryId, 
                                                     DiaryRequestDTO.DiaryUpdateRequest request) {
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
                // mood 변환 실패 시 무시하고 계속 진행
            }
        }
        if (request.getHashtag1() != null) {
            diary.setHashtag1(request.getHashtag1());
        }
        if (request.getHashtag2() != null) {
            diary.setHashtag2(request.getHashtag2());
        }
        if (request.getImageUrl() != null) {
            diary.setImageUrl(request.getImageUrl());
        }
        
        // 버전 증가
        diary.setVersion(diary.getVersion() + 1);
        
        diaryRepository.save(diary);
        
        log.info("[Diary] 일기 수정 완료. userId={}, diaryId={}, version={}", 
                userId, diaryId, diary.getVersion());
        
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
}

