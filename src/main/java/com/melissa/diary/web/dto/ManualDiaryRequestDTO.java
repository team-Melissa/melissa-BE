package com.melissa.diary.web.dto;

import com.melissa.diary.domain.enums.Mood;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ManualDiaryRequestDTO {

    @Getter
    @Setter
    public static class ManualDiaryCreateRequest {
        @NotNull(message = "연도는 필수입니다.")
        private Integer year;

        @NotNull(message = "월은 필수입니다.")
        private Integer month;

        @NotNull(message = "일은 필수입니다.")
        private Integer day;

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 30, message = "제목은 30자 이하여야 합니다.")
        private String title;

        @NotNull(message = "기분은 필수입니다.")
        private Mood mood;

        @NotBlank(message = "일기 내용은 필수입니다.")
        @Size(max = 300, message = "일기 내용은 300자 이하여야 합니다.")
        private String content;

        @Size(max = 30, message = "해시태그1은 30자 이하여야 합니다.")
        private String hashtag1;

        @Size(max = 30, message = "해시태그2는 30자 이하여야 합니다.")
        private String hashtag2;

        @NotNull(message = "AI 프로필 ID는 필수입니다.")
        private Long aiProfileId;

        private Boolean generateImage = true; // 기본값으로 이미지 생성
    }

    @Getter
    @Setter
    public static class ManualDiaryUpdateRequest {
        @Size(max = 30, message = "제목은 30자 이하여야 합니다.")
        private String title;

        private Mood mood;

        @Size(max = 1000, message = "일기 내용은 1000자 이하여야 합니다.")
        private String content;

        @Size(max = 30, message = "해시태그1은 30자 이하여야 합니다.")
        private String hashtag1;

        @Size(max = 30, message = "해시태그2는 30자 이하여야 합니다.")
        private String hashtag2;

        private Boolean generateImage = false; // 수정 시에는 기본적으로 이미지 재생성 안함
    }
}
