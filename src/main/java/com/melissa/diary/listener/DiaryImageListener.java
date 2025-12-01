package com.melissa.diary.listener;

import com.melissa.diary.event.DiaryImageEvent;
import com.melissa.diary.service.DiaryImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Diary 이미지 생성 이벤트 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiaryImageListener {
    
    private final DiaryImageService diaryImageService;
    
    @Async("asyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDiaryImageEvent(DiaryImageEvent event) {
        log.info("[DiaryImageListener] 이미지 생성 시작. diaryId={}", event.getDiaryId());
        diaryImageService.generateAndSaveImage(event.getDiaryId());
    }
}

