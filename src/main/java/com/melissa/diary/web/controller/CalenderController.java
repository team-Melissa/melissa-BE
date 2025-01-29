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

@RestController
@Tag(name = "CalenderAPI", description = "Calender 관련 API")
@RequestMapping("/api/v1/calender")
@RequiredArgsConstructor
public class CalenderController {

    private final CalenderService calenderService;

    @Operation(description = "특정 날짜의 요약과 사진을 상세 조회합니다.")
    @GetMapping("/day")
    public ApiResponse<CalenderResponseDTO.dailySummaryResponseDTO> getDailySummary(
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month,
            @RequestParam(name = "day") int day,
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        CalenderResponseDTO.dailySummaryResponseDTO response = calenderService.getDailySummary(userId, year, month, day);

        return ApiResponse.onSuccess(response);
    }

    @Operation(description = "해당 월의 모든 날짜의 해시 태그와 이미지를 조회합니다.")
    @GetMapping("/month")
    public ApiResponse<List<CalenderResponseDTO.dailyResponseDTO>> getCalenderPreview(
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month,
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        List<CalenderResponseDTO.dailyResponseDTO> response = calenderService.getMonthlySummary(userId, year, month);

        return ApiResponse.onSuccess(response);
    }
}
