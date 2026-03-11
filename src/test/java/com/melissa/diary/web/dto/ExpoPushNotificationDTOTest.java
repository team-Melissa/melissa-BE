package com.melissa.diary.web.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExpoPushNotificationDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesWhenDataIsArray() throws Exception {
        String json = """
                {
                  "data": [
                    {
                      "status": "ok",
                      "id": "ticket-array-1"
                    }
                  ]
                }
                """;

        ExpoPushNotificationDTO.PushResponse response =
                objectMapper.readValue(json, ExpoPushNotificationDTO.PushResponse.class);

        assertThat(response.getData()).isNotNull().hasSize(1);
        assertThat(response.getData().get(0).getStatus()).isEqualTo("ok");
        assertThat(response.getData().get(0).getId()).isEqualTo("ticket-array-1");
    }

    @Test
    void deserializesWhenDataIsSingleObject() throws Exception {
        String json = """
                {
                  "data": {
                    "status": "error",
                    "message": "The recipient device is not registered with FCM.",
                    "details": {
                      "error": "DeviceNotRegistered"
                    }
                  }
                }
                """;

        ExpoPushNotificationDTO.PushResponse response =
                objectMapper.readValue(json, ExpoPushNotificationDTO.PushResponse.class);

        assertThat(response.getData()).isNotNull().hasSize(1);
        assertThat(response.getData().get(0).getStatus()).isEqualTo("error");
        assertThat(response.getData().get(0).getDetails()).isNotNull();
        assertThat(response.getData().get(0).getDetails().getError()).isEqualTo("DeviceNotRegistered");
    }
}
