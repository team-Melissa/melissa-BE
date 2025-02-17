package com.melissa.diary.web.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class ThreadResponseDTO {
    @Getter
    @Builder
    public static class ThreadResponse {
        private Long threadId; // 이후 채팅로그 조회시 이용
        // 생성시 -> 생성된 id, 재생성시 -> 기존 id, 삭제시 -> 삭제된 id
        private int year;
        private int month;
        private int day;
    }
    @Getter
    @Builder
    public static class ChatResponse{
        private Long chatId;
        private String role;
        private String content;
        private LocalDateTime createAt;
        private String aiProfileName; // 채팅에 저장된 ai 프로필의 이름
        private String aiProfileImageS3; // 채팅에 저장된 ai 프로필의 이미지
    }

    @Getter
    @Builder
    public static class ChatListResponse{
        private String aiProfileName; // Thread에 저장된 ai 프로필의 이름
        private String aiProfileImageS3; // Thread에 저장된 ai 프로필의 이미지
        private List<ChatResponse> chats;
    }

}
