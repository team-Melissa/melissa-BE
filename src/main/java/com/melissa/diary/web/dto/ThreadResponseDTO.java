package com.melissa.diary.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class ThreadResponseDTO {
    
    @Getter
    @Builder
    @Schema(description = "스레드 응답")
    public static class ThreadResponse {
        
        @Schema(description = "스레드 ID (생성/재생성/삭제된 ID)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long threadId;
        
        @Schema(description = "년도", example = "2025", requiredMode = Schema.RequiredMode.REQUIRED)
        private int year;
        
        @Schema(description = "월", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private int month;
        
        @Schema(description = "일", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
        private int day;
    }
    
    @Getter
    @Builder
    @Schema(description = "채팅 응답")
    public static class ChatResponse {
        
        @Schema(description = "채팅 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long chatId;
        
        @Schema(description = "발신자 역할", example = "USER", 
                allowableValues = {"AI", "USER"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String role;
        
        @Schema(description = "메시지 내용", example = "안녕하세요!", requiredMode = Schema.RequiredMode.REQUIRED)
        private String content;
        
        @Schema(description = "생성일시", example = "2025-01-01T12:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime createAt;
        
        @Schema(description = "AI 프로필 이름 (서버 주입)", example = "루나", requiredMode = Schema.RequiredMode.REQUIRED)
        private String aiProfileName;
        
        @Schema(description = "AI 프로필 이미지 URL (서버 주입)", example = "https://s3.amazonaws.com/...", requiredMode = Schema.RequiredMode.REQUIRED)
        private String aiProfileImageS3;
    }

    @Getter
    @Builder
    @Schema(description = "채팅 목록 응답")
    public static class ChatListResponse {
        
        @Schema(description = "스레드의 AI 프로필 이름 (서버 주입)", example = "루나", requiredMode = Schema.RequiredMode.REQUIRED)
        private String aiProfileName;
        
        @Schema(description = "스레드의 AI 프로필 이미지 URL (서버 주입)", example = "https://s3.amazonaws.com/...", requiredMode = Schema.RequiredMode.REQUIRED)
        private String aiProfileImageS3;
        
        @Schema(description = "채팅 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<ChatResponse> chats;
    }
}
