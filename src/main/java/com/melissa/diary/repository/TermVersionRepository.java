package com.melissa.diary.repository;

import com.melissa.diary.domain.TermVersion;
import com.melissa.diary.domain.enums.TermVersionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TermVersionRepository extends JpaRepository<TermVersion, Long> {

    @EntityGraph(attributePaths = "term")
    List<TermVersion> findByTermIdInAndStatusAndEffectiveFromLessThanEqual(
            List<Long> termIds,
            TermVersionStatus status,
            LocalDateTime effectiveFrom
    );

    @Query("""
            SELECT tv
            FROM TermVersion tv
            JOIN FETCH tv.term
            WHERE tv.id = :termVersionId
            """)
    Optional<TermVersion> findByIdWithTerm(@Param("termVersionId") Long termVersionId);
}
