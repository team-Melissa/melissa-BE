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

    /**
     * threadId 기준으로 이미지를 생성하고 imageUrl 을 저장한다.
     * 메서드가 @Async 이므로 별도 스레드에서 실행된다.
     */
    @Async("asyncTaskExecutor")
    public void generateAndSaveImage(Long threadId) {
        try {
            Thread thread = threadRepository.findById(threadId)
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND));

            String prompt = buildImagePrompt(thread);
            String url    = imageGenerator.genProfileImage(prompt);

            updateThreadImage(thread, url);
        } catch (Exception e) {
            log.error("[Async-Image] threadId={} 처리 실패", threadId, e);
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
        return String.format("%s, %s, %s %s 지브리·디즈니 느낌 수채화 일러스트",
                mood,
                t.getSummaryTitle() == null ? "Untitled" : t.getSummaryTitle(),
                tag1, tag2);
    }
}
