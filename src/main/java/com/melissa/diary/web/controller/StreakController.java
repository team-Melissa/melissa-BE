package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.StreakService;
import com.melissa.diary.web.dto.StreakResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * [v1.3.0+] 스트릭 API
 */
@RestController
@Tag(name = "StreakAPI", description = "Streak 관련 API")
@RequestMapping("/api/v1/streak")
@RequiredArgsConstructor
public class StreakController {

    private final StreakService streakService;

    @Operation(
            summary = "현재 스트릭 조회",
            description = """
                [v1.3.0+] KST 기준 연속 작성 일수를 반환합니다.
                - 쿼리 파라미터 없음 (서버가 KST 기준 '오늘'로 계산)
                - 오늘 미작성이어도 마지막 작성일이 '어제'라면 streakDays는 유지됩니다.
                - 마지막 작성일이 '어제'보다 이전이면 streakDays=0 입니다. (이미 끊김)
                - 하루에 여러 개 작성해도 그 날은 +1로만 계산
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 사용자를 찾을 수 없음")
    })
    @GetMapping
    public ApiResponse<StreakResponseDTO.CurrentStreakResponse> getCurrentStreak(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        int streakDays = streakService.getCurrentStreakDays(userId);
        return ApiResponse.onSuccess(
                StreakResponseDTO.CurrentStreakResponse.builder()
                        .streakDays(streakDays)
                        .build()
        );
    }
}


