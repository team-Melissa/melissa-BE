package com.melissa.diary.converter;

import com.melissa.diary.aws.s3.S3AssetUrlResolver;
import com.melissa.diary.domain.AiProfile;
import com.melissa.diary.web.dto.AiProfileResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiProfileConverter {

    private final S3AssetUrlResolver s3AssetUrlResolver;

    public AiProfileResponseDTO.AiProfileResponse toResponse(AiProfile aiProfile){
        return AiProfileResponseDTO.AiProfileResponse.builder()
                .aiProfileId(aiProfile.getId())
                .profileName(aiProfile.getProfileName())
                .feature1(aiProfile.getFeature1())
                .feature2(aiProfile.getFeature2())
                .feature3(aiProfile.getFeature3())
                .hashTag1(aiProfile.getHashTag1())
                .hashTag2(aiProfile.getHashTag2())
                .imageUrl(s3AssetUrlResolver.resolve(aiProfile.getImageS3()))
                .createdAt(aiProfile.getCreatedAt())
                .isDefault(true)
                .build();
    }

    public AiProfileResponseDTO.AiProfileQuestionResponse toQuestion(AiProfile aiProfile){
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
