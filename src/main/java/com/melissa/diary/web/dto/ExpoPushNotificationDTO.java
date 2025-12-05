package com.melissa.diary.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Expo Push Notification API 통신용 DTO
 */
public class ExpoPushNotificationDTO {
    
    /**
     * Expo Push API 요청 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PushRequest {
        
        @JsonProperty("to")
        private String to;  // ExponentPushToken[xxx]
        
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("body")
        private String body;
        
        @JsonProperty("data")
        private Object data;  // 추가 데이터 (optional)
        
        @JsonProperty("sound")
        @Builder.Default
        private String sound = "default";
        
        @JsonProperty("priority")
        @Builder.Default
        private String priority = "high";
    }
    
    /**
     * Expo Push API 응답 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PushResponse {
        
        @JsonProperty("data")
        private List<PushTicket> data;
    }
    
    /**
     * Expo Push Ticket (개별 발송 결과)
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PushTicket {
        
        @JsonProperty("status")
        private String status;  // "ok" or "error"
        
        @JsonProperty("id")
        private String id;  // Ticket ID (status가 "ok"인 경우)
        
        @JsonProperty("message")
        private String message;  // 에러 메시지 (status가 "error"인 경우)
        
        @JsonProperty("details")
        private PushErrorDetail details;  // 에러 상세 (status가 "error"인 경우)
    }
    
    /**
     * Expo Push Error 상세 정보
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PushErrorDetail {
        
        @JsonProperty("error")
        private String error;  // "DeviceNotRegistered", "InvalidCredentials", "MessageTooBig" 등
    }
}

