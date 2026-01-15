package com.melissa.diary.service;

import com.melissa.diary.repository.DiaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

    @Mock
    DiaryRepository diaryRepository;

    @InjectMocks
    StreakService streakService;

    @Test
    void 오늘작성없어도_어제작성있으면_유지() {
        Long userId = 1L;
        LocalDate today = LocalDate.of(2026, 1, 10);

        when(diaryRepository.findRecentActiveDiaryDatesDesc(eq(userId), eq(Date.valueOf(today)), anyInt()))
                .thenReturn(List.of(Date.valueOf(today.minusDays(1))));

        int streak = streakService.getCurrentStreakDays(userId, today);
        assertThat(streak).isEqualTo(1);
    }

    @Test
    void 오늘작성없고_어제도없으면_0() {
        Long userId = 1L;
        LocalDate today = LocalDate.of(2026, 1, 10);

        when(diaryRepository.findRecentActiveDiaryDatesDesc(eq(userId), eq(Date.valueOf(today)), anyInt()))
                .thenReturn(List.of(Date.valueOf(today.minusDays(2))));

        int streak = streakService.getCurrentStreakDays(userId, today);
        assertThat(streak).isEqualTo(0);
    }

    @Test
    void 오늘부터연속이면_연속길이() {
        Long userId = 1L;
        LocalDate today = LocalDate.of(2026, 1, 10);

        when(diaryRepository.findRecentActiveDiaryDatesDesc(eq(userId), eq(Date.valueOf(today)), anyInt()))
                .thenReturn(List.of(
                        Date.valueOf(today),
                        Date.valueOf(today.minusDays(1)),
                        Date.valueOf(today.minusDays(2)),
                        Date.valueOf(today.minusDays(4)) // gap -> 여기서 끊김
                ));

        int streak = streakService.getCurrentStreakDays(userId, today);
        assertThat(streak).isEqualTo(3);
    }

    @Test
    void 기록없으면_0() {
        Long userId = 1L;
        LocalDate today = LocalDate.of(2026, 1, 10);

        when(diaryRepository.findRecentActiveDiaryDatesDesc(eq(userId), any(), anyInt()))
                .thenReturn(List.of());

        int streak = streakService.getCurrentStreakDays(userId, today);
        assertThat(streak).isEqualTo(0);
    }
}


