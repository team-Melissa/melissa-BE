package com.melissa.diary.repository;

import com.melissa.diary.domain.Diary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiaryRepository extends JpaRepository<Diary, Long> {
    
    /**
     * 특정 날짜의 활성화된 일기 전부 조회 (최대 3개)
     */
    List<Diary> findAllByUserIdAndYearAndMonthAndDayAndIsActiveOrderByCreatedAtDesc(
            Long userId, int year, int month, int day, boolean isActive);
    
    /**
     * 특정 월의 활성화된 일기 전부 조회
     */
    List<Diary> findAllByUserIdAndYearAndMonthAndIsActiveOrderByDayAscCreatedAtDesc(
            Long userId, int year, int month, boolean isActive);
    
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
     */
    List<Diary> findAllByThreadIdAndIsActiveOrderByCreatedAtDesc(Long threadId, boolean isActive);
    
    /**
     * 회원 탈퇴시 삭제
     */
    void deleteAllByUserId(Long userId);
}

