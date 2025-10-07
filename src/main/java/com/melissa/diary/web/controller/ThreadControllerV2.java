package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.ThreadServiceV2;
import com.melissa.diary.service.ManualDiaryService;
import com.melissa.diary.web.dto.ThreadRequestDTO;
import com.melissa.diary.web.dto.ThreadResponseDTO;
import com.melissa.diary.web.dto.ManualDiaryRequestDTO;
import com.melissa.diary.web.dto.ThreadSummaryResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import jakarta.validation.Valid;
import java.security.Principal;

@RestController
@Tag(name = "Thread&ChatsAPI-V2", description = "메모리 기반 Thread&Chats API + 수동 Thread 관리")
@RequestMapping("/api/v2/chats")
@RequiredArgsConstructor
public class ThreadControllerV2 {

    private final ThreadServiceV2 threadServiceV2;
    private final ManualDiaryService manualDiaryService;

    @PostMapping(value = "/message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "메모리 기반 스트리밍 채팅", description = "사용자 메모리를 활용한 AI와의 실시간 스트리밍 채팅입니다.")
    public Flux<ServerSentEvent<String>> messageToAi(
            @RequestBody ThreadRequestDTO.AiChatRequest request,
            Principal principal
    ) {
        Long userId = Long.parseLong(principal.getName());
        return threadServiceV2.messageToAi(userId, request.getYear(), request.getMonth(), request.getDay(), request.getContent());
    }

    // 웹 테스트용 Non-SSE 메모리 기반 채팅 API
    @PostMapping("/message-test")
    @Operation(summary = "웹 테스트용 메모리 기반 채팅", 
               description = "정성적 평가를 위한 웹 테스트용 API입니다. SSE 없이 일반 HTTP 응답으로 메모리 기반 AI 채팅을 제공합니다.")
    public ApiResponse<ThreadResponseDTO.ChatResponse> messageToAiTest(
            @RequestBody ThreadRequestDTO.AiChatRequest request,
            Principal principal
    ) {
        Long userId = Long.parseLong(principal.getName());
        ThreadResponseDTO.ChatResponse response = threadServiceV2.messageToAiTest(
                userId, request.getYear(), request.getMonth(), request.getDay(), request.getContent()
        );
        return ApiResponse.onSuccess(response);
    }

    // =============== 수동 Thread 관리 API ===============

    @Operation(description = "수동으로 Thread(일기)를 생성합니다. 같은 날짜에 기존 Thread가 있으면 덮어씁니다.")
    @PostMapping("/manual")
    public ApiResponse<ThreadSummaryResponseDTO.dailySummaryResponseDTO> createManualThread(
            Principal principal,
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month,
            @RequestParam(name = "day") int day,
            @Valid @RequestBody ManualDiaryRequestDTO.ManualDiaryCreateRequest request) {

        Long userId = Long.parseLong(principal.getName());
        var response = manualDiaryService.createManualDiary(userId, year, month, day, request);
        return ApiResponse.onSuccess(response);
    }

    @Operation(description = "기존 Thread를 수정합니다. (채팅 기반, 수동 작성 구분 없이 모두 수정 가능)")
    @PatchMapping("/manual")
    public ApiResponse<ThreadSummaryResponseDTO.dailySummaryResponseDTO> updateManualThread(
            Principal principal,
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month,
            @RequestParam(name = "day") int day,
            @Valid @RequestBody ManualDiaryRequestDTO.ManualDiaryUpdateRequest request) {

        Long userId = Long.parseLong(principal.getName());
        var response = manualDiaryService.updateManualDiary(userId, year, month, day, request);
        return ApiResponse.onSuccess(response);
    }

}

