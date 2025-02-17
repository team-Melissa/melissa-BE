package com.melissa.diary.repository;

import com.melissa.diary.domain.Thread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ThreadRepository extends JpaRepository<Thread,Long> {
    @Query("SELECT DISTINCT t FROM Thread t LEFT JOIN FETCH t.dailyChatLogs " +
            "WHERE t.user.id = :userId AND t.year = :year AND t.month = :month AND t.day = :day")
    Optional<Thread> findByUserIdAndYearAndMonthAndDay(@Param("userId") Long userId,
                                                       @Param("year") int year,
                                                       @Param("month") int month,
                                                       @Param("day") int day);

    @Query("SELECT DISTINCT t FROM Thread t LEFT JOIN FETCH t.dailyChatLogs " +
            "WHERE t.user.id = :userId AND t.year = :year AND t.month = :month")
    List<Thread> findByUserIdAndYearAndMonth(@Param("userId") Long userId,
                                             @Param("year") int year,
                                             @Param("month") int month);

    void deleteAllByUserId(Long userId);

}
