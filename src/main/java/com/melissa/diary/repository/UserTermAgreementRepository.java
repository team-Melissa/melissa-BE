package com.melissa.diary.repository;

import com.melissa.diary.domain.UserTermAgreement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserTermAgreementRepository extends JpaRepository<UserTermAgreement, Long> {

    @EntityGraph(attributePaths = {"term", "termVersion"})
    List<UserTermAgreement> findByUserIdAndTermIdIn(Long userId, List<Long> termIds);
}
