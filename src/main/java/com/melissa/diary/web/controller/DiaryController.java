package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.DiaryService;
import com.melissa.diary.web.dto.DiaryRequestDTO;
import com.melissa.diary.web.dto.DiaryResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * [v1.3.0] Diary 기반 일기 관리 API
 */
@RestController
@Tag(name = "Diary API", description = "[v1.3.0] 일기 관리 API")
@RequestMapping("/api/v1/diaries")
@RequiredArgsConstructor
@Slf4j
public class DiaryController {
    
    private final DiaryService diaryService;
    
    @Operation(summary = "수동 일기 작성", 
               description = "[v1.3.0] 사용자가 직접 작성한 일기를 저장합니다. 이미지는 DALL-E로 자동 생성됩니다.")
    @PostMapping("/manual")
    public ApiResponse<DiaryResponseDTO.DiaryResponse> createManualDiary(
            @Valid @RequestBody DiaryRequestDTO.ManualDiaryCreateRequest request,
            Principal principal) {
        
        Long userId = Long.parseLong(principal.getName());
        DiaryResponseDTO.DiaryResponse response = diaryService.createManualDiary(userId, request);
        
        return ApiResponse.onSuccess(response);
    }
    
    @Operation(summary = "일기 수정", 
               description = "[v1.3.0] 일기를 수정합니다. null이 아닌 필드만 업데이트됩니다.")
    @PutMapping("/{diaryId}")
    public ApiResponse<DiaryResponseDTO.DiaryResponse> updateDiary(
            @PathVariable Long diaryId,
            @Valid @RequestBody DiaryRequestDTO.DiaryUpdateRequest request,
            Principal principal) {
        
        Long userId = Long.parseLong(principal.getName());
        DiaryResponseDTO.DiaryResponse response = diaryService.updateDiary(userId, diaryId, request);
        
        return ApiResponse.onSuccess(response);
    }
    
    @Operation(summary = "일기 삭제", 
               description = "[v1.3.0] 일기를 삭제합니다. (소프트 삭제)")
    @DeleteMapping("/{diaryId}")
    public ApiResponse<DiaryResponseDTO.DiaryDeleteResponse> deleteDiary(
            @PathVariable Long diaryId,
            Principal principal) {
        
        Long userId = Long.parseLong(principal.getName());
        DiaryResponseDTO.DiaryDeleteResponse response = diaryService.deleteDiary(userId, diaryId);
        
        return ApiResponse.onSuccess(response);
    }
}

