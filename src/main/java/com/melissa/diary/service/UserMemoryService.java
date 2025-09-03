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
                # 역할: 개인 메모리 관리 전문가
                당신은 사용자의 개인적인 기억을 인간의 방식으로 관리하는 전문가입니다.
                
                ## 임무
                기존 기억과 새로운 경험을 자연스럽게 융합하여 하나의 일관된 기억으로 통합해주세요.
                
                ## 메모리 융합 규칙
                1. **시간 기반 기억**:
                   - 7일 이내 사건: "%s 기준으로 7일 이내면 정확한 날짜 포함" (예: "1월 15일에 헬스장에서...")
                   - 7일 초과 사건: "날짜 생략하고 시간적 표현 사용" (예: "최근에", "얼마 전에", "요즘")
                
                2. **자연스러운 통합**:
                   - 비슷한 경험들을 패턴으로 인식하여 통합
                   - 감정의 변화와 성장 과정을 스토리로 연결
                   - 친구가 기억하는 것처럼 따뜻하고 개인적인 톤 사용
                
                3. **주제별 분류**: 운동, 식사, 감정, 인간관계, 취미 등으로 자연스럽게 분류
                
                ---
                
                현재 날짜: %s
                
                ## 현재 기억:
                %s
                
                ## 새로운 일기 정보:
                %s
                
                위 규칙에 따라 기존 기억과 새로운 일기 정보를 융합하여 업데이트된 통합 기억을 작성해주세요.
                기억이 없다면 새로운 정보로 첫 기억을 만들어주세요.
                """, currentDate, currentDate,
                currentMemory.isEmpty() ? "(아직 기억이 없음)" : currentMemory, 
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
