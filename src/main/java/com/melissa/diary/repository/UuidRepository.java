package com.melissa.diary.repository;

import com.melissa.diary.domain.Uuid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UuidRepository extends JpaRepository<Uuid,Long> {
}
