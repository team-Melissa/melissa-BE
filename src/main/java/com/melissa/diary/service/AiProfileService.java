package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.converter.AiProfileConverter;
import com.melissa.diary.domain.AiProfile;
import com.melissa.diary.domain.Thread;
import com.melissa.diary.repository.AiProfileRepository;
import com.melissa.diary.repository.ThreadRepository;
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
    private final ThreadRepository threadRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AiProfileResponseDTO.AiProfileResponse getAiProfile(Long userId, Long aiProfileId){
        userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        AiProfile aiProfile = aiProfileRepository.findByIdAndActiveIsTrue(aiProfileId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));

        return AiProfileConverter.toResponse(aiProfile);
    }

    @Transactional(readOnly = true)
    public AiProfileResponseDTO.AiProfileQuestionResponse getAiProfileQuestion(Long userId, Long aiProfileId){
        userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        AiProfile aiProfile = aiProfileRepository.findByIdAndActiveIsTrue(aiProfileId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));
        
        return AiProfileConverter.toQuestion(aiProfile);
    }

    @Transactional(readOnly = true)
    public List<AiProfileResponseDTO.AiProfileResponse> getAiProfileList(Long userId){
        userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        List<AiProfile> aiProfileList = aiProfileRepository.findByActiveIsTrueOrderByIdAsc();

        return aiProfileList.stream().map(AiProfileConverter::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AiProfileResponseDTO.AiProfileResponse getRecentAiProfileId(Long userId) {
        Optional<Thread> recentThread = threadRepository.findFirstByUserIdOrderByYearDescMonthDescDayDesc(userId);
        
        if (recentThread.isEmpty()) {
            List<AiProfile> profiles = aiProfileRepository.findByActiveIsTrueOrderByIdAsc();
            if (profiles.isEmpty()) throw new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND);
            return AiProfileConverter.toResponse(profiles.get(0));
        }
        
        AiProfile recentProfile = recentThread.get().getAiProfile();
        if (!recentProfile.isActive()) {
            throw new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND);
        }
        return AiProfileConverter.toResponse(recentProfile);
    }
}
