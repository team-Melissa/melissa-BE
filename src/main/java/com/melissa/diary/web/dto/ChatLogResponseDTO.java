package com.melissa.diary.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class ChatLogResponseDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "채팅 로그 응답")
    public static class ChatLogResponse {
        
        @Schema(description = "채팅 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long chatId;
        
        @Schema(description = "발신자 역할", example = "USER", 
                allowableValues = {"AI", "USER"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String role;
        
        @Schema(description = "메시지 내용", example = "안녕하세요!", requiredMode = Schema.RequiredMode.REQUIRED)
        private String content;
        
        @Schema(description = "생성일시", example = "2025-01-01T12:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "채팅 로그 삭제 응답")
    public static class ChatLogDeleteResponse {
        
        @Schema(description = "삭제된 채팅 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long chatId;
        
        @Schema(description = "결과 메시지", example = "채팅 메시지가 삭제되었습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        private String message;
    }
}

