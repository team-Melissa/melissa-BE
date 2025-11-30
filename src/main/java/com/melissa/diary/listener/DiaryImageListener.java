package com.melissa.diary.listener;

import com.melissa.diary.event.DiaryImageEvent;
import com.melissa.diary.service.DiaryImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Diary 이미지 생성 이벤트 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiaryImageListener {
    
    private final DiaryImageService diaryImageService;
    
    @Async("asyncTaskExecutor")
    @EventListener
    public void handleDiaryImageEvent(DiaryImageEvent event) {
        log.info("[DiaryImageListener] 이미지 생성 시작. diaryId={}", event.getDiaryId());
        diaryImageService.generateAndSaveImage(event.getDiaryId());
    }
}

