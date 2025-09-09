package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.Thread;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.UserMemory;
import com.melissa.diary.repository.UserMemoryRepository;
import com.melissa.diary.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class UserMemoryService {
    
    private final UserMemoryRepository userMemoryRepository;
    private final UserRepository userRepository;
    private final ChatClient memoryFusionClient;
    private final ChatClient topicChangeDetectionClient;
    
    public UserMemoryService(
            UserMemoryRepository userMemoryRepository,
            UserRepository userRepository,
            @Qualifier("memoryFusionClient") ChatClient memoryFusionClient,
            @Qualifier("topicChangeDetectionClient") ChatClient topicChangeDetectionClient) {
        this.userMemoryRepository = userMemoryRepository;
        this.userRepository = userRepository;
        this.memoryFusionClient = memoryFusionClient;
        this.topicChangeDetectionClient = topicChangeDetectionClient;
    }
    
    /**
     * 사용자 메모리 조회 (없으면 빈 메모리 생성)
     */
    @Transactional
    public UserMemory getUserMemory(Long userId) {
        return userMemoryRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyMemory(userId));
    }
    
    /**
     * 사용자 메모리 조회 (읽기 전용)
     */
    @Transactional(readOnly = true)
    public UserMemory getUserMemoryReadOnly(Long userId) {
        return userMemoryRepository.findByUserId(userId).orElse(null);
    }
    
    /**
     * 빈 메모리 생성
     */
    private UserMemory createEmptyMemory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
        
        UserMemory memory = UserMemory.builder()
                .user(user)
                .memoryContent("")
                .build();
        
        return userMemoryRepository.save(memory);
    }
    
    /**
     * Thread 요약을 기반으로 사용자 메모리 업데이트
     */
    @Transactional
    public void updateUserMemoryFromThread(Long userId, Thread thread) {
        // Thread에 요약 정보가 없으면 스킵
        if (thread.getSummaryContent() == null || thread.getSummaryContent().trim().isEmpty()) {
            log.info("[UserMemory] Thread에 요약 정보가 없어 메모리 업데이트 스킵. userId={}, threadId={}", 
                    userId, thread.getId());
            return;
        }
        
        UserMemory userMemory = getUserMemory(userId);
        String currentMemory = userMemory.getMemoryContent() != null ? userMemory.getMemoryContent() : "";
        
        // 새로운 일기 요약 정보 구성
        String newDiaryInfo = buildDiaryInfo(thread);
        
        // LLM을 통해 메모리 융합
        String updatedMemory = fuseMemoryWithLLM(currentMemory, newDiaryInfo);
        
        userMemory.updateMemoryContent(updatedMemory);
        userMemoryRepository.save(userMemory);
        
        log.info("[UserMemory] 사용자 메모리 업데이트 완료. userId={}, threadDate={}-{}-{}", 
                userId, thread.getYear(), thread.getMonth(), thread.getDay());
    }
    
    /**
     * Thread 정보를 일기 정보로 변환
     */
    private String buildDiaryInfo(Thread thread) {
        LocalDate diaryDate = LocalDate.of(thread.getYear(), thread.getMonth(), thread.getDay());
        String formattedDate = diaryDate.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"));
        
        StringBuilder diaryInfo = new StringBuilder();
        diaryInfo.append(String.format("[%s] ", formattedDate));
        
        if (thread.getSummaryTitle() != null) {
            diaryInfo.append(thread.getSummaryTitle()).append(" - ");
        }
        
        if (thread.getSummaryContent() != null) {
            diaryInfo.append(thread.getSummaryContent());
        }
        
        if (thread.getMood() != null) {
            diaryInfo.append(" (기분: ").append(thread.getMood().name()).append(")");
        }
        
        if (thread.getHashtag1() != null || thread.getHashtag2() != null) {
            diaryInfo.append(" 태그: ");
            if (thread.getHashtag1() != null) diaryInfo.append("#").append(thread.getHashtag1());
            if (thread.getHashtag2() != null) diaryInfo.append(" #").append(thread.getHashtag2());
        }
        
        return diaryInfo.toString();
    }
    
    /**
     * LLM을 통해 기존 메모리와 새 일기 정보를 융합
     */
    private String fuseMemoryWithLLM(String currentMemory, String newDiaryInfo) {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"));
        
        String prompt = buildMemoryFusionPrompt(currentMemory, newDiaryInfo, currentDate);
        
        try {
            String response = memoryFusionClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            
            return response != null ? response.trim() : currentMemory;
            
        } catch (Exception e) {
            log.error("[UserMemory] 메모리 융합 중 오류 발생", e);
            // LLM 호출 실패 시 기존 메모리에 새 정보 단순 추가
            return currentMemory + "\n\n" + newDiaryInfo;
        }
    }
    
    /**
     * 메모리 융합을 위한 프롬프트 생성
     */
    private String buildMemoryFusionPrompt(String currentMemory, String newDiaryInfo, String currentDate) {
        return String.format("""
                # 역할: 개인 메모리 데이터베이스 관리자
                당신은 사용자의 개인 정보를 구조화된 데이터베이스 형태로 관리하는 전문가입니다.
                
                ## 임무
                기존 메모리 데이터베이스와 새로운 일기 정보를 융합하여 구조화된 USER_DATABASE를 업데이트해주세요.
                
                ## 메모리 융합 규칙
                1. **구조화된 템플릿 사용**: 반드시 아래 USER_DATABASE 템플릿 구조를 따라 작성
                2. **7일 기준 휘발성**:
                   - 7일 이내 정보: recent_7days 섹션에 정확한 날짜(YYYY.MM.DD)와 함께 상세 기록
                   - 7일 초과 정보: 날짜를 제거하고 패턴/습관으로 함축하여 각 카테고리의 일반 정보 섹션에 통합
                     * FOOD → general_patterns에 식사 습관으로 요약
                     * EXERCISE → general_history에 운동 성과로 요약  
                     * EXPERIENCES → memorable_events에 중요 체험으로 요약
                     * SOCIAL → social_history에 관계 발전으로 요약
                     * EMOTIONAL_STATE → emotional_patterns에 감정 변화로 요약
                3. **감정 정보 보존**: 사용자가 느낀 감정은 반드시 기록 (긍정적 경험의 감정 특히 중요)
                4. **카테고리별 분류**: FOOD, EXERCISE, EXPERIENCES, SOCIAL 등 적절한 카테고리에 분류, 적절한 카테고리가 없으면 생성 후 작성
                
                ## USER_DATABASE 템플릿 구조
                ```
                USER_DATABASE
                
                [CORE_PROFILE]
                personality_type: [성격 특성]
                behavioral_style: [행동 패턴, 생활 스타일]
                
                [FOOD]
                preferences: [선호 음식들]
                dislikes: [기피 음식들]
                general_patterns: [7일 초과 휘발된 식사 패턴, 습관 정보]
                recent_7days:
                - YYYY.MM.DD: [음식 관련 경험] (감정: [기분])
                
                [EXERCISE]
                pattern: [운동 패턴, 스타일]
                goals: [운동 목표]
                general_history: [7일 초과 휘발된 운동 기록, 성과 정보]
                recent_7days:
                - YYYY.MM.DD: [운동 경험] (감정: [기분])
                
                [EXPERIENCES]
                interests: [관심사, 취미]
                memorable_events: [7일 초과 휘발된 중요 체험들, 장소 방문 기록]
                recent_7days:
                - YYYY.MM.DD: [체험 내용]
                  context: [상세 내용]
                  emotion: [느낀 감정, 좋았던 점]
                  future_intent: [향후 계획이나 의향]
                
                [SOCIAL]
                relationships: [인간관계 패턴]
                social_history: [7일 초과 휘발된 만남, 관계 발전 과정]
                recent_7days:
                - YYYY.MM.DD: [만남, 소통 경험] (감정: [기분])
                
                [EMOTIONAL_STATE]
                positive_triggers: [긍정적 감정을 주는 요소들]
                stress_factors: [스트레스 요인들]
                emotional_patterns: [7일 초과 휘발된 감정 변화, 성장 과정]
                recent_mood_pattern: [최근 감정 패턴]
                
                [기타 적절한 카테고리]
                (필요시 WORK, HEALTH, TRAVEL 등 추가 카테고리 생성 가능)
                ```
                
                ---
                
                현재 날짜: %s (7일 기준점)
                
                ## 현재 메모리 데이터베이스:
                %s
                
                ## 새로운 일기 정보:
                %s
                
                위 템플릿 구조를 엄격히 따라 기존 메모리와 새로운 정보를 융합한 완전한 USER_DATABASE를 작성해주세요.
                기존 메모리가 없다면 새로운 정보로 첫 데이터베이스를 생성해주세요.
                """, currentDate,
                currentMemory.isEmpty() ?
                """
                USER_DATABASE
                
                [CORE_PROFILE]
                personality_type: 미분석
                behavioral_style: 미분석
                
                [FOOD]
                preferences: []
                dislikes: []
                general_patterns: []
                recent_7days: []
                
                [EXERCISE]
                pattern: []
                goals: []
                general_history: []
                recent_7days: []
                
                [EXPERIENCES]
                interests: []
                memorable_events: []
                recent_7days: []
                
                [SOCIAL]
                relationships: []
                social_history: []
                recent_7days: []
                
                [WORK]
                occupation: []
                work_style: []
                general_patterns: []
                recent_7days: []
                
                [STUDY]
                subjects: []
                learning_style: []
                general_progress: []
                recent_7days: []
                
                [TRAVEL]
                favorite_places: []
                travel_style: []
                memorable_trips: []
                recent_7days: []
                
                [HEALTH]
                health_concerns: []
                wellness_habits: []
                general_patterns: []
                recent_7days: []
                
                [HOBBIES]
                current_hobbies: []
                skill_level: []
                general_activities: []
                recent_7days: []
                
                [SHOPPING]
                preferences: []
                shopping_style: []
                general_patterns: []
                recent_7days: []
                
                [WEATHER_MOOD]
                weather_preferences: []
                seasonal_patterns: []
                general_observations: []
                recent_7days: []
                
                [EMOTIONAL_STATE]
                positive_triggers: []
                stress_factors: []
                emotional_patterns: []
                recent_mood_pattern: []
                """ : currentMemory, 
                newDiaryInfo);
    }
    
    /**
     * 사용자 메모리 초기화
     */
    @Transactional
    public void resetUserMemory(Long userId) {
        UserMemory userMemory = getUserMemory(userId);
        userMemory.updateMemoryContent("");
        userMemoryRepository.save(userMemory);
        
        log.info("[UserMemory] 사용자 메모리 초기화 완료. userId={}", userId);
    }
    
    /**
     * 메모리 내용이 있는지 확인
     */
    @Transactional(readOnly = true)
    public boolean hasMemoryContent(Long userId) {
        UserMemory memory = getUserMemoryReadOnly(userId);
        return memory != null && 
               memory.getMemoryContent() != null && 
               !memory.getMemoryContent().trim().isEmpty();
    }
    
    /**
     * 주제 변경 감지 (LLM 기반) - 전체 대화 맥락 고려
     */
    public boolean detectTopicChange(String todayConversation, String currentMessage) {
        if (todayConversation == null || todayConversation.trim().isEmpty()) {
            return false; // 기존 대화가 없으면 주제 변경 없음
        }
        
        String prompt = buildTopicChangeDetectionPrompt(todayConversation, currentMessage);
        
        try {
            String response = topicChangeDetectionClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            
            return response != null && response.trim().toLowerCase().contains("true");
            
        } catch (Exception e) {
            log.error("[UserMemory] 주제 변경 감지 중 오류 발생", e);
            return false; // 오류 시 주제 변경 없음으로 처리
        }
    }
    
    /**
     * 주제 변경 감지를 위한 프롬프트 생성
     */
    private String buildTopicChangeDetectionPrompt(String todayConversation, String currentMessage) {
        return String.format("""
                # 역할: 대화 주제 변경 감지 전문가
                당신은 대화의 흐름을 분석하여 주제 변경을 정확하게 감지하는 전문가입니다.
                
                ## 임무
                기존 대화 맥락과 새로운 메시지를 비교하여 주제 전환 여부를 정확히 판단해주세요.
                
                ## 판단 기준
                ** 주제 변경 (true 응답):**
                - 완전히 다른 분야로의 전환 (음식 → 운동, 일상 → 감정상담, 취미 → 인간관계)
                - 새로운 관심사나 활동 시작 (기존에 다루지 않던 새로운 주제)
                - 시간적/공간적 맥락의 급격한 변화
                
                ** 주제 변경 아님 (false 응답):**
                - 같은 주제 내 세부 사항 변화 (파스타 → 피자, 둘 다 음식)
                - 자연스러운 연관 주제로의 확장 (음식 → 요리법 → 주방용품)
                - 감정적 반응이나 후속 질문 (기존 주제에 대한 추가 정보)
                - 맥락상 연결되는 대화 (운동 → 몸이 아픔 → 휴식)
                
                ## 출력 규칙
                - 주제가 변경되었으면 정확히 "true"만 출력
                - 주제가 변경되지 않았으면 정확히 "false"만 출력
                - 다른 설명이나 부가 정보는 절대 포함하지 않음
                
                ---
                
                ## 오늘의 대화 내용:
                %s
                
                ## 현재 사용자 메시지:
                "%s"
                
                위 기준에 따라 현재 메시지가 기존 대화 주제와 완전히 다른 새로운 분야로 전환되었는지 판단해주세요.
                """, todayConversation, currentMessage);
    }
}
