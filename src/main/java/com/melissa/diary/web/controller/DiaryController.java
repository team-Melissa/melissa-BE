package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.DiaryService;
import com.melissa.diary.web.dto.DiaryRequestDTO;
import com.melissa.diary.web.dto.DiaryResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    
    @Operation(summary = "채팅 기반 일기 생성", 
               description = "[v1.3.0] Thread의 채팅 로그를 LLM으로 요약하여 일기를 자동 생성합니다. 제목, 내용, 해시태그가 자동 생성됩니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "DIARY4004: 하루 최대 3개 일기 제한 초과"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 사용자를 찾을 수 없음 / CALENDAR4001: Thread를 찾을 수 없음 / CHAT4001: 채팅 로그 부족 (2개 이하)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "QUOTA4001: 일일 사용량 초과"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "CALENDAR5001: LLM 응답 파싱 실패")
    })
    @PostMapping("/from-chat")
    public ApiResponse<DiaryResponseDTO.DiaryResponse> createChatDiary(
            @Valid @RequestBody DiaryRequestDTO.ChatDiaryCreateRequest request,
            Principal principal) {
        
        Long userId = Long.parseLong(principal.getName());
        DiaryResponseDTO.DiaryResponse response = diaryService.createChatDiarySeparated(userId, request);
        
        return ApiResponse.onSuccess(response);
    }
    
    @Operation(summary = "수동 일기 작성", 
               description = "[v1.3.0] 사용자가 직접 작성한 일기를 저장합니다. 해시태그는 LLM이 자동 생성하며, 이미지는 DALL-E로 비동기 생성됩니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "CALENDAR4003: 유효하지 않은 날짜 / DIARY4004: 하루 최대 3개 일기 제한 초과"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 사용자를 찾을 수 없음 / PROFILE4002: AI 프로필을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "QUOTA4001: 일일 사용량 초과"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "CALENDAR5001: LLM 응답 파싱 실패")
    })
    @PostMapping("/manual")
    public ApiResponse<DiaryResponseDTO.DiaryResponse> createManualDiary(
            @Valid @RequestBody DiaryRequestDTO.ManualDiaryCreateRequest request,
            Principal principal) {
        
        Long userId = Long.parseLong(principal.getName());
        DiaryResponseDTO.DiaryResponse response = diaryService.createManualDiarySeparated(userId, request);
        
        return ApiResponse.onSuccess(response);
    }
    
    @Operation(summary = "일기 수정", 
               description = "[v1.3.0] 일기를 수정합니다. null이 아닌 필드만 업데이트됩니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "DIARY4003: 이미 삭제된 일기"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "DIARY4002: 일기에 접근할 권한이 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 사용자를 찾을 수 없음 / DIARY4001: 일기를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "QUOTA4001: 일일 사용량 초과 (이미지 재생성 시)")
    })
    @PutMapping("/{diaryId}")
    public ApiResponse<DiaryResponseDTO.DiaryResponse> updateDiary(
            @Parameter(description = "일기 ID", required = true, example = "1")
            @PathVariable Long diaryId,
            @Valid @RequestBody DiaryRequestDTO.DiaryUpdateRequest request,
            Principal principal) {
        
        Long userId = Long.parseLong(principal.getName());
        DiaryResponseDTO.DiaryResponse response = diaryService.updateDiarySeparated(userId, diaryId, request);
        
        return ApiResponse.onSuccess(response);
    }
    
    @Operation(summary = "일기 삭제", 
               description = "[v1.3.0] 일기를 삭제합니다. (소프트 삭제 - isActive = false)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "DIARY4003: 이미 삭제된 일기"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "DIARY4002: 일기에 접근할 권한이 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 사용자를 찾을 수 없음 / DIARY4001: 일기를 찾을 수 없음")
    })
    @DeleteMapping("/{diaryId}")
    public ApiResponse<DiaryResponseDTO.DiaryDeleteResponse> deleteDiary(
            @Parameter(description = "일기 ID", required = true, example = "1")
            @PathVariable Long diaryId,
            Principal principal) {
        
        Long userId = Long.parseLong(principal.getName());
        DiaryResponseDTO.DiaryDeleteResponse response = diaryService.deleteDiary(userId, diaryId);
        
        return ApiResponse.onSuccess(response);
    }
}
