package com.melissa.diary.repository;

import com.melissa.diary.domain.UserTermAgreement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserTermAgreementRepository extends JpaRepository<UserTermAgreement, Long> {

    @EntityGraph(attributePaths = {"term", "termVersion"})
    List<UserTermAgreement> findByUserIdAndTermIdIn(Long userId, List<Long> termIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UserTermAgreement agreement WHERE agreement.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
