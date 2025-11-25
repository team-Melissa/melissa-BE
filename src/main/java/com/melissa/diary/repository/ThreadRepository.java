package com.melissa.diary.repository;

import com.melissa.diary.domain.Thread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ThreadRepository extends JpaRepository<Thread,Long> {
    // [v1.3.0] aiProfileId 포함 쿼리 메서드
    @Query("SELECT DISTINCT t FROM Thread t LEFT JOIN FETCH t.dailyChatLogs " +
            "WHERE t.user.id = :userId AND t.aiProfile.id = :aiProfileId AND t.year = :year AND t.month = :month AND t.day = :day")
    Optional<Thread> findByUserIdAndAiProfileIdAndYearAndMonthAndDay(@Param("userId") Long userId,
                                                                      @Param("aiProfileId") Long aiProfileId,
                                                                      @Param("year") int year,
                                                                      @Param("month") int month,
                                                                      @Param("day") int day);

    @Query("SELECT DISTINCT t FROM Thread t LEFT JOIN FETCH t.dailyChatLogs " +
            "WHERE t.user.id = :userId AND t.aiProfile.id = :aiProfileId AND t.year = :year AND t.month = :month")
    List<Thread> findByUserIdAndAiProfileIdAndYearAndMonth(@Param("userId") Long userId,
                                                            @Param("aiProfileId") Long aiProfileId,
                                                            @Param("year") int year,
                                                            @Param("month") int month);

    boolean existsByUserIdAndAiProfileIdAndYearAndMonthAndDay(Long userId, Long aiProfileId, int year, int month, int day);

    // [Deprecated] 기존 메서드 - aiProfileId 없이 조회
    @Deprecated
    @Query("SELECT DISTINCT t FROM Thread t LEFT JOIN FETCH t.dailyChatLogs " +
            "WHERE t.user.id = :userId AND t.year = :year AND t.month = :month AND t.day = :day")
    Optional<Thread> findByUserIdAndYearAndMonthAndDay(@Param("userId") Long userId,
                                                       @Param("year") int year,
                                                       @Param("month") int month,
                                                       @Param("day") int day);

    @Deprecated
    @Query("SELECT DISTINCT t FROM Thread t LEFT JOIN FETCH t.dailyChatLogs " +
            "WHERE t.user.id = :userId AND t.year = :year AND t.month = :month")
    List<Thread> findByUserIdAndYearAndMonth(@Param("userId") Long userId,
                                             @Param("year") int year,
                                             @Param("month") int month);

    void deleteAllByUserId(Long userId);

    // userId 기준 가장 최근 createdAt 레코드 하나 조회
    /**
     * userId 기준으로 year -> month -> day 내림차순 정렬,
     * 가장 첫 번째(최신) Thread를 가져오는 메서드
     */
    Optional<Thread> findFirstByUserIdOrderByYearDescMonthDescDayDesc(Long userId);

    /**
     * 특정 날짜의 요약 내용이 있는 모든 Thread 조회 (메모리 업데이트용)
     */
    @Query("SELECT t FROM Thread t LEFT JOIN FETCH t.user " +
            "WHERE t.year = :year AND t.month = :month AND t.day = :day " +
            "AND t.summaryContent IS NOT NULL AND t.summaryContent != ''")
    List<Thread> findByYearAndMonthAndDayAndSummaryContentIsNotNull(@Param("year") int year,
                                                                   @Param("month") int month,
                                                                   @Param("day") int day);

}
