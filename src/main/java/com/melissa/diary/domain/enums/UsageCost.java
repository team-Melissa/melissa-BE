package com.melissa.diary.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UsageCost {
    CHAT(3), SUMMARY(10), PROFILE(10);
    private final int cost;
}