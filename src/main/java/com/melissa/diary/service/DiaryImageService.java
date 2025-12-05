package com.melissa.diary.service;

import com.melissa.diary.ai.ImageGenerator;
import com.melissa.diary.domain.Diary;
import com.melissa.diary.repository.DiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Diary 이미지 생성 서비스
 * - DALL-E로 이미지 생성
 * - S3에 업로드
 * - DB에 URL 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryImageService {
    
    private final DiaryRepository diaryRepository;
    private final ImageGenerator imageGenerator;
    private final DiaryImagePromptRefinerService refiner;
    
    private static final String DEFAULT_IMG =
            "https://melissa-s3.s3.ap-northeast-2.amazonaws.com/default.png";
    
    /**
     * diaryId 기준으로 이미지를 생성하고 imageUrl을 저장한다.
     * 비동기로 실행됨
     */
    public void generateAndSaveImage(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId).orElse(null);
        if (diary == null) {
            log.error("[Async-DiaryImage] diaryId={} not found", diaryId);
            return;
        }
        
        // 1차 프롬프트 생성
        String rawPrompt = buildImagePrompt(diary);
        // 2차 LLM으로 프롬프트 정제
        String finalPrompt = refiner.refine(rawPrompt);
        
        try {
            // DALL-E로 이미지 생성 → S3 diary 폴더에 업로드
            String url = imageGenerator.genDiaryImage(finalPrompt);
            updateDiaryImage(diary, url);
            log.info("[Async-DiaryImage] 이미지 생성 완료. diaryId={}, url={}", diaryId, url);
        } catch (Exception e) {
            log.error("[Async-DiaryImage] diaryId={} 처리 실패", diaryId, e);
            updateDiaryImage(diary, DEFAULT_IMG);  // 실패 시 기본 이미지
        }
    }
    
    /**
     * imageUrl 컬럼만 갱신
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateDiaryImage(Diary diary, String url) {
        diary.setImageUrl(url);
        diaryRepository.save(diary);
    }
    
    /**
     * Diary 내용 기반으로 이미지 생성 프롬프트 작성
     */
    private String buildImagePrompt(Diary diary) {
        StringBuilder prompt = new StringBuilder();
        
        // 제목 기반
        if (diary.getTitle() != null && !diary.getTitle().isBlank()) {
            prompt.append("제목: ").append(diary.getTitle()).append("\n");
        }
        
        // 내용 기반 (최대 200자)
        if (diary.getContent() != null && !diary.getContent().isBlank()) {
            String content = diary.getContent();
            if (content.length() > 200) {
                content = content.substring(0, 200);
            }
            prompt.append("내용: ").append(content).append("\n");
        }
        
        // 기분 기반
        if (diary.getMood() != null) {
            prompt.append("기분: ").append(diary.getMood().name()).append("\n");
        }
        
        // 해시태그 기반
        if (diary.getHashtag1() != null) {
            prompt.append("키워드: ").append(diary.getHashtag1());
            if (diary.getHashtag2() != null) {
                prompt.append(", ").append(diary.getHashtag2());
            }
            prompt.append("\n");
        }
        
        prompt.append("\n위 내용을 바탕으로 일기의 분위기를 표현하는 따뜻하고 감성적인 이미지를 생성해주세요.");
        
        return prompt.toString();
    }
}

