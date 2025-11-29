package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.CalenderService;
import com.melissa.diary.web.dto.CalenderResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * [v1.3.0] Diary 기반 달력 API
 */
@RestController
@Tag(name = "CalenderAPI", description = "Calender 관련 API")
@RequestMapping("/api/v1/calender")
@RequiredArgsConstructor
public class CalenderController {

    private final CalenderService calenderService;

    @Operation(description = "[v1.3.0] 특정 날짜의 일기를 상세 조회합니다. (최대 3개)")
    @GetMapping("/day")
    public ApiResponse<CalenderResponseDTO.DailySummaryResponseDTO> getDailySummary(
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month,
            @RequestParam(name = "day") int day,
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        CalenderResponseDTO.DailySummaryResponseDTO response = calenderService.getDailySummary(userId, year, month, day);

        return ApiResponse.onSuccess(response);
    }

    @Operation(description = "[v1.3.0] 해당 월의 모든 날짜별 일기 미리보기를 조회합니다. (날짜별 최대 3개)")
    @GetMapping("/month")
    public ApiResponse<List<CalenderResponseDTO.DailyPreviewResponseDTO>> getCalenderPreview(
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month,
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        List<CalenderResponseDTO.DailyPreviewResponseDTO> response = calenderService.getMonthlySummary(userId, year, month);

        return ApiResponse.onSuccess(response);
    }

    @Operation(description = "[v1.3.0] 해당 월의 모든 날짜별 일기 상세 정보를 조회합니다. (날짜별 최대 3개)")
    @GetMapping("/month/summary")
    public ApiResponse<List<CalenderResponseDTO.DailySummaryResponseDTO>> getCalenderView(
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month,
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        List<CalenderResponseDTO.DailySummaryResponseDTO> response = calenderService.getMonthlyView(userId, year, month);

        return ApiResponse.onSuccess(response);
    }
}
