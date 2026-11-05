package com.studymind.backend.repository;

import com.studymind.backend.model.Exam;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamRepository extends JpaRepository<Exam, UUID> {
    List<Exam> findByUserIdOrderByExamDateAsc(UUID userId);
    Optional<Exam> findByIdAndUserId(UUID id, UUID userId);
    boolean existsBySubjectId(UUID subjectId);
}
