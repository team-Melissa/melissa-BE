package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.ThreadSummaryService;
import com.melissa.diary.web.dto.ThreadSummaryResponseDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
            Principal principal
    ) {
        Long userId = Long.parseLong(principal.getName());

        ThreadSummaryResponseDTO.dailySummaryResponseDTO response = threadSummaryService.generateImmediateSummary(userId);
        return ApiResponse.onSuccess(response );
    }

}