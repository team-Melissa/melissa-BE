package com.melissa.diary.web.dto;

import com.melissa.diary.domain.enums.Mood;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ManualDiaryRequestDTO {

    @Getter
    @Setter
    public static class ManualDiaryCreateRequest {
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 30, message = "제목은 30자 이하여야 합니다.")
        @Schema(description = "일기 제목", example = "오늘의 즐거운 하루")
        private String title;

        @NotNull(message = "기분은 필수입니다.")
        @Schema(description = "기분 상태 (HAPPY, SAD, TIRED, ANGRY, RELAX 중 선택)", 
                example = "HAPPY", 
                allowableValues = {"HAPPY", "SAD", "TIRED", "ANGRY", "RELAX"})
        private Mood mood;

        @NotBlank(message = "일기 내용은 필수입니다.")
        @Size(max = 1000, message = "일기 내용은 1000자 이하여야 합니다.")
        @Schema(description = "일기 내용", example = "오늘은 친구들과 카페에서 즐거운 시간을 보냈다...")
        private String content;

        @Size(max = 30, message = "해시태그1은 30자 이하여야 합니다.")
        @Schema(description = "첫 번째 해시태그", example = "행복")
        private String hashtag1;

        @Size(max = 30, message = "해시태그2는 30자 이하여야 합니다.")
        @Schema(description = "두 번째 해시태그", example = "일상")
        private String hashtag2;

        @Schema(description = "이미지 생성 여부 (true: 생성, false: 생성 안함)", 
                example = "true", 
                defaultValue = "true")
        private Boolean generateImage = true; // 기본값으로 이미지 생성
    }

    @Getter
    @Setter
    public static class ManualDiaryUpdateRequest {
        @Size(max = 30, message = "제목은 30자 이하여야 합니다.")
        @Schema(description = "일기 제목 (수정할 경우만)", example = "수정된 제목")
        private String title;

        @Schema(description = "기분 상태 (수정할 경우만, HAPPY, SAD, TIRED, ANGRY, RELAX 중 선택)", 
                example = "HAPPY", 
                allowableValues = {"HAPPY", "SAD", "TIRED", "ANGRY", "RELAX"})
        private Mood mood;

        @Size(max = 1000, message = "일기 내용은 1000자 이하여야 합니다.")
        @Schema(description = "일기 내용 (수정할 경우만)", example = "수정된 일기 내용...")
        private String content;

        @Size(max = 30, message = "해시태그1은 30자 이하여야 합니다.")
        @Schema(description = "첫 번째 해시태그 (수정할 경우만)", example = "수정된태그")
        private String hashtag1;

        @Size(max = 30, message = "해시태그2는 30자 이하여야 합니다.")
        @Schema(description = "두 번째 해시태그 (수정할 경우만)", example = "새태그")
        private String hashtag2;

        @Schema(description = "이미지 재생성 여부 (true: 재생성, false: 기존 이미지 유지)", 
                example = "false", 
                defaultValue = "false")
        private Boolean generateImage = false; // 수정 시에는 기본적으로 이미지 재생성 안함
    }
}
