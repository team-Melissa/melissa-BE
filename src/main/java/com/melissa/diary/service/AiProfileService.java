package com.melissa.diary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melissa.diary.ai.ImageGenerator;
import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.aws.s3.AmazonS3Manager;
import com.melissa.diary.converter.AiProfileConverter;
import com.melissa.diary.domain.AiProfile;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.Uuid;
import com.melissa.diary.repository.AiProfileRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.repository.UuidRepository;
import com.melissa.diary.web.dto.AiProfileRequestDTO;
import com.melissa.diary.web.dto.AiProfileResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiProfileService {

    private final AiProfileRepository aiProfileRepository;
    private final UserRepository userRepository;
    private final ChatModel chatModel;
    private final ImageGenerator imageGenerator;
    // Jackson : json 맵핑 도와주는 객체
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public AiProfileResponseDTO.AiProfileResponse createAiProfile(Long userId,
                                                                  AiProfileRequestDTO.AiProfileCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        // 1) 프롬프트 생성
        String promptText = buildPromptProfileText(request);

        // 2) LLM 호출
        String llmResponseJson = callLLM(promptText);

        // 3) JSON 파싱 + 결과 프롬프트 저장 -> 객체생성
        AiProfile newProfile = parseLlmResponse(llmResponseJson);
        newProfile.setPromptText(promptText); // LLM에게 보낸 프롬프트 전문 저장
        newProfile.setUser(user);

        // 4) 프롬프트 생성
        String promptImage = buildPromptProfileImage(newProfile);

        // 4) 프로필사진 생성
        String imageUrl = imageGenerator.genProfileImage(promptImage);
        newProfile.setImageS3(imageUrl);
        newProfile.setQ1(request.getQ1());
        newProfile.setQ2(request.getQ2());
        newProfile.setQ3(request.getQ3());
        newProfile.setQ4(request.getQ4());
        newProfile.setQ5(request.getQ5());
        newProfile.setQ6(request.getQ6());

        // 5 DB 저장
        AiProfile saved = aiProfileRepository.save(newProfile);

        // 6 변환 후 반환
        return AiProfileConverter.toResponse(saved);
    }




    @Transactional
    public AiProfileResponseDTO.AiProfileResponse getAiProfile(Long userId, Long aiProfileId){
        AiProfile aiProfile = aiProfileRepository.findById(aiProfileId).orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));

        if (!aiProfile.getUser().getId().equals(userId)){
            throw new ErrorHandler(ErrorStatus.PROFILE_NOT_UNAUTHORIZED);
        }

        return  AiProfileConverter.toResponse(aiProfile);

    }

    @Transactional
    public AiProfileResponseDTO.AiProfileQuestionResponse getAiProfileQuestion(Long userId, Long aiProfileId){
        AiProfile aiProfile = aiProfileRepository.findById(aiProfileId).orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));
        if (!aiProfile.getUser().getId().equals(userId)){
            throw new ErrorHandler(ErrorStatus.PROFILE_NOT_UNAUTHORIZED);
        }
        return AiProfileConverter.toQuestion(aiProfile);

    }

    @Transactional
    public List<AiProfileResponseDTO.AiProfileResponse> getAiProfileList(Long userId){
        List<AiProfile> aiProfileList = aiProfileRepository.findByUserId(userId);

        return aiProfileList.stream().map(AiProfileConverter::toResponse).toList();
    }


    @Transactional
    public void deleteAiProfile(Long userId, Long aiProfileId) {
        AiProfile aiProfile = aiProfileRepository.findById(aiProfileId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));

        if (!aiProfile.getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.PROFILE_NOT_UNAUTHORIZED);
        }
        aiProfileRepository.delete(aiProfile);
    }


    private String buildPromptProfileText(AiProfileRequestDTO.AiProfileCreateRequest req) {
        // 예시: Q들의 답변을 이어붙여서 프롬프트 형태로 구성
        // 실제로는 좀 더 정교하게 작성 가능
        return """
               아래의 6가지 정보를 바탕으로, 다음 JSON을 생성해주세요:
               반드시 형식을 지켜 예시 응답(Json)처럼 리턴해주세요.
               1) profileName: 대화 상대에 어울리는 귀여운 이름. 형용상 뒤의 이름은 동물이나 사물로 한정 (예: "행복한 빵빵이")
               2) hashTag1, hashTag2: 2가지 해시태그
               3) feature1, feature2, feature3: 3가지 특징
               
               질문과 답변:
               Q1(성격): %s
               Q2(대화주제): %s
               Q3(대화스타일): %s
               Q4(연령대/분위기): %s
               Q5(목적성): %s
               Q6(언어표현방식): %s
               
               형식:
               {
                 "profileName": "...",
                 "imageS3": "...",
                 "hashTag1": "...",
                 "hashTag2": "...",
                 "feature1": "...",
                 "feature2": "...",
                 "feature3": "..."
               }
               
               답변 예시:
               {
                 "profileName": "행복한 빵빵이",
                 "hashTag1": "무사태평",
                 "hashTag2": "공감",
                 "feature1": "쾌활하고 친근함",
                 "feature2": "언제나 긍정적인 에너지",
                 "feature3": "친구처럼 편한 대화"
               }
               """.formatted(
                req.getQ1(), req.getQ2(), req.getQ3(),
                req.getQ4(), req.getQ5(), req.getQ6()
        );
    }

    private String callLLM(String promptText) {
        ChatResponse response = chatModel.call(
                new Prompt(
                        promptText,
                        OpenAiChatOptions.builder()
                                .model(OpenAiApi.ChatModel.GPT_4_O)
                                .temperature(0.4)
                                .build()
                ));
        System.out.println(response.getResult().getOutput().getText());
        return response.getResult().getOutput().getText();
    }

    private AiProfile parseLlmResponse(String llmResponseJson) {
        try {
            // 1. JSON 시작(`{`) 인덱스와 JSON 끝(`}`) 인덱스를 찾는다.
            int startIndex = llmResponseJson.indexOf("{");
            int endIndex = llmResponseJson.lastIndexOf("}");

            // 만약 { 또는 } 가 없다면 잘못된 형식이므로 예외 처리
            if (startIndex == -1 || endIndex == -1) {
                throw new RuntimeException("JSON 형식이 올바르지 않습니다: " + llmResponseJson);
            }

            // 2. 실제 JSON 내용만 잘라낸다.
            String jsonContent = llmResponseJson.substring(startIndex, endIndex + 1);

            // 3. 잘라낸 JSON 내용을 파싱한다.
            JsonNode node = objectMapper.readTree(jsonContent);

            // 4. 필요한 필드를 꺼내서 AiProfile 객체를 구성한다.
            return AiProfile.builder()
                    .profileName(node.get("profileName").asText())
                    .hashTag1(node.get("hashTag1").asText())
                    .hashTag2(node.get("hashTag2").asText())
                    .feature1(node.get("feature1").asText())
                    .feature2(node.get("feature2").asText())
                    .feature3(node.get("feature3").asText())
                    .imageS3(node.has("imageS3") ? node.get("imageS3").asText() : null)
                    .promptText(jsonContent)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("LLM 응답 파싱 실패", e);
        }
    }

    private String buildPromptProfileImage(AiProfile aiProfile) {
        // 예시: Q들의 답변을 이어붙여서 프롬프트 형태로 구성
        return """
               아래 7가지 정보를 바탕으로 캐릭터 프로필 사진을 만들어줘.
               그림체는 카툰풍으로 귀엽게, 누구나 호불호 없도록 만들어줘.
               이름을 바탕으로 해당 동물을 생성하고 해시태그와 특징을 그림에 잘 녹여줘.
               얼굴을 메인으로 프로필사진! 글은 쓰지마.
               
               {
                 "profileName": %s,
                 "hashTag1": "%s,
                 "hashTag2": %s,
                 "feature1": %s,
                 "feature2": %s,
                 "feature3": %s
               }
               
               """.formatted(
                aiProfile.getProfileName(),
                aiProfile.getHashTag1(),
                aiProfile.getHashTag2(),
                aiProfile.getFeature1(),
                aiProfile.getFeature2(),
                aiProfile.getFeature3()
                );
    }

}
