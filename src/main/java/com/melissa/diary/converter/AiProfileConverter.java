package com.melissa.diary.converter;

import com.melissa.diary.domain.AiProfile;
import com.melissa.diary.domain.User;
import com.melissa.diary.web.dto.AiProfileRequestDTO;

public class AiProfileConverter {

    // Req DTO -> Entity
    public static AiProfile toAiProfile(AiProfileRequestDTO.AiProfileCreateRequest request, User user){
        return AiProfile.builder()
                .

                .build();
    }

}
