package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.converter.UserMemoryConverter;
import com.melissa.diary.domain.UserMemory;
import com.melissa.diary.service.UserMemoryService;
import com.melissa.diary.web.dto.UserMemoryResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@Tag(name = "UserMemoryAPI", description = "사용자 메모리 관리 API")
@RequestMapping("/api/v1/memory")
@RequiredArgsConstructor
public class UserMemoryController {
    
    private final UserMemoryService userMemoryService;
    
    /**
     * 사용자 메모리 조회
     */
    @GetMapping
    @Operation(summary = "사용자 메모리 조회", description = "현재 사용자의 축적된 메모리 내용을 조회합니다.")
    public ApiResponse<UserMemoryResponseDTO.MemoryResponse> getUserMemory(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        
        UserMemory userMemory = userMemoryService.getUserMemoryReadOnly(userId);
        UserMemoryResponseDTO.MemoryResponse response = UserMemoryConverter.toMemoryResponse(userMemory);
        
        return ApiResponse.onSuccess(response);
    }
    
    /**
     * 사용자 메모리 초기화
     */
    @DeleteMapping
    @Operation(summary = "사용자 메모리 초기화", description = "현재 사용자의 축적된 메모리를 모두 삭제합니다.")
    public ApiResponse<UserMemoryResponseDTO.MemoryResetResponse> resetUserMemory(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        
        userMemoryService.resetUserMemory(userId);
        UserMemoryResponseDTO.MemoryResetResponse response = UserMemoryConverter.toMemoryResetResponse();
        
        return ApiResponse.onSuccess(response);
    }
}
