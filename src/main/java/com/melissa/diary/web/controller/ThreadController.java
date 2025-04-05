package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.ThreadService;
import com.melissa.diary.web.dto.ThreadRequestDTO;
import com.melissa.diary.web.dto.ThreadResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.security.Principal;

@RestController
@Tag(name = "Thread&ChatsAPI", description = "Thread&Chats 관련 API")
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
@Slf4j
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

        ThreadResponseDTO.ThreadResponse response = threadService. createThread(userId, aiProfileId, year, month, day);

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

    // 해당 날짜(Thread)의 AiProfile을 변경합니다. 이후 채팅을 생성할 때는 해당 스레드의 AiProfile을 불러와 작성합니다.@Operation(description = "Thread의 AiProfile을 변경합니다. 이후 채팅을 생성할 때 해당 프로필을 불러와 작성합니다.")
    @PatchMapping("/ai-profile")
    public ApiResponse<String> updateAiProfile(
            @RequestParam(name = "aiProfileId") Long aiProfileId,
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month,
            @RequestParam(name = "day") int day,
            Principal principal
    ){
        Long userId = Long.parseLong(principal.getName());
        threadService.updateThreadAiProfile(userId, aiProfileId, year, month, day);
        return ApiResponse.onSuccess("AI Profile 업데이트가 완료되었습니다.");
    }

    // 🔹 SSE 기반 AI 응답 스트리밍 API
    @Operation(description = "AI에게 채팅 메시지를 전송하고, SSE로 실시간 응답을 받습니다.")
    @PostMapping(value = "/message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> messageToAi(
            @RequestBody ThreadRequestDTO.AiChatRequest request,
            Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        return threadService.messageToAi(userId, request.getYear(), request.getMonth(), request.getDay(), request.getContent());
    }

    // 해당 날짜(Thread)의 채팅메시지 조회
    @Operation(description = "해당 날짜(Thread)의 채팅 메시지 조회")
    @GetMapping
    public ApiResponse<ThreadResponseDTO.ChatListResponse> getMessages(
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month,
            @RequestParam(name = "day") int day,
            Principal principal
    ){
        Long userId = Long.parseLong(principal.getName());
        ThreadResponseDTO.ChatListResponse response = threadService.getThreadMessages(userId, year, month, day);
        return ApiResponse.onSuccess(response);
    }
}
