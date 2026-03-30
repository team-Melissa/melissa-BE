package com.melissa.diary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.aws.s3.S3AssetUrlResolver;
import com.melissa.diary.domain.AiProfile;
import com.melissa.diary.domain.DailyChatLog;
import com.melissa.diary.domain.Diary;
import com.melissa.diary.domain.Thread;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.enums.DiaryImageStatus;
import com.melissa.diary.domain.enums.DiaryType;
import com.melissa.diary.domain.enums.Mood;
import com.melissa.diary.domain.enums.Role;
import com.melissa.diary.domain.enums.UsageCost;
import com.melissa.diary.event.DiaryImageEvent;
import com.melissa.diary.repository.AiProfileRepository;
import com.melissa.diary.repository.DiaryRepository;
import com.melissa.diary.repository.ThreadRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.web.dto.DiaryRequestDTO;
import com.melissa.diary.web.dto.DiaryResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * [v1.3.0] Diary 기반 일기 관리 서비스
 */
@Slf4j
@Service
public class DiaryService {
    
    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final AiProfileRepository aiProfileRepository;
    private final QuotaService quotaService;
    private final ApplicationEventPublisher publisher;
    private final ChatClient summaryClient;
    private final ChatClient hashtagClient;
    private final TransactionTemplate transactionTemplate;
    private final S3AssetUrlResolver s3AssetUrlResolver;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public DiaryService(DiaryRepository diaryRepository, 
                       UserRepository userRepository,
                       ThreadRepository threadRepository,
                       AiProfileRepository aiProfileRepository,
                       QuotaService quotaService,
                       ApplicationEventPublisher publisher,
                       @Qualifier("summaryClient") ChatClient summaryClient,
                       @Qualifier("hashtagClient") ChatClient hashtagClient,
                       TransactionTemplate transactionTemplate,
                       S3AssetUrlResolver s3AssetUrlResolver) {
        this.diaryRepository = diaryRepository;
        this.userRepository = userRepository;
        this.threadRepository = threadRepository;
        this.aiProfileRepository = aiProfileRepository;
        this.quotaService = quotaService;
        this.publisher = publisher;
        this.summaryClient = summaryClient;
        this.hashtagClient = hashtagClient;
        this.transactionTemplate = transactionTemplate;
        this.s3AssetUrlResolver = s3AssetUrlResolver;
    }
    
    /**
     * 수동 일기 작성
     * - Thread가 없으면 자동 생성
     * - 하루 최대 3개 제한
     * - 이미지는 DALL-E로 생성 (비동기)
     */
    @Transactional
    public DiaryResponseDTO.DiaryResponse createManualDiary(Long userId, 
                                                           DiaryRequestDTO.ManualDiaryCreateRequest request) {
        // 유저 검증 및 쿼터 체크
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
        quotaService.checkAndConsume(user, UsageCost.SUMMARY);
        
        // AI 프로필 검증
        AiProfile aiProfile = aiProfileRepository.findById(request.getAiProfileId())
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));
        
        // 날짜 유효성 검증
        if (!isValidDate(request.getYear(), request.getMonth(), request.getDay())) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_INVALID_DATE);
        }
        
        // 하루 최대 3개 제한 확인
        int diaryCount = diaryRepository.countByUserIdAndYearAndMonthAndDayAndIsActive(
                userId, request.getYear(), request.getMonth(), request.getDay(), true);
        
        if (diaryCount >= 3) {
            throw new ErrorHandler(ErrorStatus.DIARY_MAX_COUNT_EXCEEDED);
        }
        
        // Thread 조회 또는 생성
        Thread thread = threadRepository.findByUserIdAndAiProfileIdAndYearAndMonthAndDay(
                userId, request.getAiProfileId(), 
                request.getYear(), request.getMonth(), request.getDay())
                .orElseGet(() -> createNewThread(user, aiProfile, 
                        request.getYear(), request.getMonth(), request.getDay()));
        
        // Mood 변환
        Mood mood = null;
        if (request.getMood() != null) {
            try {
                mood = Mood.valueOf(request.getMood().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("[Diary] 유효하지 않은 mood 값: {}", request.getMood());
            }
        }
        
        // LLM으로 해시태그 자동 생성
        HashtagData hashtagData;
        try {
            String hashtagPrompt = buildHashtagPrompt(request.getTitle(), request.getContent());
            String llmHashtagResponse = hashtagClient.prompt().user(hashtagPrompt).call().content();
            hashtagData = parseHashtagResponse(llmHashtagResponse);
        } catch (Exception e) {
            log.error("[Diary] LLM 해시태그 생성 실패, 기본값 사용. userId={}", userId, e);
            hashtagData = new HashtagData("일기", "기록");  // fallback
        }
        
        // 일기 생성
        Diary diary = Diary.builder()
                .user(user)
                .thread(thread)
                .year(request.getYear())
                .month(request.getMonth())
                .day(request.getDay())
                .title(request.getTitle())
                .content(request.getContent())
                .mood(mood)
                .type(DiaryType.MANUAL)  // 수동 작성
                .hashtag1(hashtagData.getHashTag1())  // LLM 생성
                .hashtag2(hashtagData.getHashTag2())  // LLM 생성
                .imageUrl(null)  // 초기에는 null, 비동기로 생성
                .imageStatus(Boolean.TRUE.equals(request.getGenerateImage()) ? DiaryImageStatus.PENDING : DiaryImageStatus.NONE)
                .version(1)
                .isActive(true)
                .build();
        
        diary = diaryRepository.save(diary);
        
        // 이미지 생성 요청 (비동기)
        if (Boolean.TRUE.equals(request.getGenerateImage())) {
            publisher.publishEvent(new DiaryImageEvent(diary.getId()));
        }
        
        log.info("[Diary] 수동 일기 작성 완료. userId={}, diaryId={}, year={}-{}-{}", 
                userId, diary.getId(), request.getYear(), request.getMonth(), request.getDay());
        
        return buildDiaryResponse(diary);
    }
    
    /**
     * 채팅 기반 일기 생성
     * - Thread의 채팅 로그를 LLM으로 요약
     * - 제목, 내용, 해시태그 자동 생성
     * - 이미지는 DALL-E로 생성 (비동기)
     */
    @Transactional
    public DiaryResponseDTO.DiaryResponse createChatDiary(Long userId, 
                                                         DiaryRequestDTO.ChatDiaryCreateRequest request) {
        // 유저 검증 및 쿼터 체크
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
        quotaService.checkAndConsume(user, UsageCost.SUMMARY);
        
        // Thread 조회 (채팅 로그가 있어야 함)
        Thread thread = threadRepository.findByUserIdAndAiProfileIdAndYearAndMonthAndDay(
                userId, request.getAiProfileId(), 
                request.getYear(), request.getMonth(), request.getDay())
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND));
        
        // 채팅 로그 조회 (USER role만)
        List<DailyChatLog> chatLogs = thread.getDailyChatLogs().stream()
                .filter(log -> Role.USER.equals(log.getRole()))
                .collect(Collectors.toList());
        
        if (chatLogs.size() <= 2) {
            throw new ErrorHandler(ErrorStatus.CHAT_NOT_FOUND);
        }
        
        // 하루 최대 3개 제한 확인
        int diaryCount = diaryRepository.countByUserIdAndYearAndMonthAndDayAndIsActive(
                userId, request.getYear(), request.getMonth(), request.getDay(), true);
        
        if (diaryCount >= 3) {
            throw new ErrorHandler(ErrorStatus.DIARY_MAX_COUNT_EXCEEDED);
        }
        
        // LLM으로 채팅 로그 요약
        DiaryData diaryData;
        try {
            String chatLogsPrompt = buildChatLogsPrompt(thread.getDailyChatLogs());
            String summaryPrompt = buildSummaryPrompt(chatLogsPrompt);
            String llmResponse = summaryClient.prompt().user(summaryPrompt).call().content();
            diaryData = parseLLMResponse(llmResponse);
        } catch (Exception e) {
            log.error("[Diary] LLM 요약 실패, fallback 사용. userId={}, threadId={}", 
                    userId, thread.getId(), e);
            // Fallback: 채팅 내용을 간단히 합쳐서 일기로 생성
            String fallbackContent = thread.getDailyChatLogs().stream()
                    .filter(log -> Role.USER.equals(log.getRole()))
                    .map(log -> log.getContent())
                    .collect(Collectors.joining(" "));
            diaryData = new DiaryData(
                    "오늘의 대화",  // title
                    fallbackContent.substring(0, Math.min(fallbackContent.length(), 500)),  // content (최대 500자)
                    null,  // mood
                    "대화", "일기"  // hashtags
            );
        }
        
        // 일기 생성
        Diary diary = Diary.builder()
                .user(user)
                .thread(thread)
                .year(request.getYear())
                .month(request.getMonth())
                .day(request.getDay())
                .title(diaryData.getTitle())
                .content(diaryData.getStory())
                .mood(diaryData.getMood())
                .type(DiaryType.CHAT_BASED)  // 채팅 기반 자동 생성
                .hashtag1(diaryData.getHashTag1())
                .hashtag2(diaryData.getHashTag2())
                .imageUrl(null)  // 초기에는 null, 비동기로 생성
                .imageStatus(Boolean.TRUE.equals(request.getGenerateImage()) ? DiaryImageStatus.PENDING : DiaryImageStatus.NONE)
                .version(1)
                .isActive(true)
                .build();
        
        diary = diaryRepository.save(diary);
        
        // 이미지 생성 요청 (비동기)
        if (Boolean.TRUE.equals(request.getGenerateImage())) {
            publisher.publishEvent(new DiaryImageEvent(diary.getId()));
        }
        
        log.info("[Diary] 채팅 기반 일기 생성 완료. userId={}, diaryId={}, year={}-{}-{}", 
                userId, diary.getId(), request.getYear(), request.getMonth(), request.getDay());
        
        return buildDiaryResponse(diary);
    }
    
    /**
     * Thread 생성 (일기 작성 시 필요한 경우)
     */
    private Thread createNewThread(User user, AiProfile aiProfile, int year, int month, int day) {
        Thread newThread = Thread.builder()
                .user(user)
                .aiProfile(aiProfile)
                .year(year)
                .month(month)
                .day(day)
                .build();
        
        try {
            return threadRepository.save(newThread);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 동시성 문제로 이미 생성된 경우 조회해서 반환
            return threadRepository.findByUserIdAndAiProfileIdAndYearAndMonthAndDay(
                    user.getId(), aiProfile.getId(), year, month, day)
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.THREAD_ALREADY_ENROLL));
        }
    }
    
    /**
     * 일기 수정
     * - 본인의 일기만 수정 가능
     * - null이 아닌 필드만 업데이트
     */
    @Transactional
    public DiaryResponseDTO.DiaryResponse updateDiary(Long userId, Long diaryId, 
                                                     DiaryRequestDTO.DiaryUpdateRequest request) {
        // 유저 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
        
        // 일기 조회 (Thread, AiProfile FETCH JOIN)
        Diary diary = diaryRepository.findByIdWithThreadAndProfile(diaryId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.DIARY_NOT_FOUND));
        
        // 권한 검증: 본인의 일기인지 확인
        if (!diary.getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.DIARY_FORBIDDEN);
        }
        
        // 삭제된 일기는 수정 불가
        if (!diary.isActive()) {
            throw new ErrorHandler(ErrorStatus.DIARY_ALREADY_DELETED);
        }
        
        // null이 아닌 필드만 업데이트
        if (request.getTitle() != null) {
            diary.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            diary.setContent(request.getContent());
        }
        if (request.getMood() != null) {
            try {
                diary.setMood(Mood.valueOf(request.getMood().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("[Diary] 유효하지 않은 mood 값: {}", request.getMood());
            }
        }
        if (request.getHashtag1() != null) {
            diary.setHashtag1(request.getHashtag1());
        }
        if (request.getHashtag2() != null) {
            diary.setHashtag2(request.getHashtag2());
        }
        
        // 버전 증가
        diary.setVersion(diary.getVersion() + 1);
        
        diary = diaryRepository.save(diary);
        
        // 이미지 재생성 요청 (선택적)
        if (Boolean.TRUE.equals(request.getGenerateImage())) {
            quotaService.checkAndConsume(user, UsageCost.SUMMARY);
            diary.requestImageGeneration();
            diaryRepository.save(diary);
            publisher.publishEvent(new DiaryImageEvent(diary.getId()));
        }
        
        log.info("[Diary] 일기 수정 완료. userId={}, diaryId={}, version={}", 
                userId, diaryId, diary.getVersion());
        
        return buildDiaryResponse(diary);
    }
    
    /**
     * 일기 삭제 (소프트 삭제)
     * - isActive = false 처리
     * - 본인의 일기만 삭제 가능
     */
    @Transactional
    public DiaryResponseDTO.DiaryDeleteResponse deleteDiary(Long userId, Long diaryId) {
        // 유저 검증
        userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
        
        // 일기 조회 (Thread, AiProfile FETCH JOIN) - 삭제 시에는 불필요하지만 일관성 유지
        Diary diary = diaryRepository.findByIdWithThreadAndProfile(diaryId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.DIARY_NOT_FOUND));
        
        // 권한 검증: 본인의 일기인지 확인
        if (!diary.getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.DIARY_FORBIDDEN);
        }
        
        // 이미 삭제된 일기인지 확인
        if (!diary.isActive()) {
            throw new ErrorHandler(ErrorStatus.DIARY_ALREADY_DELETED);
        }
        
        // 소프트 삭제
        diary.deactivate();
        diaryRepository.save(diary);
        
        log.info("[Diary] 일기 삭제 완료. userId={}, diaryId={}", userId, diaryId);
        
        return DiaryResponseDTO.DiaryDeleteResponse.builder()
                .diaryId(diaryId)
                .message("일기가 삭제되었습니다.")
                .build();
    }
    
    /**
     * Diary -> DiaryResponse 변환
     */
    private DiaryResponseDTO.DiaryResponse buildDiaryResponse(Diary diary) {
        return DiaryResponseDTO.DiaryResponse.builder()
                .diaryId(diary.getId())
                .threadId(diary.getThread().getId())
                .aiProfileId(diary.getThread().getAiProfile().getId())
                .year(diary.getYear())
                .month(diary.getMonth())
                .day(diary.getDay())
                .title(diary.getTitle())
                .content(diary.getContent())
                .mood(diary.getMood() != null ? diary.getMood().name() : null)
                .type(diary.getType().name())
                .hashtag1(diary.getHashtag1())
                .hashtag2(diary.getHashtag2())
                .imageUrl(s3AssetUrlResolver.resolve(diary.getImageUrl()))
                .imageStatus(diary.getImageStatus() != null ? diary.getImageStatus().name() : null)
                .version(diary.getVersion())
                .createdAt(diary.getCreatedAt())
                .build();
    }
    
    /**
     * 날짜 유효성 검증
     */
    public DiaryResponseDTO.DiaryResponse createManualDiarySeparated(Long userId,
                                                                     DiaryRequestDTO.ManualDiaryCreateRequest request) {
        if (!isValidDate(request.getYear(), request.getMonth(), request.getDay())) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_INVALID_DATE);
        }

        prepareManualDiaryCreation(userId, request);
        HashtagData hashtagData = generateHashtagsSafe(request.getTitle(), request.getContent(), userId);
        return persistManualDiary(userId, request, hashtagData);
    }

    public DiaryResponseDTO.DiaryResponse createChatDiarySeparated(Long userId,
                                                                   DiaryRequestDTO.ChatDiaryCreateRequest request) {
        ChatDiaryPreparation preparation = prepareChatDiaryCreation(userId, request);
        DiaryData diaryData = summarizeDiaryDataSafe(userId, preparation);
        return persistChatDiary(userId, request, diaryData);
    }

    public DiaryResponseDTO.DiaryResponse updateDiarySeparated(Long userId, Long diaryId,
                                                               DiaryRequestDTO.DiaryUpdateRequest request) {
        DiaryResponseDTO.DiaryResponse response = applyDiaryUpdate(userId, diaryId, request);

        if (Boolean.TRUE.equals(request.getGenerateImage())) {
            response = requestDiaryImageGeneration(userId, diaryId);
        }

        return response;
    }

    private void prepareManualDiaryCreation(Long userId, DiaryRequestDTO.ManualDiaryCreateRequest request) {
        transactionTemplate.executeWithoutResult(status -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

            quotaService.checkAndConsume(user, UsageCost.SUMMARY);
            aiProfileRepository.findById(request.getAiProfileId())
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));

            validateDiaryCapacity(userId, request.getYear(), request.getMonth(), request.getDay());
        });
    }

    private ChatDiaryPreparation prepareChatDiaryCreation(Long userId,
                                                          DiaryRequestDTO.ChatDiaryCreateRequest request) {
        ChatDiaryPreparation preparation = transactionTemplate.execute(status -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

            quotaService.checkAndConsume(user, UsageCost.SUMMARY);

            Thread thread = threadRepository.findByUserIdAndAiProfileIdAndYearAndMonthAndDay(
                            userId, request.getAiProfileId(), request.getYear(), request.getMonth(), request.getDay())
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND));

            List<ChatLogSnapshot> chatLogs = thread.getDailyChatLogs().stream()
                    .map(log -> new ChatLogSnapshot(log.getRole(), log.getContent()))
                    .toList();

            long userChatCount = chatLogs.stream()
                    .filter(log -> Role.USER.equals(log.getRole()))
                    .count();
            if (userChatCount <= 2) {
                throw new ErrorHandler(ErrorStatus.CHAT_NOT_FOUND);
            }

            validateDiaryCapacity(userId, request.getYear(), request.getMonth(), request.getDay());
            return new ChatDiaryPreparation(thread.getId(), chatLogs);
        });

        if (preparation == null) {
            throw new IllegalStateException("Failed to prepare chat diary creation");
        }
        return preparation;
    }

    private void validateDiaryCapacity(Long userId, int year, int month, int day) {
        int diaryCount = diaryRepository.countByUserIdAndYearAndMonthAndDayAndIsActive(
                userId, year, month, day, true);
        if (diaryCount >= 3) {
            throw new ErrorHandler(ErrorStatus.DIARY_MAX_COUNT_EXCEEDED);
        }
    }

    private HashtagData generateHashtagsSafe(String title, String content, Long userId) {
        try {
            String hashtagPrompt = buildHashtagPrompt(title, content);
            String llmHashtagResponse = hashtagClient.prompt().user(hashtagPrompt).call().content();
            return parseHashtagResponse(llmHashtagResponse);
        } catch (Exception e) {
            log.error("[Diary] hashtag generation failed. fallback used. userId={}", userId, e);
            return new HashtagData("diary", "record");
        }
    }

    private DiaryData summarizeDiaryDataSafe(Long userId, ChatDiaryPreparation preparation) {
        try {
            String chatLogsPrompt = buildChatLogsPromptFromSnapshots(preparation.getChatLogs());
            String summaryPrompt = buildSummaryPrompt(chatLogsPrompt);
            String llmResponse = summaryClient.prompt().user(summaryPrompt).call().content();
            return parseLLMResponse(llmResponse);
        } catch (Exception e) {
            log.error("[Diary] summary generation failed. fallback used. userId={}, threadId={}",
                    userId, preparation.getThreadId(), e);

            String fallbackContent = preparation.getChatLogs().stream()
                    .filter(log -> Role.USER.equals(log.getRole()))
                    .map(ChatLogSnapshot::getContent)
                    .collect(Collectors.joining(" "));

            return new DiaryData(
                    "Untitled diary",
                    fallbackContent.substring(0, Math.min(fallbackContent.length(), 500)),
                    null,
                    "daily",
                    "record"
            );
        }
    }

    private DiaryResponseDTO.DiaryResponse persistManualDiary(Long userId,
                                                              DiaryRequestDTO.ManualDiaryCreateRequest request,
                                                              HashtagData hashtagData) {
        DiaryResponseDTO.DiaryResponse response = transactionTemplate.execute(status -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
            AiProfile aiProfile = aiProfileRepository.findById(request.getAiProfileId())
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));

            validateDiaryCapacity(userId, request.getYear(), request.getMonth(), request.getDay());

            Thread thread = threadRepository.findByUserIdAndAiProfileIdAndYearAndMonthAndDay(
                            userId, request.getAiProfileId(), request.getYear(), request.getMonth(), request.getDay())
                    .orElseGet(() -> createNewThread(user, aiProfile,
                            request.getYear(), request.getMonth(), request.getDay()));

            Diary diary = Diary.builder()
                    .user(user)
                    .thread(thread)
                    .year(request.getYear())
                    .month(request.getMonth())
                    .day(request.getDay())
                    .title(request.getTitle())
                    .content(request.getContent())
                    .mood(parseMoodOrNull(request.getMood()))
                    .type(DiaryType.MANUAL)
                    .hashtag1(hashtagData.getHashTag1())
                    .hashtag2(hashtagData.getHashTag2())
                    .imageUrl(null)
                    .imageStatus(Boolean.TRUE.equals(request.getGenerateImage()) ? DiaryImageStatus.PENDING : DiaryImageStatus.NONE)
                    .version(1)
                    .isActive(true)
                    .build();

            diary = diaryRepository.save(diary);

            if (Boolean.TRUE.equals(request.getGenerateImage())) {
                publisher.publishEvent(new DiaryImageEvent(diary.getId()));
            }

            return buildDiaryResponse(diary);
        });

        if (response == null) {
            throw new IllegalStateException("Failed to persist manual diary");
        }
        return response;
    }

    private DiaryResponseDTO.DiaryResponse persistChatDiary(Long userId,
                                                            DiaryRequestDTO.ChatDiaryCreateRequest request,
                                                            DiaryData diaryData) {
        DiaryResponseDTO.DiaryResponse response = transactionTemplate.execute(status -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
            Thread thread = threadRepository.findByUserIdAndAiProfileIdAndYearAndMonthAndDay(
                            userId, request.getAiProfileId(), request.getYear(), request.getMonth(), request.getDay())
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND));

            validateDiaryCapacity(userId, request.getYear(), request.getMonth(), request.getDay());

            Diary diary = Diary.builder()
                    .user(user)
                    .thread(thread)
                    .year(request.getYear())
                    .month(request.getMonth())
                    .day(request.getDay())
                    .title(diaryData.getTitle())
                    .content(diaryData.getStory())
                    .mood(diaryData.getMood())
                    .type(DiaryType.CHAT_BASED)
                    .hashtag1(diaryData.getHashTag1())
                    .hashtag2(diaryData.getHashTag2())
                    .imageUrl(null)
                    .imageStatus(Boolean.TRUE.equals(request.getGenerateImage()) ? DiaryImageStatus.PENDING : DiaryImageStatus.NONE)
                    .version(1)
                    .isActive(true)
                    .build();

            diary = diaryRepository.save(diary);

            if (Boolean.TRUE.equals(request.getGenerateImage())) {
                publisher.publishEvent(new DiaryImageEvent(diary.getId()));
            }

            return buildDiaryResponse(diary);
        });

        if (response == null) {
            throw new IllegalStateException("Failed to persist chat diary");
        }
        return response;
    }

    private DiaryResponseDTO.DiaryResponse applyDiaryUpdate(Long userId, Long diaryId,
                                                            DiaryRequestDTO.DiaryUpdateRequest request) {
        DiaryResponseDTO.DiaryResponse response = transactionTemplate.execute(status -> {
            userRepository.findById(userId)
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

            Diary diary = diaryRepository.findByIdWithThreadAndProfile(diaryId)
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.DIARY_NOT_FOUND));

            validateDiaryOwnership(userId, diary);

            if (request.getTitle() != null) {
                diary.setTitle(request.getTitle());
            }
            if (request.getContent() != null) {
                diary.setContent(request.getContent());
            }
            if (request.getMood() != null) {
                diary.setMood(parseMoodOrNull(request.getMood()));
            }
            if (request.getHashtag1() != null) {
                diary.setHashtag1(request.getHashtag1());
            }
            if (request.getHashtag2() != null) {
                diary.setHashtag2(request.getHashtag2());
            }

            diary.setVersion(diary.getVersion() + 1);
            diary = diaryRepository.save(diary);
            return buildDiaryResponse(diary);
        });

        if (response == null) {
            throw new IllegalStateException("Failed to update diary");
        }
        return response;
    }

    private DiaryResponseDTO.DiaryResponse requestDiaryImageGeneration(Long userId, Long diaryId) {
        DiaryResponseDTO.DiaryResponse response = transactionTemplate.execute(status -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
            Diary diary = diaryRepository.findByIdWithThreadAndProfile(diaryId)
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.DIARY_NOT_FOUND));

            validateDiaryOwnership(userId, diary);

            quotaService.checkAndConsume(user, UsageCost.SUMMARY);
            diary.requestImageGeneration();
            diary = diaryRepository.save(diary);
            publisher.publishEvent(new DiaryImageEvent(diary.getId()));
            return buildDiaryResponse(diary);
        });

        if (response == null) {
            throw new IllegalStateException("Failed to request diary image generation");
        }
        return response;
    }

    private Mood parseMoodOrNull(String moodValue) {
        if (moodValue == null) {
            return null;
        }

        try {
            return Mood.valueOf(moodValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("[Diary] ?좏슚?섏? ?딆? mood 媛? {}", moodValue);
            return null;
        }
    }

    private void validateDiaryOwnership(Long userId, Diary diary) {
        if (!diary.getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.DIARY_FORBIDDEN);
        }
        if (!diary.isActive()) {
            throw new ErrorHandler(ErrorStatus.DIARY_ALREADY_DELETED);
        }
    }

    private boolean isValidDate(int year, int month, int day) {
        if (month < 1 || month > 12) return false;
        if (day < 1 || day > 31) return false;
        
        // 2월 처리
        if (month == 2) {
            boolean isLeapYear = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
            return day <= (isLeapYear ? 29 : 28);
        }
        
        // 4, 6, 9, 11월은 30일까지
        if (month == 4 || month == 6 || month == 9 || month == 11) {
            return day <= 30;
        }
        
        return true;
    }

    private String buildChatLogsPromptFromSnapshots(List<ChatLogSnapshot> logs) {
        return logs.stream()
                .map(log -> {
                    if (log.getRole() == Role.USER) {
                        return "[User] " + log.getContent();
                    } else {
                        return "[Assistant] " + log.getContent();
                    }
                })
                .collect(Collectors.joining("\n"));
    }
    
    /**
     * 채팅 로그를 프롬프트용 문자열로 변환
     */
    private String buildChatLogsPrompt(List<DailyChatLog> logs) {
        return logs.stream()
                .map(log -> {
                    if (log.getRole() == Role.USER) {
                        return "[User] " + log.getContent();
                    } else {
                        return "[Assistant] " + log.getContent();
                    }
                })
                .collect(Collectors.joining("\n"));
    }
    
    /**
     * LLM 요약 프롬프트 생성 (user 프롬프트)
     * 시스템 프롬프트는 AiConfig의 summaryClient에 정의됨
     */
    private String buildSummaryPrompt(String chatLogs) {
        return """
                오늘의 채팅 로그입니다:
                
                %s
                
                위 대화를 일기 형식으로 요약해주세요.
                """.formatted(chatLogs);
    }
    
    /**
     * LLM 응답(JSON) 파싱
     */
    private DiaryData parseLLMResponse(String llmResponse) {
        try {
            int startIndex = llmResponse.indexOf("{");
            int endIndex = llmResponse.lastIndexOf("}");
            if (startIndex == -1 || endIndex == -1) {
                throw new ErrorHandler(ErrorStatus.CALENDAR_PROCESSING_FAILED);
            }
            
            String jsonContent = llmResponse.substring(startIndex, endIndex + 1);
            JsonNode node = objectMapper.readTree(jsonContent);
            
            String title = node.has("title") ? node.get("title").asText() : null;
            String moodStr = node.has("mood") ? node.get("mood").asText() : null;
            String story = node.has("story") ? node.get("story").asText() : null;
            String hashTag1 = node.has("hashTag1") ? node.get("hashTag1").asText() : null;
            String hashTag2 = node.has("hashTag2") ? node.get("hashTag2").asText() : null;
            
            // # 기호 제거 (안전장치)
            if (hashTag1 != null) {
                hashTag1 = hashTag1.replace("#", "").trim();
            }
            if (hashTag2 != null) {
                hashTag2 = hashTag2.replace("#", "").trim();
            }
            
            // Mood enum 변환 (기본값 HAPPY)
            Mood mood = Mood.HAPPY;
            if (moodStr != null) {
                try {
                    mood = Mood.valueOf(moodStr.toUpperCase().trim());
                } catch (IllegalArgumentException e) {
                    log.warn("[Diary] 유효하지 않은 mood 값: {}, 기본값 HAPPY 사용", moodStr);
                    mood = Mood.HAPPY;
                }
            }
            
            return new DiaryData(title, story, mood, hashTag1, hashTag2);
            
        } catch (IOException e) {
            log.error("[Diary] LLM 응답 파싱 실패", e);
            throw new ErrorHandler(ErrorStatus.CALENDAR_PROCESSING_FAILED);
        }
    }
    
    /**
     * 해시태그 생성 프롬프트 (Manual 일기용)
     * 시스템 프롬프트는 AiConfig의 hashtagClient에 정의됨
     */
    private String buildHashtagPrompt(String title, String content) {
        StringBuilder prompt = new StringBuilder();
        
        if (title != null && !title.isBlank()) {
            prompt.append("제목: ").append(title).append("\n");
        }
        prompt.append("내용: ").append(content);
        
        return prompt.toString();
    }
    
    /**
     * 해시태그 LLM 응답 파싱
     */
    private HashtagData parseHashtagResponse(String llmResponse) {
        try {
            int startIndex = llmResponse.indexOf("{");
            int endIndex = llmResponse.lastIndexOf("}");
            if (startIndex == -1 || endIndex == -1) {
                log.warn("[Diary] 해시태그 파싱 실패, 기본값 사용");
                return new HashtagData("일상", "기록");
            }
            
            String jsonContent = llmResponse.substring(startIndex, endIndex + 1);
            JsonNode node = objectMapper.readTree(jsonContent);
            
            String hashTag1 = node.has("hashTag1") ? node.get("hashTag1").asText() : "일상";
            String hashTag2 = node.has("hashTag2") ? node.get("hashTag2").asText() : "기록";
            
            // # 기호 제거 (안전장치)
            hashTag1 = hashTag1.replace("#", "").trim();
            hashTag2 = hashTag2.replace("#", "").trim();
            
            return new HashtagData(hashTag1, hashTag2);
            
        } catch (IOException e) {
            log.warn("[Diary] 해시태그 파싱 실패, 기본값 사용. error={}", e.getMessage());
            return new HashtagData("일상", "기록");
        }
    }
    
    /**
     * LLM 응답 파싱 결과를 담는 내부 클래스 (Chat 일기용)
     */
    @lombok.Getter
    @lombok.AllArgsConstructor
    private static class DiaryData {
        private final String title;
        private final String story;
        private final Mood mood;
        private final String hashTag1;
        private final String hashTag2;
    }
    
    /**
     * 해시태그 파싱 결과를 담는 내부 클래스 (Manual 일기용)
     */
    @lombok.Getter
    @lombok.AllArgsConstructor
    private static class HashtagData {
        private final String hashTag1;
        private final String hashTag2;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    private static class ChatDiaryPreparation {
        private final Long threadId;
        private final List<ChatLogSnapshot> chatLogs;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    private static class ChatLogSnapshot {
        private final Role role;
        private final String content;
    }
}

