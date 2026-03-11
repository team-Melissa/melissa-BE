package com.melissa.diary.scheduler;

import com.melissa.diary.repository.UserSettingRepository;
import com.melissa.diary.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationSchedulerTest {

    @Test
    void floorToTenMinuteSlotRoundsDownWithZeroSeconds() {
        LocalTime result = NotificationScheduler.floorToTenMinuteSlot(LocalTime.of(23, 17, 42));

        assertThat(result).isEqualTo(LocalTime.of(23, 10, 0));
    }

    @Test
    void sendDailyNotificationsUsesKstDateForTargetQuery() {
        UserSettingRepository repository = mock(UserSettingRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        NotificationScheduler scheduler = new NotificationScheduler(repository, notificationService);

        when(repository.findNotificationTargets(any(Time.class), any(LocalDate.class)))
                .thenReturn(List.of());

        scheduler.sendDailyNotifications();

        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(repository).findNotificationTargets(any(Time.class), dateCaptor.capture());
        verify(notificationService, never()).sendBatchNotifications(any());

        LocalDate expectedKstDate = LocalDate.now(ZoneId.of("Asia/Seoul"));
        assertThat(dateCaptor.getValue()).isEqualTo(expectedKstDate);
    }
}
