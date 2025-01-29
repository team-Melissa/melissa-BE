package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.ThreadSummaryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "SummaryAPI", description = "테스트를 위한 트리거용 API")
@RequestMapping("/api/v1/summary")
@RequiredArgsConstructor
public class ThreadSummaryController {

    private final ThreadSummaryService threadSummaryService;

    /**
     * 모든 사용자에 대한 요약 생성 (스케줄러 강제 트리거)
     */
    @PostMapping("/all")
    public ApiResponse<String> generateSummaryForAllUsers() {
        threadSummaryService.generateDailySummaryForAllUsers();
        return ApiResponse.onSuccess("모든 사용자 요약을 강제로 생성 완료");
    }

}