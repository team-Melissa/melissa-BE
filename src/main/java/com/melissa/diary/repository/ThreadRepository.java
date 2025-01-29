package com.melissa.diary.repository;

import com.melissa.diary.domain.Thread;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ThreadRepository extends JpaRepository<Thread,Long> {
    Optional<Thread> findByUserIdAndYearAndMonthAndDay(Long userId, int year, int month, int day);

    List<Thread> findByUserIdAndYearAndMonth(Long userId, int year, int month);
}
