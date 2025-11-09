package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.ChatLogService;
import com.melissa.diary.web.dto.ChatLogRequestDTO;
import com.melissa.diary.web.dto.ChatLogResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@Tag(name = "ChatLog API", description = "채팅 메시지 관리 API")
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
@Slf4j
public class ChatLogController {

    private final ChatLogService chatLogService;

    @Operation(summary = "채팅 메시지 삭제", description = "사용자가 작성한 채팅 메시지를 삭제합니다. AI 메시지는 삭제할 수 없습니다.")
    @DeleteMapping("/{chatLogId}")
    public ApiResponse<ChatLogResponseDTO.ChatLogDeleteResponse> deleteChatLog(
            @PathVariable Long chatLogId,
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        ChatLogResponseDTO.ChatLogDeleteResponse response = chatLogService.deleteChatLog(userId, chatLogId);

        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "채팅 메시지 수정", description = "사용자가 작성한 채팅 메시지를 수정합니다. AI 메시지는 수정할 수 없습니다.")
    @PatchMapping("/{chatLogId}")
    public ApiResponse<ChatLogResponseDTO.ChatLogResponse> updateChatLog(
            @PathVariable Long chatLogId,
            @Valid @RequestBody ChatLogRequestDTO.ChatLogUpdateRequest request,
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        ChatLogResponseDTO.ChatLogResponse response = chatLogService.updateChatLog(userId, chatLogId, request);

        return ApiResponse.onSuccess(response);
    }
}

