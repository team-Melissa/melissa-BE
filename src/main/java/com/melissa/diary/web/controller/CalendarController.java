package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.CalendarService;
import com.melissa.diary.web.dto.CalendarResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * [v1.3.0] Diary 기반 달력 API
 */
@RestController
@Tag(name = "CalendarAPI", description = "Calendar 관련 API")
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @Operation(summary = "특정 날짜 일기 조회",
               description = "[v1.3.0] 특정 날짜의 일기를 상세 조회합니다. (최대 3개)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "CALENDAR4003: 유효하지 않은 날짜"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 사용자를 찾을 수 없음")
    })
    @GetMapping("/day")
    public ApiResponse<CalendarResponseDTO.DailySummaryResponseDTO> getDailySummary(
            @Parameter(description = "년도", required = true, example = "2025")
            @RequestParam(name = "year") int year,
            @Parameter(description = "월", required = true, example = "1")
            @RequestParam(name = "month") int month,
            @Parameter(description = "일", required = true, example = "15")
            @RequestParam(name = "day") int day,
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        CalendarResponseDTO.DailySummaryResponseDTO response = calendarService.getDailySummary(userId, year, month, day);

        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "월간 일기 미리보기",
               description = "[v1.3.0] 해당 월의 모든 날짜별 일기 미리보기를 조회합니다. (날짜별 최대 3개, 해시태그 + 이미지만)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "CALENDAR4003: 유효하지 않은 날짜"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 사용자를 찾을 수 없음")
    })
    @GetMapping("/month")
    public ApiResponse<List<CalendarResponseDTO.DailyPreviewResponseDTO>> getCalendarPreview(
            @Parameter(description = "년도", required = true, example = "2025")
            @RequestParam(name = "year") int year,
            @Parameter(description = "월", required = true, example = "1")
            @RequestParam(name = "month") int month,
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        List<CalendarResponseDTO.DailyPreviewResponseDTO> response = calendarService.getMonthlySummary(userId, year, month);

        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "월간 일기 전체 조회",
               description = "[v1.3.0] 해당 월의 모든 날짜별 일기 상세 정보를 조회합니다. (날짜별 최대 3개)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "CALENDAR4003: 유효하지 않은 날짜"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 사용자를 찾을 수 없음")
    })
    @GetMapping("/month/summary")
    public ApiResponse<List<CalendarResponseDTO.DailySummaryResponseDTO>> getCalendarView(
            @Parameter(description = "년도", required = true, example = "2025")
            @RequestParam(name = "year") int year,
            @Parameter(description = "월", required = true, example = "1")
            @RequestParam(name = "month") int month,
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        List<CalendarResponseDTO.DailySummaryResponseDTO> response = calendarService.getMonthlyView(userId, year, month);

        return ApiResponse.onSuccess(response);
    }
}
