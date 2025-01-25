package com.melissa.diary.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Mood {
    HAPPY("행복"), SAD("슬픔"); // 추가 필요
    private String name;
}
