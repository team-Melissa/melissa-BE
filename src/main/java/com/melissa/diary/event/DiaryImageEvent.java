package com.melissa.diary.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Diary 이미지 생성 이벤트
 */
@Getter
@AllArgsConstructor
public class DiaryImageEvent {
    private final Long diaryId;
}

