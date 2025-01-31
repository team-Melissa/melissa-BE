package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.ThreadService;
import com.melissa.diary.web.dto.ThreadResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@Tag(name = "ThreadAPI", description = "Thread 관련 API")
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ThreadController {

    private final ThreadService threadService;

    @Operation(description = "해당 날짜의 채팅 스레드를 생성합니다. 기존에 존재 시, 같은 threadId 리턴")
    @PostMapping
    public ApiResponse<ThreadResponseDTO.ThreadResponse> createThread(
            @RequestParam(name = "aiProfileId") Long aiProfileId,
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month,
            @RequestParam(name = "day") int day,
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());

        ThreadResponseDTO.ThreadResponse response = threadService.creatTread(userId, aiProfileId, year, month, day);

        return ApiResponse.onSuccess(response);
    }

    @Operation(description = "해당 날짜의 스레드를 삭제합니다.")
    @DeleteMapping
    public ApiResponse<ThreadResponseDTO.ThreadResponse> deleteThread(
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month,
            @RequestParam(name = "day") int day,
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());

        ThreadResponseDTO.ThreadResponse response = threadService.deleteTread(userId, year, month, day);

        return ApiResponse.onSuccess(response);
    }
}
