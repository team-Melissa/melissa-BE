package com.melissa.diary.web.dto;

import lombok.Builder;
import lombok.Getter;

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
}
