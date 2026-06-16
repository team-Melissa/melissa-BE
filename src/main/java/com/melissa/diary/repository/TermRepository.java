package com.melissa.diary.repository;

import com.melissa.diary.domain.Term;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Long> {

    List<Term> findByActiveTrueOrderByDisplayOrderAsc();

    Optional<Term> findByTermCode(String termCode);
}
