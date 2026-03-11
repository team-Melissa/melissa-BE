package com.melissa.diary.web.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExpoPushRequestSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void omitsDataWhenNull() throws Exception {
        ExpoPushNotificationDTO.PushRequest request = ExpoPushNotificationDTO.PushRequest.builder()
                .to("ExponentPushToken[test]")
                .title("title")
                .body("body")
                .data(null)
                .sound("default")
                .priority("high")
                .build();

        String json = objectMapper.writeValueAsString(request);

        assertThat(json).doesNotContain("\"data\"");
    }

    @Test
    void includesDataWhenObjectPresent() throws Exception {
        ExpoPushNotificationDTO.PushRequest request = ExpoPushNotificationDTO.PushRequest.builder()
                .to("ExponentPushToken[test]")
                .title("title")
                .body("body")
                .data(new Payload("daily_reminder", 1L))
                .sound("default")
                .priority("high")
                .build();

        String json = objectMapper.writeValueAsString(request);

        assertThat(json).contains("\"data\"");
        assertThat(json).contains("\"type\":\"daily_reminder\"");
        assertThat(json).contains("\"userId\":1");
    }

    private record Payload(String type, Long userId) {}
}
