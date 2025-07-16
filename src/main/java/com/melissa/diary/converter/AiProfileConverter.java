package com.melissa.diary.converter;

import com.melissa.diary.domain.AiProfile;
import com.melissa.diary.domain.DefaultAiProfile;
import com.melissa.diary.domain.User;
import com.melissa.diary.web.dto.AiProfileRequestDTO;
import com.melissa.diary.web.dto.AiProfileResponseDTO;

public class AiProfileConverter {

    // Entity -> Response DTO
    public static AiProfileResponseDTO.AiProfileResponse toResponse(DefaultAiProfile defaultAiProfile) {
        return AiProfileResponseDTO.AiProfileResponse.builder()
                .aiProfileId(defaultAiProfile.getId())
                .profileName(defaultAiProfile.getProfileName())
                .feature1(defaultAiProfile.getFeature1())
                .feature2(defaultAiProfile.getFeature2())
                .feature3(defaultAiProfile.getFeature3())
                .hashTag1(defaultAiProfile.getHashTag1())
                .hashTag2(defaultAiProfile.getHashTag2())
                .imageUrl(defaultAiProfile.getImageS3())
                .createdAt(defaultAiProfile.getCreatedAt())
                .isDefault(true)
                .build();
    }

    public static AiProfileResponseDTO.AiProfileResponse toResponse(AiProfile aiProfile){
        return AiProfileResponseDTO.AiProfileResponse.builder()
                .aiProfileId(aiProfile.getId())
                .profileName(aiProfile.getProfileName())
                .feature1(aiProfile.getFeature1())
                .feature2(aiProfile.getFeature2())
                .feature3(aiProfile.getFeature3())
                .hashTag1(aiProfile.getHashTag1())
                .hashTag2(aiProfile.getHashTag2())
                .imageUrl(aiProfile.getImageS3())
                .createdAt(aiProfile.getCreatedAt())
                .isDefault(false)
                .build();
    }

    public static AiProfileResponseDTO.AiProfileQuestionResponse toQuestion(AiProfile aiProfile){
        return AiProfileResponseDTO.AiProfileQuestionResponse.builder()
                .q1(aiProfile.getQ1())
                .q2(aiProfile.getQ2())
                .q3(aiProfile.getQ3())
                .q4(aiProfile.getQ4())
                .q5(aiProfile.getQ5())
                .q6(aiProfile.getQ6())
                .createdAt(aiProfile.getCreatedAt())
                .build();
    }

}
