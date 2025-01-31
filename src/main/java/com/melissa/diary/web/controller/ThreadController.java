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
@Tag(name = "Thread&ChatsAPI", description = "Thread&Chats 관련 API")
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

    // TODO 현재는 AI 대답이 아닌 목업데이터로 전송 및 추가
    @Operation(description = "AI에게 채팅메시지를 전송합니다. (Mock)")
    @PostMapping("/message")
    public ApiResponse<ThreadResponseDTO.ChatResponse> messageToAi(
            @RequestParam(name = "content") String userMessage,
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month,
            @RequestParam(name = "day") int day,
            Principal principal
    ){
        Long userId = Long.parseLong(principal.getName());
        ThreadResponseDTO.ChatResponse aiResponse = threadService.sendMockMessageToAi(userId, year, month, day, userMessage);
        return ApiResponse.onSuccess(aiResponse);
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
