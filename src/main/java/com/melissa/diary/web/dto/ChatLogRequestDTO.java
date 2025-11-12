package com.melissa.diary.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class ChatLogRequestDTO {

    @Getter
    @Setter
    public static class ChatLogUpdateRequest {
        @NotBlank(message = "메시지 내용은 필수입니다.")
        @Size(max = 1000, message = "메시지 내용은 1000자 이하여야 합니다.")
        @Schema(description = "수정할 메시지 내용", example = "수정된 메시지 내용입니다.")
        private String content;
    }
}

