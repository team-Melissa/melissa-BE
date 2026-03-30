package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.converter.AiProfileConverter;
import com.melissa.diary.domain.AiProfile;
import com.melissa.diary.domain.DailyChatLog;
import com.melissa.diary.repository.AiProfileRepository;
import com.melissa.diary.repository.DailyChatLogRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.web.dto.AiProfileResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiProfileService {

    private final AiProfileRepository aiProfileRepository;
    private final DailyChatLogRepository dailyChatLogRepository;
    private final UserRepository userRepository;
    private final AiProfileConverter aiProfileConverter;

    @Transactional(readOnly = true)
    public AiProfileResponseDTO.AiProfileResponse getAiProfile(Long userId, Long aiProfileId){
        userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        AiProfile aiProfile = aiProfileRepository.findByIdAndActiveIsTrue(aiProfileId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));

        return aiProfileConverter.toResponse(aiProfile);
    }

    @Transactional(readOnly = true)
    public AiProfileResponseDTO.AiProfileQuestionResponse getAiProfileQuestion(Long userId, Long aiProfileId){
        userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        AiProfile aiProfile = aiProfileRepository.findByIdAndActiveIsTrue(aiProfileId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));
        
        return aiProfileConverter.toQuestion(aiProfile);
    }

    @Transactional(readOnly = true)
    public List<AiProfileResponseDTO.AiProfileResponse> getAiProfileList(Long userId){
        userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        List<AiProfile> aiProfileList = aiProfileRepository.findByActiveIsTrueOrderByIdAsc();

        return aiProfileList.stream().map(aiProfileConverter::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AiProfileResponseDTO.AiProfileResponse getRecentAiProfileId(Long userId) {
        // 가장 최근 채팅 로그 기반으로 AI 프로필 조회
        Optional<DailyChatLog> recentChatLog = dailyChatLogRepository.findFirstByThreadUserIdOrderByCreatedAtDesc(userId);
        
        if (recentChatLog.isEmpty()) {
            // 채팅 기록이 없으면 첫 번째 프로필 반환
            List<AiProfile> profiles = aiProfileRepository.findByActiveIsTrueOrderByIdAsc();
            if (profiles.isEmpty()) throw new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND);
            return aiProfileConverter.toResponse(profiles.get(0));
        }
        
        AiProfile recentProfile = recentChatLog.get().getThread().getAiProfile();
        if (!recentProfile.isActive()) {
            throw new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND);
        }
        return aiProfileConverter.toResponse(recentProfile);
    }
}
