package com.melissa.diary.scheduler;

import com.melissa.diary.domain.Thread;
import com.melissa.diary.repository.ThreadRepository;
import com.melissa.diary.service.UserMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemoryUpdateScheduler {
    
    private final ThreadRepository threadRepository;
    private final UserMemoryService userMemoryService;
    
    /**
     * 매일 새벽 3시에 전날 Thread들의 요약을 기반으로 사용자 메모리 업데이트
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @Transactional
    public void updateUserMemoriesFromYesterdayThreads() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("[MemoryScheduler] 메모리 업데이트 스케줄러 시작. 대상 날짜: {}", yesterday);
        
        try {
            // 어제 날짜의 모든 Thread 조회 (요약 내용이 있는 것만)
            List<Thread> threadsWithSummary = threadRepository.findByYearAndMonthAndDayAndSummaryContentIsNotNull(
                    yesterday.getYear(), 
                    yesterday.getMonthValue(), 
                    yesterday.getDayOfMonth()
            );
            
            log.info("[MemoryScheduler] 처리 대상 Thread 개수: {}", threadsWithSummary.size());
            
            int successCount = 0;
            int errorCount = 0;
            
            for (Thread thread : threadsWithSummary) {
                try {
                    userMemoryService.updateUserMemoryFromThread(thread.getUser().getId(), thread);
                    successCount++;
                } catch (Exception e) {
                    log.error("[MemoryScheduler] 메모리 업데이트 실패. userId={}, threadId={}", 
                            thread.getUser().getId(), thread.getId(), e);
                    errorCount++;
                }
            }
            
            log.info("[MemoryScheduler] 메모리 업데이트 완료. 성공: {}, 실패: {}", successCount, errorCount);
            
        } catch (Exception e) {
            log.error("[MemoryScheduler] 메모리 업데이트 스케줄러 실행 중 오류 발생", e);
        }
    }
    
    /**
     * 매주 일요일 새벽 4시에 메모리 통계 로그 출력
     */
    @Scheduled(cron = "0 0 4 * * SUN", zone = "Asia/Seoul")
    public void logMemoryStatistics() {
        try {
            // 통계 정보 수집 및 로그 출력
            log.info("[MemoryScheduler] === 주간 메모리 통계 ===");
            log.info("[MemoryScheduler] 지난 주 업데이트된 메모리 수: {개} (예상)", 
                    "통계 구현 필요"); // TODO: 실제 통계 구현
            log.info("[MemoryScheduler] === 통계 종료 ===");
            
        } catch (Exception e) {
            log.error("[MemoryScheduler] 메모리 통계 로그 출력 중 오류 발생", e);
        }
    }
}
