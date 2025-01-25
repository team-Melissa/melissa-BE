package com.melissa.diary.converter;

import com.melissa.diary.domain.AiProfile;
import com.melissa.diary.domain.User;
import com.melissa.diary.web.dto.AiProfileRequestDTO;
import com.melissa.diary.web.dto.AiProfileResponseDTO;

public class AiProfileConverter {

    // Entity -> Response DTO
    public static AiProfileResponseDTO.AiProfileResponse toResponse(AiProfile aiProfile){
        return AiProfileResponseDTO.AiProfileResponse.builder()
                .aiProfileId(aiProfile.getId())
                .profileName(aiProfile.getProfileName())
                .imageUrl(aiProfile.getImageS3())
                .feature1(aiProfile.getFeature1())
                .feature2(aiProfile.getFeature2())
                .feature3(aiProfile.getFeature3())
                .hashTag1(aiProfile.getHashTag1())
                .hashTag2(aiProfile.getHashTag2())
                .createdAt(aiProfile.getCreatedAt())
                .build();
    }

}
