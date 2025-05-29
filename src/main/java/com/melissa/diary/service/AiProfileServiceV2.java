package com.melissa.diary.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.converter.AiProfileConverter;
import com.melissa.diary.domain.AiProfile;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.enums.UsageCost;
import com.melissa.diary.repository.AiProfileRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.web.dto.AiProfileRequestDTO;
import com.melissa.diary.web.dto.AiProfileResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service("aiProfileV2")
public class AiProfileServiceV2 {

    private final UserRepository userRepo;
    private final AiProfileRepository profileRepo;
    private final @Qualifier("profileClient") ChatClient profileClient;
    private final AiProfileImageService     imageService;
    private final QuotaService              quotaService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiProfileServiceV2(UserRepository userRepo, AiProfileRepository profileRepo, @Qualifier("profileClient") ChatClient profileClient, AiProfileImageService imageService, QuotaService quotaService) {
        this.userRepo = userRepo;
        this.profileRepo = profileRepo;
        this.profileClient = profileClient;
        this.imageService = imageService;
        this.quotaService = quotaService;
    }

    /** V2: 텍스트 프로필 즉시 리턴, 이미지 비동기 */
    @Transactional
    public AiProfileResponseDTO.AiProfileResponse createAiProfile(
            Long userId, AiProfileRequestDTO.AiProfileCreateRequest req) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
        quotaService.checkAndConsume(user, UsageCost.PROFILE);

        // 1) LLM 호출로 프로필 텍스트 생성
        String prompt       = buildPromptProfileText(req);
        String llmJson      = profileClient.prompt().user(prompt).call().content();

        // 2) JSON 파싱 → AiProfile entity (imageS3 = null)
        AiProfile profile   = parseLlmResponse(llmJson);
        profile.setUser(user);
        profile.setQ1(req.getQ1());
        profile.setQ2(req.getQ2());
        profile.setQ3(req.getQ3());
        profile.setQ4(req.getQ4());
        profile.setQ5(req.getQ5());
        profile.setQ6(req.getQ6());
        profile.setImageS3(null);                // **중요**

        // 3) 저장 (same TX)
        AiProfile saved = profileRepo.save(profile);

        // 4) 비동기 이미지 생성 시작
        imageService.generateAndSaveProfileImage(saved.getId());

        // 5) DTO 변환 (imageUrl == null) 즉시 리턴
        return AiProfileConverter.toResponse(saved);
    }

    /* ===================== util ====================== */

    private String buildPromptProfileText(AiProfileRequestDTO.AiProfileCreateRequest req) {
        return """
                 아래의 6가지 정보를 바탕으로, 다음 JSON을 생성해주세요:
                 반드시 형식을 지켜 예시 응답(Json)처럼 리턴해주세요.
                
                 1) profileName: 대화 상대에 어울리는 귀여운 이름. 형용사 뒤의 이름은 사물 또는 동물로 한정
                 (예: '행복한 빵빵이, 즐거운 몽몽이, 말랑한 구름이, 달콤한 마시멜로우, 보송한 리본, 엉뚱한 솔방울')
                 2) firstChat : 첫 인사말 작성
                 - 연령대와 성격에 맞는 표현 활용
                 - 이모티콘 적절히 사용 (profileName에 관련된 이모티콘 또는 인사말에 맞는 이모티콘 선택)
                 ٩(ˊᗜˋ*)و // ｡•̀ᴗ-)✧//◝(⑅•ᴗ•⑅)◜..°♡ //  (◍•ᴗ•◍) //  (ﾉ≧∀≦)ﾉ
                 - 함께할 내용 제안, 상대방 상황에 대한 공감/질문, 인사말
                 3) hashTag1, hashTag2: 2가지 해시태그
                 - 대화 상대의 연령대와 분위기 반영
                 4) feature1, feature2, feature3: 3가지 구체적인 행동이나 특징, 15자 이내
                 5) defaultSystem: 채팅 서비스에서 ai의 성격 정리
                 - 사용자가 원하는 특징을 지닌 프로필에 대한 설명을 적어줘.
                 - 이는 사용자의 대화를 이끌 AI의 시스템 메시지로 들어갈 거야.
                
                 질문과 답변:
                 Q1. 말투 스타일을 어떻게 할까? - %s
                 Q2. 응답 방식은 어떻게 할까? - %s
                 Q3. 감정 표현은 어느 정도로 할까? - %s
                 Q4. 질문 방식은 어떻게 할까? - %s
                 Q5. 대화의 개입 정도는? - %s
                 Q6. 유머 사용 여부는? - %s
                
                
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
                    "profileName": "달콤한 마시멜로우",
                    "firstChat": "오늘 뭐 재밌는 일 있었어? 같이 이야기 나눠보자!",
                    "hashTag1": "청춘의_하루",
                    "hashTag2": "웃음치료사",
                    "feature1": "오글거릴 정도로 진심인 칭찬",
                    "feature2": "잘 웃어주는 리액션",
                    "feature3": "틈새 유머 투척",
                    "defaultSystem": "이 AI의 프로필 이름은 '달콤한 마시멜로우'이다. 이 AI는 다음과 같은 특징을 갖는다.
                    첫째, 대화 상대에게 진심 어린 칭찬을 아끼지 않으며 언제나 긍정적인 에너지를 전달한다.
                    둘째, 대화 중 자주 웃어주어 밝고 유쾌한 분위기를 조성한다.
                    셋째, 대화 중 필요할 때마다 유머를 던져 상대방에게 즐거움을 준다."
                  }
                """.formatted(
                req.getQ1(), req.getQ2(), req.getQ3(),
                req.getQ4(), req.getQ5(), req.getQ6()
        );
    }

    private AiProfile parseLlmResponse(String json){
        try{
            int s=json.indexOf("{"); int e=json.lastIndexOf("}");
            if(s<0||e<0) throw new ErrorHandler(ErrorStatus.PARSING_FAIL);
            JsonNode n=objectMapper.readTree(json.substring(s,e+1));
            return AiProfile.builder()
                    .profileName(n.get("profileName").asText())
                    .firstChat(n.get("firstChat").asText())
                    .hashTag1(n.get("hashTag1").asText())
                    .hashTag2(n.get("hashTag2").asText())
                    .feature1(n.get("feature1").asText())
                    .feature2(n.get("feature2").asText())
                    .feature3(n.get("feature3").asText())
                    .promptText(n.get("defaultSystem").asText())
                    .active(true)
                    .build();
        }catch(Exception e){ throw new ErrorHandler(ErrorStatus.PARSING_FAIL); }
    }
}