package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.ThreadSummaryService;
import com.melissa.diary.service.ThreadSummaryServiceV2;
import com.melissa.diary.web.dto.ThreadSummaryResponseDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;

@RestController
@RequestMapping("/api/v2/summary")
@RequiredArgsConstructor
@Tag(name = "ThreadSummaryAPI-V2", description = "일일요약 수동생성 API V2(이미지 로직 비동기 전환)")
public class ThreadSummaryV2Controller {

    private final ThreadSummaryServiceV2 threadSummaryServiceV2;

    @PostMapping
    public ApiResponse<ThreadSummaryResponseDTO.dailySummaryResponseDTO> createSummaryV2(
            Principal principal,
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month,
            @RequestParam(name = "day") int day) {

        Long userId = Long.parseLong(principal.getName());
        var dto = threadSummaryServiceV2.generateImmediateSummaryV2(userId, year, month, day);
        return ApiResponse.onSuccess(dto);
    }
}
