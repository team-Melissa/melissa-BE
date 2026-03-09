package com.melissa.diary.service;

import com.melissa.diary.ai.ImageGenerator;
import com.melissa.diary.domain.Diary;
import com.melissa.diary.domain.enums.DiaryImageStatus;
import com.melissa.diary.repository.DiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryImageService {

    private static final String DEFAULT_IMG =
            "https://melissa-s3.s3.ap-northeast-2.amazonaws.com/default.png";

    private final DiaryRepository diaryRepository;
    private final ImageGenerator imageGenerator;
    private final DiaryImagePromptRefinerService refiner;

    public void generateAndSaveImage(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId).orElse(null);
        if (diary == null) {
            log.error("[Async-DiaryImage] diaryId={} not found", diaryId);
            return;
        }

        if (!diary.isActive()) {
            log.warn("[Async-DiaryImage] diaryId={} is inactive. skip image generation", diaryId);
            return;
        }

        if (diary.getImageStatus() != DiaryImageStatus.PENDING) {
            log.warn("[Async-DiaryImage] diaryId={} imageStatus={} is not PENDING. skip stale event",
                    diaryId, diary.getImageStatus());
            return;
        }

        String rawPrompt = buildImagePrompt(diary);
        String finalPrompt = refiner.refine(rawPrompt);

        try {
            String url = imageGenerator.genDiaryImage(finalPrompt);
            markImageReady(diaryId, url);
            log.info("[Async-DiaryImage] image generation completed. diaryId={}, url={}", diaryId, url);
        } catch (Exception e) {
            log.error("[Async-DiaryImage] diaryId={} processing failed", diaryId, e);
            markImageFailed(diaryId, DEFAULT_IMG);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markImageReady(Long diaryId, String url) {
        Diary diary = diaryRepository.findById(diaryId).orElse(null);
        if (diary == null || diary.getImageStatus() != DiaryImageStatus.PENDING) {
            return;
        }

        diary.markImageReady(url);
        diaryRepository.save(diary);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markImageFailed(Long diaryId, String fallbackUrl) {
        Diary diary = diaryRepository.findById(diaryId).orElse(null);
        if (diary == null || diary.getImageStatus() != DiaryImageStatus.PENDING) {
            return;
        }

        diary.markImageFailed(fallbackUrl);
        diaryRepository.save(diary);
    }

    private String buildImagePrompt(Diary diary) {
        StringBuilder prompt = new StringBuilder();

        if (diary.getTitle() != null && !diary.getTitle().isBlank()) {
            prompt.append("제목: ").append(diary.getTitle()).append("\n");
        }

        if (diary.getContent() != null && !diary.getContent().isBlank()) {
            String content = diary.getContent();
            if (content.length() > 200) {
                content = content.substring(0, 200);
            }
            prompt.append("내용: ").append(content).append("\n");
        }

        if (diary.getMood() != null) {
            prompt.append("기분: ").append(diary.getMood().name()).append("\n");
        }

        if (diary.getHashtag1() != null) {
            prompt.append("키워드: ").append(diary.getHashtag1());
            if (diary.getHashtag2() != null) {
                prompt.append(", ").append(diary.getHashtag2());
            }
            prompt.append("\n");
        }

        prompt.append("\n위 내용을 바탕으로 감성적인 일기 삽화를 생성해 주세요.");

        return prompt.toString();
    }
}

