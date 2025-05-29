package com.melissa.diary.service;

import com.melissa.diary.ai.ImageGenerator;
import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.AiProfile;
import com.melissa.diary.repository.AiProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiProfileImageService {

    private final AiProfileRepository aiProfileRepository;
    private final ImageGenerator imageGenerator;

    /** 프로필 ID 기준으로 이미지 생성·S3 업로드·DB 반영을 비동기로 수행 */
    @Async("asyncTaskExecutor")
    public void generateAndSaveProfileImage(Long aiProfileId) {
        try {
            AiProfile profile = aiProfileRepository.findById(aiProfileId)
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));

            String prompt = buildPromptProfileImage(profile);
            String url    = imageGenerator.genProfileImage(prompt);

            updateImageUrl(profile, url);
        } catch (Exception e) {
            log.error("[Async-ProfileImage] 생성 실패 id={}", aiProfileId, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateImageUrl(AiProfile profile, String url) {
        profile.setImageS3(url);
        aiProfileRepository.save(profile);
    }

    private String buildPromptProfileImage(AiProfile p) {
        // 간단 예시: 프로필 이름만 넣어도 충분히 유니크한 캐릭터 그림을 얻을 수 있음
        return "%s, 단일 캐릭터, 픽사 스타일, 귀여운 일러스트".formatted(p.getProfileName());
    }
}