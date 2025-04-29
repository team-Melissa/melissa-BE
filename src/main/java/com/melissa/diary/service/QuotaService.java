package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.enums.UsageCost;
import com.melissa.diary.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class QuotaService {

    private final UserRepository userRepo;

    @Transactional
    public void checkAndConsume(Long userId, UsageCost type) {
        User u = userRepo.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        /* 날짜 바뀌면 초기화 */
        if (!u.getQuotaDate().equals(LocalDate.now())) {
            u.setDailyQuota(100);
            u.setQuotaDate(LocalDate.now());
        }

        if (u.getDailyQuota() < type.getCost())
            throw new ErrorHandler(ErrorStatus.QUOTA_LIMIT_EXCEEDED);

        u.setDailyQuota(u.getDailyQuota() - type.getCost());
    }
}
