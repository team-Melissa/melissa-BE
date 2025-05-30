package com.melissa.diary.listener;

import com.melissa.diary.event.ThreadImageEvent;
import com.melissa.diary.service.ThreadImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ThreadImageListener {

    private final ThreadImageService imageSvc;

    @Async("asyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ThreadImageEvent e) {
        imageSvc.generateAndSaveImage(e.threadId());
    }
}