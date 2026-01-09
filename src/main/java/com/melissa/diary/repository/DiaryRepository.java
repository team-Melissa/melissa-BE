package com.melissa.diary.repository;

import com.melissa.diary.domain.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {
    
    /**
     * 특정 날짜의 활성화된 일기 전부 조회 (최대 3개)
     * FETCH JOIN으로 Thread, AiProfile 한번에 로딩 (N+1 방지)
     */
    @Query("""
        SELECT d FROM Diary d
        INNER JOIN FETCH d.thread t
        INNER JOIN FETCH t.aiProfile ap
        WHERE d.user.id = :userId
        AND d.year = :year
        AND d.month = :month
        AND d.day = :day
        AND d.isActive = :isActive
        ORDER BY d.createdAt DESC
        """)
    List<Diary> findAllByUserIdAndYearAndMonthAndDayAndIsActiveOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month,
            @Param("day") int day,
            @Param("isActive") boolean isActive);
    
    /**
     * 특정 월의 활성화된 일기 전부 조회
     * FETCH JOIN으로 Thread, AiProfile 한번에 로딩 (N+1 방지)
     */
    @Query("""
        SELECT d FROM Diary d
        INNER JOIN FETCH d.thread t
        INNER JOIN FETCH t.aiProfile ap
        WHERE d.user.id = :userId
        AND d.year = :year
        AND d.month = :month
        AND d.isActive = :isActive
        ORDER BY d.day ASC, d.createdAt DESC
        """)
    List<Diary> findAllByUserIdAndYearAndMonthAndIsActiveOrderByDayAscCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month,
            @Param("isActive") boolean isActive);
    
    /**
     * 특정 날짜의 모든 사용자의 활성화된 일기 조회 (스케줄러용)
     */
    List<Diary> findAllByYearAndMonthAndDayAndIsActiveOrderByCreatedAtDesc(
            int year, int month, int day, boolean isActive);
    
    /**
     * 하루 최대 3개 제약 확인용
     */
    int countByUserIdAndYearAndMonthAndDayAndIsActive(
            Long userId, int year, int month, int day, boolean isActive);
    
    /**
     * Thread에 속한 모든 활성화된 일기 조회
     * FETCH JOIN으로 Thread, AiProfile 한번에 로딩 (N+1 방지)
     */
    @Query("""
        SELECT d FROM Diary d
        INNER JOIN FETCH d.thread t
        INNER JOIN FETCH t.aiProfile ap
        WHERE d.thread.id = :threadId
        AND d.isActive = :isActive
        ORDER BY d.createdAt DESC
        """)
    List<Diary> findAllByThreadIdAndIsActiveOrderByCreatedAtDesc(
            @Param("threadId") Long threadId,
            @Param("isActive") boolean isActive);
    
    /**
     * Diary 단건 조회 (Thread, AiProfile FETCH JOIN)
     */
    @Query("""
        SELECT d FROM Diary d
        INNER JOIN FETCH d.thread t
        INNER JOIN FETCH t.aiProfile ap
        WHERE d.id = :id
        """)
    Optional<Diary> findByIdWithThreadAndProfile(@Param("id") Long id);

    /**
     * 피드 전용 조회 (최신순, 커서 기반 페이지네이션)
     * - createdAt DESC, id DESC 정렬
     * - 커서 조건: (createdAt < cursorCreatedAt) OR (createdAt = cursorCreatedAt AND id < cursorDiaryId)
     * - FETCH JOIN으로 Thread, AiProfile 한번에 로딩 (N+1 방지)
     *
     * 주의: Pageable은 limit 용도로만 사용 (page=0 고정)
     */
    @Query("""
        SELECT d FROM Diary d
        INNER JOIN FETCH d.thread t
        INNER JOIN FETCH t.aiProfile ap
        WHERE d.user.id = :userId
          AND d.isActive = true
          AND (
            :cursorCreatedAt IS NULL
            OR d.createdAt < :cursorCreatedAt
            OR (d.createdAt = :cursorCreatedAt AND d.id < :cursorDiaryId)
          )
        ORDER BY d.createdAt DESC, d.id DESC
        """)
    List<Diary> findFeedPage(
            @Param("userId") Long userId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorDiaryId") Long cursorDiaryId,
            Pageable pageable);
    
    /**
     * 회원 탈퇴시 삭제
     */
    void deleteAllByUserId(Long userId);

    /**
     * 스트릭 계산용: 활성 일기 작성 날짜(중복 제거) 최신순 조회 (KST 기준 오늘까지)
     *
     * - year/month/day를 DATE로 변환하여 DISTINCT
     * - 반환 타입은 java.sql.Date로 두고 서비스에서 LocalDate로 변환
     */
    @Query(value = """
        SELECT DISTINCT STR_TO_DATE(
            CONCAT(d.year, '-', LPAD(d.month, 2, '0'), '-', LPAD(d.day, 2, '0')),
            '%Y-%m-%d'
        ) AS diary_date
        FROM diary d
        WHERE d.user_id = :userId
          AND d.is_active = 1
          AND STR_TO_DATE(
                CONCAT(d.year, '-', LPAD(d.month, 2, '0'), '-', LPAD(d.day, 2, '0')),
                '%Y-%m-%d'
              ) <= :endDate
        ORDER BY diary_date DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Date> findRecentActiveDiaryDatesDesc(
            @Param("userId") Long userId,
            @Param("endDate") Date endDate,
            @Param("limit") int limit);
}

