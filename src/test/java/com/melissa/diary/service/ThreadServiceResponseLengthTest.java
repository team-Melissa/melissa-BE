package com.melissa.diary.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ThreadService의 AI 응답 길이 자동 조절 기능 테스트
 * calculateMaxTokens() 메서드의 입력 길이별 토큰 계산 검증
 */
class ThreadServiceResponseLengthTest {

    private ThreadService threadService;

    @BeforeEach
    void setUp() {
        // ThreadService의 의존성은 null이어도 됨 (calculateMaxTokens는 의존성 없는 순수 함수)
        threadService = new ThreadService(null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("calculateMaxTokens: null 입력 → 기본값 50 토큰")
    void testCalculateMaxTokens_NullInput() {
        // given
        String input = null;

        // when
        Integer result = ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", input);

        // then
        assertNotNull(result);
        assertEquals(50, result, "null 입력 시 기본값 50 토큰이어야 함");
    }

    @Test
    @DisplayName("calculateMaxTokens: 빈 문자열 입력 → 기본값 50 토큰")
    void testCalculateMaxTokens_EmptyInput() {
        // given
        String input = "";

        // when
        Integer result = ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", input);

        // then
        assertNotNull(result);
        assertEquals(50, result, "빈 문자열 입력 시 기본값 50 토큰이어야 함");
    }

    @Test
    @DisplayName("calculateMaxTokens: 매우 짧은 입력(5자) → 50 토큰")
    void testCalculateMaxTokens_VeryShortInput() {
        // given
        String input = "기분좋아"; // 4자

        // when
        Integer result = ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", input);

        // then
        assertNotNull(result);
        assertEquals(50, result, "5자 이하 입력 시 50 토큰이어야 함");
    }

    @Test
    @DisplayName("calculateMaxTokens: 짧은 입력(10자) → 50 토큰")
    void testCalculateMaxTokens_ShortInput() {
        // given
        String input = "오늘 기분 좋아"; // 8자

        // when
        Integer result = ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", input);

        // then
        assertNotNull(result);
        assertEquals(50, result, "20자 이하 입력 시 50 토큰이어야 함");
    }

    @Test
    @DisplayName("calculateMaxTokens: 경계값 테스트(정확히 20자) → 50 토큰")
    void testCalculateMaxTokens_Exactly20Chars() {
        // given
        String input = "12345678901234567890"; // 정확히 20자

        // when
        Integer result = ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", input);

        // then
        assertNotNull(result);
        assertEquals(50, result, "정확히 20자 입력 시 50 토큰이어야 함");
    }

    @Test
    @DisplayName("calculateMaxTokens: 경계값 테스트(21자) → 120 토큰")
    void testCalculateMaxTokens_Exactly21Chars() {
        // given
        String input = "123456789012345678901"; // 정확히 21자

        // when
        Integer result = ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", input);

        // then
        assertNotNull(result);
        assertEquals(120, result, "21자 입력 시 120 토큰이어야 함");
    }

    @Test
    @DisplayName("calculateMaxTokens: 중간 입력(50자) → 120 토큰")
    void testCalculateMaxTokens_MediumInput() {
        // given
        String input = "오늘 회사에서 프로젝트 발표했는데 긴장됐어. 그래도 잘 끝났어."; // 약 32자

        // when
        Integer result = ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", input);

        // then
        assertNotNull(result);
        assertEquals(120, result, "21~100자 입력 시 120 토큰이어야 함");
    }

    @Test
    @DisplayName("calculateMaxTokens: 경계값 테스트(정확히 100자) → 120 토큰")
    void testCalculateMaxTokens_Exactly100Chars() {
        // given
        String input = "가".repeat(100); // 정확히 100자

        // when
        Integer result = ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", input);

        // then
        assertNotNull(result);
        assertEquals(120, result, "정확히 100자 입력 시 120 토큰이어야 함");
    }

    @Test
    @DisplayName("calculateMaxTokens: 경계값 테스트(101자) → 200 토큰")
    void testCalculateMaxTokens_Exactly101Chars() {
        // given
        String input = "가".repeat(101); // 정확히 101자

        // when
        Integer result = ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", input);

        // then
        assertNotNull(result);
        assertEquals(200, result, "101자 입력 시 200 토큰이어야 함");
    }

    @Test
    @DisplayName("calculateMaxTokens: 긴 입력(200자) → 200 토큰")
    void testCalculateMaxTokens_LongInput() {
        // given
        String input = "가".repeat(200); // 200자

        // when
        Integer result = ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", input);

        // then
        assertNotNull(result);
        assertEquals(200, result, "101~300자 입력 시 200 토큰이어야 함");
    }

    @Test
    @DisplayName("calculateMaxTokens: 경계값 테스트(정확히 300자) → 200 토큰")
    void testCalculateMaxTokens_Exactly300Chars() {
        // given
        String input = "가".repeat(300); // 정확히 300자

        // when
        Integer result = ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", input);

        // then
        assertNotNull(result);
        assertEquals(200, result, "정확히 300자 입력 시 200 토큰이어야 함");
    }

    @Test
    @DisplayName("calculateMaxTokens: 경계값 테스트(301자) → 300 토큰")
    void testCalculateMaxTokens_Exactly301Chars() {
        // given
        String input = "가".repeat(301); // 정확히 301자

        // when
        Integer result = ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", input);

        // then
        assertNotNull(result);
        assertEquals(300, result, "301자 입력 시 300 토큰이어야 함");
    }

    @Test
    @DisplayName("calculateMaxTokens: 매우 긴 입력(500자) → 300 토큰")
    void testCalculateMaxTokens_VeryLongInput() {
        // given
        String input = "가".repeat(500); // 500자

        // when
        Integer result = ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", input);

        // then
        assertNotNull(result);
        assertEquals(300, result, "301자 이상 입력 시 300 토큰이어야 함");
    }

    @Test
    @DisplayName("calculateMaxTokens: 공백 포함 입력(trim 테스트)")
    void testCalculateMaxTokens_WithWhitespace() {
        // given
        String input = "   오늘 기분 좋아   "; // 앞뒤 공백 포함, trim 후 8자

        // when
        Integer result = ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", input);

        // then
        assertNotNull(result);
        assertEquals(50, result, "trim 후 20자 이하이므로 50 토큰이어야 함");
    }

    @Test
    @DisplayName("calculateMaxTokens: 실제 사용 예시 - 간단한 감정 표현")
    void testCalculateMaxTokens_RealExample_ShortEmotion() {
        // given
        String[] shortInputs = {
            "피곤해",
            "행복해",
            "슬퍼",
            "좋아",
            "오늘 기분 좋아",
            "너무 힘들어"
        };

        // when & then
        for (String input : shortInputs) {
            Integer result = ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", input);
            assertNotNull(result);
            assertEquals(50, result, 
                String.format("'%s'(%d자)는 짧은 입력이므로 50 토큰이어야 함", input, input.length()));
        }
    }

    @Test
    @DisplayName("calculateMaxTokens: 실제 사용 예시 - 일반 메모")
    void testCalculateMaxTokens_RealExample_NormalMemo() {
        // given
        String[] mediumInputs = {
            "오늘 카페 갔다가 맛있는 케이크 먹었어",
            "회사에서 회의하느라 피곤했지만 저녁은 맛있게 먹었어",
            "친구랑 오랜만에 만나서 수다 떨었는데 시간 가는 줄 몰랐어"
        };

        // when & then
        for (String input : mediumInputs) {
            Integer result = ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", input);
            assertNotNull(result);
            assertEquals(120, result,
                String.format("'%s'(%d자)는 일반 입력이므로 120 토큰이어야 함", 
                    input.substring(0, Math.min(20, input.length())) + "...", input.length()));
        }
    }

    @Test
    @DisplayName("calculateMaxTokens: 실제 사용 예시 - 상세한 일기")
    void testCalculateMaxTokens_RealExample_DetailedDiary() {
        // given
        String input = "오늘은 정말 특별한 하루였어. 아침에 일어나서 날씨가 너무 좋아서 기분이 좋았고, " +
                      "회사에 가는 길에 예쁜 꽃들을 봤어. 회사에서는 팀 프로젝트 발표가 있었는데 " +
                      "많이 긴장했지만 팀원들이 도와줘서 무사히 끝낼 수 있었어. 점심에는 동료들이랑 " +
                      "새로 생긴 맛집에 갔는데 진짜 맛있었어."; // 약 150자

        // when
        Integer result = ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", input);

        // then
        assertNotNull(result);
        assertEquals(200, result, "상세한 일기(101~300자)는 200 토큰이어야 함");
    }

    @Test
    @DisplayName("calculateMaxTokens: 한글과 영문 혼합 입력")
    void testCalculateMaxTokens_MixedKoreanEnglish() {
        // given
        String input = "Today was a great day! 오늘 정말 좋은 하루였어."; // 약 31자

        // when
        Integer result = ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", input);

        // then
        assertNotNull(result);
        assertEquals(120, result, "31자 혼합 입력은 120 토큰이어야 함");
    }

    @Test
    @DisplayName("calculateMaxTokens: 특수문자 포함 입력")
    void testCalculateMaxTokens_WithSpecialChars() {
        // given
        String input = "오늘 기분 좋아!!! ^^"; // 약 13자

        // when
        Integer result = ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", input);

        // then
        assertNotNull(result);
        assertEquals(50, result, "특수문자 포함 13자 입력은 50 토큰이어야 함");
    }

    @Test
    @DisplayName("calculateMaxTokens: 모든 경계값 검증")
    void testCalculateMaxTokens_AllBoundaries() {
        // given & when & then
        // 20자 이하
        assertEquals(Integer.valueOf(50), ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", "가".repeat(1)));
        assertEquals(Integer.valueOf(50), ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", "가".repeat(10)));
        assertEquals(Integer.valueOf(50), ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", "가".repeat(20)));
        
        // 21~100자
        assertEquals(Integer.valueOf(120), ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", "가".repeat(21)));
        assertEquals(Integer.valueOf(120), ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", "가".repeat(50)));
        assertEquals(Integer.valueOf(120), ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", "가".repeat(100)));
        
        // 101~300자
        assertEquals(Integer.valueOf(200), ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", "가".repeat(101)));
        assertEquals(Integer.valueOf(200), ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", "가".repeat(200)));
        assertEquals(Integer.valueOf(200), ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", "가".repeat(300)));
        
        // 301자 이상
        assertEquals(Integer.valueOf(300), ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", "가".repeat(301)));
        assertEquals(Integer.valueOf(300), ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", "가".repeat(500)));
        assertEquals(Integer.valueOf(300), ReflectionTestUtils.invokeMethod(threadService, "calculateMaxTokens", "가".repeat(1000)));
    }
}
