package com.melissa.diary.service;

import com.melissa.diary.ai.ImageGenerator;
import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.Thread;
import com.melissa.diary.domain.enums.Mood;
import com.melissa.diary.repository.ThreadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이미지 생성-업로드-DB 반영을 전담하는 비동기 서비스이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThreadImageService {

    private final ThreadRepository threadRepository;
    private final ImageGenerator    imageGenerator;
    private final ThreadImagePromptRefinerService refiner;
    private static final String DEFAULT_IMG =
            "https://melissa-s3.s3.ap-northeast-2.amazonaws.com/default.png";

    /**
     * threadId 기준으로 이미지를 생성하고 imageUrl 을 저장한다.
     * 메서드가 @Async 이므로 별도 스레드에서 실행된다.
     */
    public void generateAndSaveImage(Long threadId) {
        Thread t = threadRepository.findById(threadId).orElse(null);
        if (t == null) {
            log.error("[Async-Image] threadId={} not found", threadId);
            return;
        }

        String rawPrompt   = buildImagePrompt(t);          // 1차(raw)
        String finalPrompt = refiner.refine(rawPrompt);    // 2차(LLM)

        try {
            String url = imageGenerator.genProfileImage(finalPrompt);
            updateThreadImage(t, url);                            // 정상 저장
        } catch (Exception e) {
            log.error("[Async-Image] threadId={} 처리 실패", threadId, e);
            updateThreadImage(t, DEFAULT_IMG);                    // 실패 시 기본 이미지
        }
    }

    /** imageUrl 컬럼만 갱신 담당 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateThreadImage(Thread thread, String url) {
        thread.setImageUrl(url);
        threadRepository.save(thread);
    }

    private String buildImagePrompt(Thread t) {
        String mood = t.getMood() == null ? Mood.HAPPY.name() : t.getMood().name();
        String tag1 = t.getHashtag1() == null ? "" : t.getHashtag1();
        String tag2 = t.getHashtag2() == null ? "" : t.getHashtag2();
        return String.format("%s, %s, %s %s 수채화 일러스트",
                mood,
                t.getSummaryTitle() == null ? "Untitled" : t.getSummaryTitle(),
                tag1, tag2);
    }
}
