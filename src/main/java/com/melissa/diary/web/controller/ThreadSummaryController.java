package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.ThreadSummaryService;
import com.melissa.diary.web.dto.ThreadSummaryResponseDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@Tag(name = "ThreadSummaryAPI", description = "일일요약 수동생성 API")
@RequestMapping("/api/v1/summary")
@RequiredArgsConstructor
public class ThreadSummaryController {

    private final ThreadSummaryService threadSummaryService;

    // 요청한 유저에 대한 요약 생성 -> 채팅방 나갈 때로 변경
    @PostMapping
    public ApiResponse<ThreadSummaryResponseDTO.dailySummaryResponseDTO> generateSummaryForUser(
            Principal principal,
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month,
            @RequestParam(name = "day") int day
    ) {
        Long userId = Long.parseLong(principal.getName());

        ThreadSummaryResponseDTO.dailySummaryResponseDTO response = threadSummaryService.generateImmediateSummary(userId, year, month, day);
        return ApiResponse.onSuccess(response );
    }

}