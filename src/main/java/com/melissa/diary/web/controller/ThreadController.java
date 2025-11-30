package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.ThreadService;
import com.melissa.diary.web.dto.ThreadRequestDTO;
import com.melissa.diary.web.dto.ThreadResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
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

    @Operation(summary = "채팅 스레드 생성",
               description = "해당 날짜의 채팅 스레드를 생성합니다. 기존에 존재 시, 같은 threadId 리턴")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "THREAD4001: 해당 날짜의 스레드가 이미 존재"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 사용자를 찾을 수 없음 / PROFILE4002: AI 프로필을 찾을 수 없음")
    })
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

    @Operation(summary = "채팅 스레드 삭제",
               description = "해당 날짜의 스레드와 연결된 채팅 로그, 일기를 모두 삭제합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CALENDAR4002: 스레드에 접근할 권한이 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 사용자를 찾을 수 없음 / CALENDAR4001: 스레드를 찾을 수 없음")
    })
    @DeleteMapping
    public ApiResponse<ThreadResponseDTO.ThreadResponse> deleteThread(
            @RequestParam(name = "aiProfileId") Long aiProfileId,
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month,
            @RequestParam(name = "day") int day,
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());

        ThreadResponseDTO.ThreadResponse response = threadService.deleteTread(userId, aiProfileId, year, month, day);

        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "AI 채팅 메시지 전송 (SSE 스트리밍)",
               description = "AI에게 채팅 메시지를 전송하고, SSE로 실시간 응답을 받습니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CALENDAR4002: 스레드에 접근할 권한이 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 사용자를 찾을 수 없음 / CALENDAR4001: 스레드를 찾을 수 없음 / PROFILE4002: AI 프로필을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "QUOTA4001: 일일 사용량 초과")
    })
    @PostMapping(value = "/message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> messageToAi(
            @jakarta.validation.Valid @RequestBody ThreadRequestDTO.AiChatRequest request,
            Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        return threadService.messageToAi(userId, request.getAiProfileId(), request.getYear(), request.getMonth(), request.getDay(), request.getContent());
    }

    @Operation(summary = "채팅 메시지 조회",
               description = "해당 날짜(Thread)의 채팅 메시지를 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CALENDAR4002: 스레드에 접근할 권한이 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 사용자를 찾을 수 없음 / CALENDAR4001: 스레드를 찾을 수 없음")
    })
    @GetMapping
    public ApiResponse<ThreadResponseDTO.ChatListResponse> getMessages(
            @RequestParam(name = "aiProfileId") Long aiProfileId,
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month,
            @RequestParam(name = "day") int day,
            Principal principal
    ){
        Long userId = Long.parseLong(principal.getName());
        ThreadResponseDTO.ChatListResponse response = threadService.getThreadMessages(userId, aiProfileId, year, month, day);
        return ApiResponse.onSuccess(response);
    }
    
    // =============== v2: UserMemory 기반 API ===============
    
    /**
     * v2: UserMemory 기반 SSE 스트리밍 채팅
     */
    @PostMapping(value = "/v2/message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "메모리 기반 스트리밍 채팅", 
               description = "사용자 장기 기억을 활용한 AI와의 실시간 스트리밍 채팅입니다.")
    public Flux<ServerSentEvent<String>> messageToAiV2(
            @jakarta.validation.Valid @RequestBody ThreadRequestDTO.AiChatRequestV2 request,
            Principal principal
    ) {
        Long userId = Long.parseLong(principal.getName());
        return threadService.messageToAiV2(
                userId, request.getAiProfileId(), request.getYear(), request.getMonth(), request.getDay(), request.getContent()
        );
    }
    
    /**
     * v2: 웹 테스트용 Non-SSE 메모리 기반 채팅
     * 정성적 평가를 위한 동기 API
     */
    @PostMapping("/v2/message-test")
    @Operation(summary = "웹 테스트용 메모리 기반 채팅", 
               description = "정성적 평가를 위한 웹 테스트용 API입니다. SSE 없이 일반 HTTP 응답으로 메모리 기반 AI 채팅을 제공합니다.")
    public ApiResponse<ThreadResponseDTO.ChatResponse> messageToAiTest(
            @jakarta.validation.Valid @RequestBody ThreadRequestDTO.AiChatRequestV2 request,
            Principal principal
    ) {
        Long userId = Long.parseLong(principal.getName());
        ThreadResponseDTO.ChatResponse response = threadService.messageToAiTest(
                userId, request.getAiProfileId(), request.getYear(), request.getMonth(), request.getDay(), request.getContent()
        );
        return ApiResponse.onSuccess(response);
    }
}
