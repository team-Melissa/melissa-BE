package com.melissa.diary.web.controller;

import com.melissa.diary.service.ThreadServiceV2;
import com.melissa.diary.web.dto.ThreadRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.security.Principal;

@RestController
@Tag(name = "Thread&ChatsAPI-V2", description = "메모리 기반 Thread&Chats API")
@RequestMapping("/api/v2/chats")
@RequiredArgsConstructor
public class ThreadControllerV2 {

    private final ThreadServiceV2 threadServiceV2;

    @PostMapping(value = "/message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "메모리 기반 스트리밍 채팅", description = "사용자 메모리를 활용한 AI와의 실시간 스트리밍 채팅입니다.")
    public Flux<ServerSentEvent<String>> messageToAi(
            @RequestBody ThreadRequestDTO.AiChatRequest request,
            Principal principal
    ) {
        Long userId = Long.parseLong(principal.getName());
        return threadServiceV2.messageToAi(userId, request.getYear(), request.getMonth(), request.getDay(), request.getContent());
    }
}

