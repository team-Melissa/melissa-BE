package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.DiaryService;
import com.melissa.diary.web.dto.DiaryResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * [v1.3.0] 일기 관리 API
 */
@RestController
@Tag(name = "Diary API", description = "일기 관리 API")
@RequestMapping("/api/v1/diaries")
@RequiredArgsConstructor
@Slf4j
public class DiaryController {
    
    private final DiaryService diaryService;
    
    @Operation(summary = "일기 삭제", description = "[v1.3.0] 일기를 삭제합니다. (소프트 삭제)")
    @DeleteMapping("/{diaryId}")
    public ApiResponse<DiaryResponseDTO.DiaryDeleteResponse> deleteDiary(
            @PathVariable Long diaryId,
            Principal principal) {
        
        Long userId = Long.parseLong(principal.getName());
        DiaryResponseDTO.DiaryDeleteResponse response = diaryService.deleteDiary(userId, diaryId);
        
        return ApiResponse.onSuccess(response);
    }
}

