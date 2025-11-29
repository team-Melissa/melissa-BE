package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.Diary;
import com.melissa.diary.repository.DiaryRepository;
import com.melissa.diary.repository.UserRepository;
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

