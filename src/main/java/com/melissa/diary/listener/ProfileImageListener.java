package com.melissa.diary.listener;

import com.melissa.diary.event.ProfileImageEvent;
import com.melissa.diary.service.AiProfileImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ProfileImageListener {

    private final AiProfileImageService imageSvc;

    @Async("asyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ProfileImageEvent e) {
        imageSvc.generateAndSaveProfileImage(e.profileId());
    }
}