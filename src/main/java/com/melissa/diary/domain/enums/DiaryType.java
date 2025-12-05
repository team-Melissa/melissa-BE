package com.melissa.diary.domain.enums;

/**
 * 일기 생성 방식 구분
 * - MANUAL: 사용자가 직접 작성한 일기
 * - CHAT_BASED: 채팅 로그 기반 자동 생성 일기
 */
public enum DiaryType {
    MANUAL,        // 수동 작성
    CHAT_BASED     // 채팅 기반 자동 생성
}

