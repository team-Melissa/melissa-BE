package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.DailyChatLog;
import com.melissa.diary.domain.enums.Role;
import com.melissa.diary.repository.DailyChatLogRepository;
import com.melissa.diary.web.dto.ChatLogRequestDTO;
import com.melissa.diary.web.dto.ChatLogResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatLogService {

    private final DailyChatLogRepository dailyChatLogRepository;

    /**
     * [v1.3.0] 채팅 메시지 삭제
     * - 본인 Thread의 메시지만 삭제 가능
     * - 사용자/AI 메시지 모두 삭제 가능
     */
    @Transactional
    public ChatLogResponseDTO.ChatLogDeleteResponse deleteChatLog(Long userId, Long chatLogId) {
        // 채팅 로그 조회
        DailyChatLog chatLog = dailyChatLogRepository.findById(chatLogId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.CHAT_LOG_NOT_FOUND));

        // 권한 검증: 본인의 Thread에 속한 메시지인지 확인
        if (chatLog.getThread() == null || !chatLog.getThread().getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.CHAT_LOG_FORBIDDEN);
        }

        // 삭제 실행
        dailyChatLogRepository.delete(chatLog);
        
        log.info("[ChatLog] 채팅 메시지 삭제 완료. userId={}, chatLogId={}, role={}", 
                userId, chatLogId, chatLog.getRole());

        return ChatLogResponseDTO.ChatLogDeleteResponse.builder()
                .chatId(chatLogId)
                .message("채팅 메시지가 삭제되었습니다.")
                .build();
    }

    /**
     * 채팅 메시지 수정
     * - 본인 메시지만 수정 가능
     * - AI 메시지는 수정 불가
     */
    @Transactional
    public ChatLogResponseDTO.ChatLogResponse updateChatLog(
            Long userId, Long chatLogId, ChatLogRequestDTO.ChatLogUpdateRequest request) {
        
        // 채팅 로그 조회
        DailyChatLog chatLog = dailyChatLogRepository.findById(chatLogId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.CHAT_LOG_NOT_FOUND));

        // AI 메시지 수정 차단
        if (chatLog.getRole() == Role.AI) {
            throw new ErrorHandler(ErrorStatus.CHAT_LOG_AI_MESSAGE);
        }

        // 권한 검증: 본인의 Thread에 속한 메시지인지 확인
        if (chatLog.getThread() == null || !chatLog.getThread().getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.CHAT_LOG_FORBIDDEN);
        }

        // 내용 수정
        chatLog.setContent(request.getContent());
        dailyChatLogRepository.save(chatLog);
        
        log.info("[ChatLog] 채팅 메시지 수정 완료. userId={}, chatLogId={}", userId, chatLogId);

        return ChatLogResponseDTO.ChatLogResponse.builder()
                .chatId(chatLog.getId())
                .role(chatLog.getRole().name())
                .content(chatLog.getContent())
                .createdAt(chatLog.getCreatedAt())
                .build();
    }
}

