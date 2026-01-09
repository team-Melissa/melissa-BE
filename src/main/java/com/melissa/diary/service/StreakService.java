package com.melissa.diary.service;

import com.melissa.diary.repository.DiaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StreakService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int RECENT_DAYS_LIMIT = 400; // 충분히 크게 잡고, 실제 계산은 첫 gap에서 즉시 종료

    private final DiaryRepository diaryRepository;

    public int getCurrentStreakDays(Long userId) {
        LocalDate todayKst = LocalDate.now(KST);
        return getCurrentStreakDays(userId, todayKst);
    }

    /**
     * 테스트 용이성을 위해 today를 외부에서 주입할 수 있게 분리
     */
    int getCurrentStreakDays(Long userId, LocalDate today) {
        List<Date> recentDates = diaryRepository.findRecentActiveDiaryDatesDesc(
                userId,
                Date.valueOf(today),
                RECENT_DAYS_LIMIT
        );

        if (recentDates.isEmpty()) {
            return 0;
        }

        LocalDate first = recentDates.get(0).toLocalDate();
        if (!first.equals(today)) {
            return 0; // 오늘 작성이 없으면 스트릭 0
        }

        int streak = 1;
        LocalDate expected = today.minusDays(1);

        for (int i = 1; i < recentDates.size(); i++) {
            LocalDate d = recentDates.get(i).toLocalDate();
            if (d.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else {
                break;
            }
        }

        return streak;
    }
}


