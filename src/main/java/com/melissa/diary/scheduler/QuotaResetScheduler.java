package com.melissa.diary.scheduler;

import com.melissa.diary.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class QuotaResetScheduler {

    private final UserRepository userRepo;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void reset() {
        // 스케줄러를 통해 매일 100으로 초기화
        userRepo.resetAll(LocalDate.now());
    }
}
