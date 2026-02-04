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

    @Operation(summary = "피드 전용 최신순 조회 (커서 기반 무한 페이징)",
               description = """
                   [v1.3.0+] 피드 전용 최신순 정렬(id DESC)을 보장하며, 커서 기반 무한 페이징을 제공합니다.
                   - limit은 날짜 묶음 개수입니다 (일기 개수가 아님). 기본 20개, 최대 50개.
                   - 첫 요청(첫 페이지)은 cursorDiaryId 없이 호출합니다.
                   - 다음 페이지부터는 직전 응답의 pageInfo.nextCursor.cursorDiaryId 값을 그대로 재전송합니다.
                   - 마지막 페이지에서는 pageInfo.hasNext=false 이며 pageInfo.nextCursor=null 입니다.
                   - 각 날짜 묶음에는 최대 3개의 일기가 포함됩니다.
                   """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "CALENDAR4005: 유효하지 않은 limit"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 사용자를 찾을 수 없음")
    })
    @GetMapping("/feed")
    public ApiResponse<CalendarResponseDTO.FeedResponseDTO> getFeed(
            @Parameter(description = "조회할 날짜 묶음 개수 (기본 20, 최대 50)", example = "20")
            @RequestParam(name = "limit", required = false) Integer limit,
            @Parameter(description = """
                    커서 diaryId (단일 필드 커서)
                    - 첫 요청은 미전송(null) 가능
                    - 다음 페이지부터는 직전 응답의 pageInfo.nextCursor.cursorDiaryId 값을 그대로 재전송
                    - 마지막 페이지는 pageInfo.nextCursor=null 이므로 커서를 다시 미전송
                    """, example = "401")
            @RequestParam(name = "cursorDiaryId", required = false) Long cursorDiaryId,
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        CalendarResponseDTO.FeedResponseDTO response = calendarService.getFeed(userId, limit, cursorDiaryId);

        return ApiResponse.onSuccess(response);
    }
}
