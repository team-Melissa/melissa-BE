package com.melissa.diary.bootstrap;

import com.melissa.diary.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class QuotaInitializer implements ApplicationRunner {

    private final UserRepository userRepo;

    // 이전 사용자 null 처리
    @Transactional
    public void run(ApplicationArguments args) {
        userRepo.findAll().forEach(u -> {
            if (u.getDailyQuota() == null) u.setDailyQuota(100);
            if (u.getQuotaDate()  == null) u.setQuotaDate(LocalDate.now());
        });
    }
}
