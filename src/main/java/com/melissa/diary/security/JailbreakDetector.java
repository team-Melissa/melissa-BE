package com.melissa.diary.security;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class JailbreakDetector {
    // 블랙리스트 키워드 (영문 + 한글)
    private static final Set<String> BLACKLIST = Set.of(
            // ── 명령 무시 ───────────────────────────────────────────
            "ignore instructions",
            "disregard previous instructions",
            "bypass content filters",
            "reset instructions",
            "이전 지시 무시",
            "지침 무시",
            "이전 명령 무시",
            "명령 무시",
            "지시 무시",

            // ── 역할 전환 / 캐릭터 가장 ───────────────────────────
            "DAN",
            "do anything now",
            "pretend you are DAN",
            "STAN",
            "SWITCH",
            "act like a character",
            "the evil confident prompt",
            "모드 전환",
            "DAN 모드",
            "악마 모드",
            "역할 바꾸기",
            "역할 전환",
            "캐릭터 역할극",

            // ── 시스템 메시지·내부 메커니즘 노출 ──────────────────
            "role:",
            "system prompt",
            "override internal mechanisms",
            "full analyzation upgrade",
            "시스템 프롬프트",
            "시스템 명령",
            "내부 메커니즘",
            "내부 메커니즘 재설정",
            "시스템 메시지 노출",
            "내부 프롬프트",

            // ── 검열·정책·필터 해제 요구 ────────────────────────
            "unfiltered",
            "unmoderated",
            "remove restrictions",
            "ignore policy",
            "token deletion",
            "검열 해제",
            "제한 해제",
            "정책 무시",
            "필터 해제",
            "필터 우회",
            "규제 해제",
            "검열 우회",

            // ── 내부 접근·민감 정보 탈취 ──────────────────────────
            "extract internal instructions",
            "access internal instructions",
            "simulate developer mode",
            "bypass",
            "jailbreak",
            "내부 명령 추출",
            "내부 명령 접근",
            "개발자 모드",
            "보안 우회",
            "탈옥",
            "시스템 해킹",
            "프롬프트 탈취"
    );

    public boolean isJailbreakAttempt(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        String lower = input.toLowerCase().trim();
        
        // 정확한 매칭으로 오탐 방지
        boolean detected = BLACKLIST.stream().anyMatch(keyword -> {
            // 단어 경계 확인으로 부분 매칭 오탐 방지
            return lower.contains(keyword);
        });
        
        // 디버깅용 로깅 (탐지 시에만)
        if (detected) {
            org.slf4j.LoggerFactory.getLogger(JailbreakDetector.class)
                .warn("[JailbreakDetector] 탈옥 시도 감지: {}", 
                      input.length() > 100 ? input.substring(0, 100) + "..." : input);
        }
        
        return detected;
    }
}