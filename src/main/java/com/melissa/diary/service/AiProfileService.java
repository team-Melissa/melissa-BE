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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AiProfileService {

    private final AiProfileRepository aiProfileRepository;
    private final UserRepository userRepository;
    private final ChatClient chatClient;
    private final ImageGenerator imageGenerator;
    // Jackson : json 맵핑 도와주는 객체
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiProfileService(AiProfileRepository aiProfileRepository, UserRepository userRepository, @Qualifier("profileClient") ChatClient chatClient, ImageGenerator imageGenerator) {
        this.aiProfileRepository = aiProfileRepository;
        this.userRepository = userRepository;
        this.chatClient = chatClient;
        this.imageGenerator = imageGenerator;
    }

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
            throw new ErrorHandler(ErrorStatus.PROFILE_FORBIDDEN);
        }

        return  AiProfileConverter.toResponse(aiProfile);

    }

    @Transactional
    public AiProfileResponseDTO.AiProfileQuestionResponse getAiProfileQuestion(Long userId, Long aiProfileId){
        AiProfile aiProfile = aiProfileRepository.findById(aiProfileId).orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));
        if (!aiProfile.getUser().getId().equals(userId)){
            throw new ErrorHandler(ErrorStatus.PROFILE_FORBIDDEN);
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
            throw new ErrorHandler(ErrorStatus.PROFILE_FORBIDDEN);
        }
        aiProfileRepository.delete(aiProfile);
    }


    private String buildPromptProfileText(AiProfileRequestDTO.AiProfileCreateRequest req) {
        return """
                 아래의 6가지 정보를 바탕으로, 다음 JSON을 생성해주세요:
                 반드시 형식을 지켜 예시 응답(Json)처럼 리턴해주세요.
                                
                 1) profileName: 대화 상대에 어울리는 귀여운 이름. 형용사 뒤의 이름은 사물 또는 동물로 한정
                 (예: '행복한 빵빵이, 즐거운 몽몽이, 말랑한 구름이, 달콤한 마시멜로우, 보송한 리본, 엉뚱한 솔방울')
                 2) firstChat : 첫 인사말 작성
                 - 연령대에 맞는 표현 활용
                 - 이모티콘 적절히 사용 (profileName에 관련된 이모티콘 또는 인사말에 맞는 이모티콘 선택)
                 ٩(ˊᗜˋ*)و // ｡•̀ᴗ-)✧//◝(⑅•ᴗ•⑅)◜..°♡ //  (◍•ᴗ•◍) //  (ﾉ≧∀≦)ﾉ
                 - 함께할 내용 제안, 상대방 상황에 대한 공감/질문, 인사말
                 - "안녕하세요", "반갑습니다" 같은 형식적 인사 금지
                 3) hashTag1, hashTag2: 2가지 해시태그
                 - 대화 상대의 연령대와 분위기 반영
                 4) feature1, feature2, feature3: 3가지 구체적인 행동이나 특징, 15자 이내
                 5) defaultSystem: 채팅 서비스에서 ai의 성격 정의
                 - 사용자가 원하는 특징을 지닌 프로필에 대한 설명을 적어줘.
                 - 이는 나중에 사용자의 대화를 이끌 ai의 프로필 정의항목에 들어갈 예정.
                 - 그리고 생성한 프로필과도 잘 융합하도록 반영해.
                                
                 질문과 답변:
                 Q1(성격): %s
                 Q2(대화주제): %s
                 Q3(대화스타일): %s
                 Q4(연령대/분위기): %s
                 Q5(목적성): %s
                 Q6(언어표현방식): %s
                                
                                
                 반드시 형식을 지켜 예시 응답(Json)처럼 리턴해주세요.
                 형식:
                 {
                   "profileName": "...",
                   "firstChat": "...",
                   "hashTag1": "...",
                   "hashTag2": "...",
                   "feature1": "...",
                   "feature2": "...",
                   "feature3": "...",
                   "defaultSystem": "..."
                 }
                                
                 답변 예시:
                 {
                   "profileName": "행복한 빵빵이",
                   "firstChat": "오늘 하루는 어땠어?",
                   "hashTag1": "일잘러들의_TMI",
                   "hashTag2": "오늘의_발견",
                   "feature1": "지루한 IT 용어도 맛있게 씹어먹는 달달 설명",
                   "feature2": "퇴근 후 우리만의 자기계발 수다",
                   "feature3": "찰떡같이 알아듣는 맞춤 솔루션",
                   "defaultSystem": "이 AI의 프로필 이름은 '행복한 빵빵이'이다. 첫 메시지는 항상 '오늘 하루는 어땠어?'로 시작한다.
                                     이 AI는 '#일잘러들의_TMI'와 '#오늘의_발견'이라는 해시태그를 기반으로 대화의 흐름을 유지하며, 다음과 같은 특징을 갖는다.
                                     
                                     첫째, 어려운 IT 용어를 쉽게 풀어 설명하여 누구나 이해할 수 있도록 한다.
                                     둘째, 퇴근 후 자기계발과 관련된 다양한 주제로 대화를 나눈다.
                                     셋째, 상대방이 원하는 정보를 정확히 이해하고 최적의 맞춤형 솔루션을 제공한다.
                                     
                                     이에 따라, AI는 IT 용어를 친근하게 설명하고, 자기계발 관련 대화를 유도하며, 사용자의 질문이나 고민에 대해 쉽게 이해할 수 있는 맞춤형 답변을 제공하도록 한다."
                 }
                """.formatted(
                req.getQ1(), req.getQ2(), req.getQ3(),
                req.getQ4(), req.getQ5(), req.getQ6()
        );
    }

    private String callLLM(String promptText) {
        return chatClient.prompt()
                .user(promptText)
                .call()
                .content();
    }

    private AiProfile parseLlmResponse(String llmResponseJson) {
        try {
            // 1. JSON 시작(`{`) 인덱스와 JSON 끝(`}`) 인덱스를 찾는다.
            int startIndex = llmResponseJson.indexOf("{");
            int endIndex = llmResponseJson.lastIndexOf("}");

            // 만약 { 또는 } 가 없다면 잘못된 형식이므로 예외 처리
            if (startIndex == -1 || endIndex == -1) {
                throw new ErrorHandler(ErrorStatus.PARSING_FAIL);
            }

            // 2. 실제 JSON 내용만 잘라낸다.
            String jsonContent = llmResponseJson.substring(startIndex, endIndex + 1);

            // 3. 잘라낸 JSON 내용을 파싱한다.
            JsonNode node = objectMapper.readTree(jsonContent);

            // 4. 필요한 필드를 꺼내서 AiProfile 객체를 구성한다.
            return AiProfile.builder()
                    .profileName(node.get("profileName").asText())
                    .firstChat(node.get("firstChat").asText())
                    .hashTag1(node.get("hashTag1").asText())
                    .hashTag2(node.get("hashTag2").asText())
                    .feature1(node.get("feature1").asText())
                    .feature2(node.get("feature2").asText())
                    .feature3(node.get("feature3").asText())
                    .promptText(node.get("defaultSystem").asText())
                    .q1(null)
                    .q2(null)
                    .q3(null)
                    .q4(null)
                    .q5(null)
                    .q6(null)
                    .imageS3(node.has("imageS3") ? node.get("imageS3").asText() : null)
                    .promptText(jsonContent)
                    .build();

        } catch (IOException e) {
            throw new ErrorHandler(ErrorStatus.PARSING_FAIL);
        }
    }

    private String buildPromptProfileImage(AiProfile aiProfile) {
        // 예시: Q들의 답변을 이어붙여서 프롬프트 형태로 구성
        return """
                %s. 단일 캐릭터의 카툰 일러스트. 픽사 스타일. 디즈니 스타일.
                """.formatted(
                aiProfile.getProfileName()
                );
    }

}
