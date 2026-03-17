package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.ChatLogService;
import com.melissa.diary.service.IdempotencyService;
import com.melissa.diary.service.ThreadService;
import com.melissa.diary.web.dto.ChatLogRequestDTO;
import com.melissa.diary.web.dto.ChatLogResponseDTO;
import com.melissa.diary.web.dto.ThreadRequestDTO;
import com.melissa.diary.web.dto.ThreadResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.security.Principal;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@Tag(name = "Thread&ChatsAPI", description = "Thread&Chats 관리 API")
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ThreadController {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final String V1_CHAT_MESSAGE_ENDPOINT = "POST:/api/v1/chats/message";
    private static final String V2_CHAT_MESSAGE_ENDPOINT = "POST:/api/v2/chats/message";
    private static final String V2_CHAT_MESSAGE_TEST_ENDPOINT = "POST:/api/v2/chats/message-test";

    private final ThreadService threadService;
    private final ChatLogService chatLogService;
    private final IdempotencyService idempotencyService;

    @Operation(summary = "채팅 스레드 생성",
            description = "해당 날짜의 채팅 스레드를 생성합니다. 기존 스레드가 있으면 같은 threadId를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "THREAD4001: 해당 날짜의 스레드가 이미 존재"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 사용자를 찾을 수 없음 / PROFILE4002: AI 프로필을 찾을 수 없음")
    })
    @PostMapping("/v1/chats")
    public ApiResponse<ThreadResponseDTO.ThreadResponse> createThread(
            @Parameter(description = "AI 프로필 ID", required = true, example = "1")
            @RequestParam(name = "aiProfileId") Long aiProfileId,
            @Parameter(description = "연도", required = true, example = "2025")
            @RequestParam(name = "year") int year,
            @Parameter(description = "월", required = true, example = "1")
            @RequestParam(name = "month") int month,
            @Parameter(description = "일", required = true, example = "15")
            @RequestParam(name = "day") int day,
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        ThreadResponseDTO.ThreadResponse response = threadService.createThread(userId, aiProfileId, year, month, day);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "채팅 스레드 삭제",
            description = "해당 날짜의 스레드와 연결된 채팅 로그를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CALENDAR4002: 스레드에 접근할 권한이 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 사용자를 찾을 수 없음 / CALENDAR4001: 스레드를 찾을 수 없음")
    })
    @DeleteMapping("/v1/chats")
    public ApiResponse<ThreadResponseDTO.ThreadResponse> deleteThread(
            @Parameter(description = "AI 프로필 ID", required = true, example = "1")
            @RequestParam(name = "aiProfileId") Long aiProfileId,
            @Parameter(description = "연도", required = true, example = "2025")
            @RequestParam(name = "year") int year,
            @Parameter(description = "월", required = true, example = "1")
            @RequestParam(name = "month") int month,
            @Parameter(description = "일", required = true, example = "15")
            @RequestParam(name = "day") int day,
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        ThreadResponseDTO.ThreadResponse response = threadService.deleteTread(userId, aiProfileId, year, month, day);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "AI 채팅 메시지 전송 (SSE)",
            description = "AI에게 채팅 메시지를 전송하고 SSE로 응답을 받습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CALENDAR4002: 스레드에 접근할 권한이 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 사용자를 찾을 수 없음 / CALENDAR4001: 스레드를 찾을 수 없음 / PROFILE4002: AI 프로필을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "QUOTA4001: 일일 사용량 초과")
    })
    @PostMapping(value = "/v1/chats/message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> messageToAi(
            @Valid @RequestBody ThreadRequestDTO.AiChatRequest request,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
            Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        if (isBlank(idempotencyKey)) {
            return threadService.messageToAi(
                    userId, request.getAiProfileId(), request.getYear(), request.getMonth(), request.getDay(), request.getContent()
            );
        }

        var claim = idempotencyService.claim(userId, V1_CHAT_MESSAGE_ENDPOINT, idempotencyKey, request);
        if (claim.getAction() == IdempotencyService.ClaimAction.RETURN_CACHED) {
            return buildCachedAiStream(claim.getRecord().getResponseBody());
        }

        return wrapIdempotentStream(
                claim.getRecord().getId(),
                threadService.messageToAi(
                        userId, request.getAiProfileId(), request.getYear(), request.getMonth(), request.getDay(), request.getContent()
                )
        );
    }

    @Operation(summary = "채팅 메시지 조회",
            description = "해당 날짜 스레드의 채팅 메시지를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CALENDAR4002: 스레드에 접근할 권한이 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 사용자를 찾을 수 없음 / CALENDAR4001: 스레드를 찾을 수 없음")
    })
    @GetMapping("/v1/chats")
    public ApiResponse<ThreadResponseDTO.ChatListResponse> getMessages(
            @Parameter(description = "AI 프로필 ID", required = true, example = "1")
            @RequestParam(name = "aiProfileId") Long aiProfileId,
            @Parameter(description = "연도", required = true, example = "2025")
            @RequestParam(name = "year") int year,
            @Parameter(description = "월", required = true, example = "1")
            @RequestParam(name = "month") int month,
            @Parameter(description = "일", required = true, example = "15")
            @RequestParam(name = "day") int day,
            Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        ThreadResponseDTO.ChatListResponse response = threadService.getThreadMessages(userId, aiProfileId, year, month, day);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "채팅 메시지 수정",
            description = "사용자 메시지만 수정할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "CHAT4004: AI 메시지는 수정할 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CHAT4003: 채팅 메시지에 접근할 권한이 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "CHAT4002: 채팅 메시지를 찾을 수 없음")
    })
    @PatchMapping("/v1/chats/{chatLogId}")
    public ApiResponse<ChatLogResponseDTO.ChatLogResponse> updateChatLog(
            @Parameter(description = "채팅 로그 ID", required = true, example = "1")
            @PathVariable Long chatLogId,
            @Valid @RequestBody ChatLogRequestDTO.ChatLogUpdateRequest request,
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        ChatLogResponseDTO.ChatLogResponse response = chatLogService.updateChatLog(userId, chatLogId, request);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "채팅 메시지 삭제",
            description = "사용자 또는 AI가 작성한 채팅 메시지를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CHAT4003: 채팅 메시지에 접근할 권한이 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "CHAT4002: 채팅 메시지를 찾을 수 없음")
    })
    @DeleteMapping("/v1/chats/{chatLogId}")
    public ApiResponse<ChatLogResponseDTO.ChatLogDeleteResponse> deleteChatLog(
            @Parameter(description = "채팅 로그 ID", required = true, example = "1")
            @PathVariable Long chatLogId,
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        ChatLogResponseDTO.ChatLogDeleteResponse response = chatLogService.deleteChatLog(userId, chatLogId);
        return ApiResponse.onSuccess(response);
    }

    @PostMapping(value = "/v2/chats/message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "메모리 기반 SSE 채팅",
            description = "사용자 메모리를 포함한 AI 채팅입니다.")
    public Flux<ServerSentEvent<String>> messageToAiV2(
            @Valid @RequestBody ThreadRequestDTO.AiChatRequestV2 request,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
            Principal principal
    ) {
        Long userId = Long.parseLong(principal.getName());

        if (isBlank(idempotencyKey)) {
            return threadService.messageToAiV2(
                    userId, request.getAiProfileId(), request.getYear(), request.getMonth(), request.getDay(), request.getContent()
            );
        }

        var claim = idempotencyService.claim(userId, V2_CHAT_MESSAGE_ENDPOINT, idempotencyKey, request);
        if (claim.getAction() == IdempotencyService.ClaimAction.RETURN_CACHED) {
            return buildCachedAiStream(claim.getRecord().getResponseBody());
        }

        return wrapIdempotentStream(
                claim.getRecord().getId(),
                threadService.messageToAiV2(
                        userId, request.getAiProfileId(), request.getYear(), request.getMonth(), request.getDay(), request.getContent()
                )
        );
    }

    @PostMapping("/v2/chats/message-test")
    @Operation(summary = "메모리 기반 테스트 채팅",
            description = "SSE 없이 일반 HTTP 응답으로 메모리 기반 AI 채팅을 수행합니다.")
    public ApiResponse<ThreadResponseDTO.ChatResponse> messageToAiTest(
            @Valid @RequestBody ThreadRequestDTO.AiChatRequestV2 request,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
            Principal principal
    ) {
        Long userId = Long.parseLong(principal.getName());

        if (isBlank(idempotencyKey)) {
            ThreadResponseDTO.ChatResponse response = threadService.messageToAiTest(
                    userId, request.getAiProfileId(), request.getYear(), request.getMonth(), request.getDay(), request.getContent()
            );
            return ApiResponse.onSuccess(response);
        }

        var claim = idempotencyService.claim(userId, V2_CHAT_MESSAGE_TEST_ENDPOINT, idempotencyKey, request);
        if (claim.getAction() == IdempotencyService.ClaimAction.RETURN_CACHED) {
            ThreadResponseDTO.ChatResponse cached = idempotencyService.deserialize(
                    claim.getRecord().getResponseBody(),
                    ThreadResponseDTO.ChatResponse.class
            );
            return ApiResponse.onSuccess(cached);
        }

        try {
            ThreadResponseDTO.ChatResponse response = threadService.messageToAiTest(
                    userId, request.getAiProfileId(), request.getYear(), request.getMonth(), request.getDay(), request.getContent()
            );
            idempotencyService.markSucceeded(claim.getRecord().getId(), idempotencyService.serialize(response));
            return ApiResponse.onSuccess(response);
        } catch (RuntimeException e) {
            idempotencyService.markFailedRetryable(claim.getRecord().getId(), e);
            throw e;
        }
    }

    private Flux<ServerSentEvent<String>> wrapIdempotentStream(Long recordId, Flux<ServerSentEvent<String>> source) {
        StringBuilder answer = new StringBuilder();
        AtomicBoolean failed = new AtomicBoolean(false);

        return source
                .doOnNext(event -> {
                    if ("aiMessage".equals(event.event()) && event.data() != null) {
                        answer.append(event.data());
                    }
                    if ("error".equals(event.event())) {
                        failed.set(true);
                    }
                })
                .doOnError(error -> {
                    failed.set(true);
                    idempotencyService.markFailedRetryable(recordId, error);
                })
                .doOnComplete(() -> {
                    if (failed.get()) {
                        idempotencyService.markFailedRetryable(recordId, new IllegalStateException("stream failed"));
                        return;
                    }
                    idempotencyService.markSucceeded(recordId, answer.toString().trim());
                });
    }

    private Flux<ServerSentEvent<String>> buildCachedAiStream(String cachedMessage) {
        return Flux.just(
                ServerSentEvent.<String>builder().event("aiMessage").data(cachedMessage).build(),
                ServerSentEvent.<String>builder().event("finish").data("finish").build()
        ).delayElements(Duration.ofMillis(10));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
