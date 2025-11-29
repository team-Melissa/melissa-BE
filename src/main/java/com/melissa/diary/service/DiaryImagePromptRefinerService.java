package com.melissa.diary.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Diary 이미지 프롬프트 정제 서비스
 * LLM을 사용해 이미지 생성 프롬프트를 DALL-E에 최적화된 형태로 변환
 */
@Slf4j
@Service
public class DiaryImagePromptRefinerService {
    
    private final ChatClient diaryPromptRefinerClient;
    
    public DiaryImagePromptRefinerService(@Qualifier("diaryPromptRefinerClient") ChatClient diaryPromptRefinerClient) {
        this.diaryPromptRefinerClient = diaryPromptRefinerClient;
    }
    
    /**
     * 1차 프롬프트를 LLM으로 정제하여 DALL-E용 프롬프트 생성
     * AiConfig의 diaryPromptRefinerClient Bean 설정 사용
     */
    public String refine(String rawPrompt) {
        try {
            String response = diaryPromptRefinerClient.prompt()
                    .user(rawPrompt)
                    .call()
                    .content();
            
            log.debug("[DiaryImagePrompt] 정제 완료. raw={}, refined={}", rawPrompt, response);
            return response;
            
        } catch (Exception e) {
            log.warn("[DiaryImagePrompt] LLM 정제 실패, raw 프롬프트 사용. error={}", e.getMessage());
            return rawPrompt;
        }
    }
}

