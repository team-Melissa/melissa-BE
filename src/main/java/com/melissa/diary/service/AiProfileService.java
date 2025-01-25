package com.melissa.diary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.converter.AiProfileConverter;
import com.melissa.diary.domain.AiProfile;
import com.melissa.diary.domain.User;
import com.melissa.diary.repository.AiProfileRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.web.dto.AiProfileRequestDTO;
import com.melissa.diary.web.dto.AiProfileResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class AiProfileService {

    private final AiProfileRepository aiProfileRepository;
    private final UserRepository userRepository;
    // Jackson : json 맵핑 도와주는 객체
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 현재는 그냥 목업으로 구현 - 프롬프팅할 때 모두 추가 예정
     * 1) 6개 질문(q1~q6) → 프롬프트 생성
     * 2) LLM 호출 → 프로필에 필요한 데이터(JSON)
     * 3) JSON 파싱 → AiProfile 엔티티 세팅
     * 4) 이미지 생성
     * 5) DB 저장
     * 6) 응답 DTO 리턴
     */
    @Transactional
    public AiProfileResponseDTO.AiProfileResponse createAiProfile(Long userId,
                                                                  AiProfileRequestDTO.AiProfileCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        // 1 프롬프트 생성
        String promptText = buildPromptText(request);

        // 2 LLM 호출
        // TODO 실제로는 OpenAI API에 promptText를 전송해 결과 JSON을 얻는다
        String llmResponseJson = callLLM(promptText);

        // 3 JSON 파싱 + 결과 프롬프트 저장 -> 객체생성
        AiProfile newProfile = parseLlmResponse(llmResponseJson);
        newProfile.setPromptText(promptText); // LLM에게 보낸 프롬프트 전문 저장
        newProfile.setUser(user);

        // 4 이미지 생성
        // 지금은 생략
        // TODO 실제로는 이미지를 생성해서 S3에 저장하는 로직 필요.

        // 5 DB 저장
        AiProfile saved = aiProfileRepository.save(newProfile);

        // 6 변환 후 반환
        return AiProfileConverter.toResponse(saved);
    }











    private String buildPromptText(AiProfileRequestDTO.AiProfileCreateRequest req) {
        // 예시: Q들의 답변을 이어붙여서 프롬프트 형태로 구성
        // 실제로는 좀 더 정교하게 작성 가능
        return """
               아래의 6가지 정보를 바탕으로, 다음 JSON을 생성해주세요:
               1) profileName: 대화 상대에 어울리는 귀여운 이름 (예: "행복한 빵빵이")
               2) hashTag1, hashTag2: 2가지 해시태그
               3) feature1, feature2, feature3: 3가지 특징
               
               질문과 답변:
               Q1(성격): %s
               Q2(대화주제): %s
               Q3(대화스타일): %s
               Q4(연령대/분위기): %s
               Q5(목적성): %s
               Q6(언어표현방식): %s
               
               형식 예시:
               {
                 "profileName": "...",
                 "imageS3": "...",
                 "hashTag1": "...",
                 "hashTag2": "...",
                 "feature1": "...",
                 "feature2": "...",
                 "feature3": "..."
               }
               """.formatted(
                req.getQ1(), req.getQ2(), req.getQ3(),
                req.getQ4(), req.getQ5(), req.getQ6()
        );
    }

    private String callLLM(String promptText) {
        // 여기서는 간단히, 하드코딩된 JSON을 응답으로 가정
        // 실제로는 HTTP 통신, ChatCompletion API, etc.
        // 응답: profileName, imageS3, hashTag1, hashTag2, feature1~3
        return """
               {
                 "profileName": "행복한 빵빵이",
                 "hashTag1": "행복이",
                 "hashTag2": "빵빵해요",
                 "feature1": "쾌활하고 친근함",
                 "feature2": "언제나 긍정적인 에너지",
                 "feature3": "빵을 매우 좋아함"
               }
               """;
    }

    private AiProfile parseLlmResponse(String llmResponseJson) {
        try {
            JsonNode node = objectMapper.readTree(llmResponseJson);

            return AiProfile.builder()
                    .profileName(node.get("profileName").asText())
                    .hashTag1(node.get("hashTag1").asText())
                    .hashTag2(node.get("hashTag2").asText())
                    .feature1(node.get("feature1").asText())
                    .feature2(node.get("feature2").asText())
                    .feature3(node.get("feature3").asText())
                    .promptText(null)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("LLM 응답 파싱 실패", e);
        }
    }

}
