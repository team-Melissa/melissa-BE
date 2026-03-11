package com.melissa.diary.service;

import com.melissa.diary.domain.User;
import com.melissa.diary.domain.UserSetting;
import com.melissa.diary.repository.ExpoPushTokenRepository;
import com.melissa.diary.repository.UserSettingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class NotificationServiceStateModelTest {

    @Test
    void marksFailedAttemptWhenNoValidToken() {
        UserSettingRepository userSettingRepository = mock(UserSettingRepository.class);
        ExpoPushTokenRepository expoPushTokenRepository = mock(ExpoPushTokenRepository.class);
        WebClient webClient = mock(WebClient.class);
        NotificationService service = new NotificationService(userSettingRepository, expoPushTokenRepository, webClient);

        User user = User.builder()
                .id(1L)
                .expoPushTokenList(new ArrayList<>())
                .build();
        UserSetting setting = UserSetting.builder()
                .id(10L)
                .user(user)
                .notificationEnabled(true)
                .notificationTime(Time.valueOf("23:00:00"))
                .retryCount(0)
                .build();

        boolean delivered = service.sendNotificationToUser(setting);

        assertThat(delivered).isFalse();
        assertThat(setting.getRetryCount()).isEqualTo(1);
        assertThat(setting.getLastAttemptAt()).isNotNull();
        verify(userSettingRepository).save(setting);
        verify(expoPushTokenRepository, never()).save(any());
    }

    @Test
    void resetsRetryCountOnNewDayFailure() {
        UserSettingRepository userSettingRepository = mock(UserSettingRepository.class);
        ExpoPushTokenRepository expoPushTokenRepository = mock(ExpoPushTokenRepository.class);
        WebClient webClient = mock(WebClient.class);
        NotificationService service = new NotificationService(userSettingRepository, expoPushTokenRepository, webClient);

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDateTime yesterdayAttempt = today.minusDays(1).atTime(23, 0);

        User user = User.builder()
                .id(1L)
                .expoPushTokenList(new ArrayList<>())
                .build();
        UserSetting setting = UserSetting.builder()
                .id(11L)
                .user(user)
                .notificationEnabled(true)
                .notificationTime(Time.valueOf("23:00:00"))
                .retryCount(5)
                .lastAttemptAt(yesterdayAttempt)
                .build();

        boolean delivered = service.sendNotificationToUser(setting);

        assertThat(delivered).isFalse();
        assertThat(setting.getRetryCount()).isEqualTo(1);
        assertThat(setting.getLastAttemptAt()).isAfter(yesterdayAttempt);
        verify(userSettingRepository).save(setting);
    }

    @Test
    void skipsWhenAlreadySentToday() {
        UserSettingRepository userSettingRepository = mock(UserSettingRepository.class);
        ExpoPushTokenRepository expoPushTokenRepository = mock(ExpoPushTokenRepository.class);
        WebClient webClient = mock(WebClient.class);
        NotificationService service = new NotificationService(userSettingRepository, expoPushTokenRepository, webClient);

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        User user = User.builder()
                .id(1L)
                .expoPushTokenList(new ArrayList<>())
                .build();
        UserSetting setting = UserSetting.builder()
                .id(12L)
                .user(user)
                .notificationEnabled(true)
                .notificationTime(Time.valueOf("23:00:00"))
                .lastSentDate(today)
                .retryCount(3)
                .build();

        boolean delivered = service.sendNotificationToUser(setting);

        assertThat(delivered).isTrue();
        assertThat(setting.getRetryCount()).isEqualTo(3);
        verify(userSettingRepository, never()).save(any(UserSetting.class));
        verify(expoPushTokenRepository, never()).save(any());
    }
}
