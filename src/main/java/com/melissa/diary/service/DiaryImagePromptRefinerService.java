package com.melissa.diary.service;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class DiaryImagePromptRefinerService {
    
    @Qualifier("summaryClient")
    private final ChatClient chatClient;
    
    /**
     * 1차 프롬프트를 LLM으로 정제하여 DALL-E용 프롬프트 생성
     */
    public String refine(String rawPrompt) {
        try {
            String systemPrompt = """
                    당신은 이미지 생성 프롬프트 전문가입니다.
                    사용자의 일기 내용을 바탕으로 DALL-E에 최적화된 이미지 생성 프롬프트를 작성해주세요.
                    
                    요구사항:
                    - 영어로 작성
                    - 구체적이고 시각적인 표현 사용
                    - 감성적이고 따뜻한 분위기
                    - 최대 100단어 이내
                    - 일기의 핵심 감정과 분위기를 포착
                    
                    예시:
                    입력: "제목: 행복한 하루, 내용: 친구들과 카페에서 즐거운 시간, 기분: HAPPY"
                    출력: "A warm and cozy cafe scene with friends laughing together, soft afternoon sunlight streaming through windows, pastel colors, peaceful and joyful atmosphere, artistic illustration style"
                    """;
            
            String response = chatClient.prompt()
                    .system(systemPrompt)
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

