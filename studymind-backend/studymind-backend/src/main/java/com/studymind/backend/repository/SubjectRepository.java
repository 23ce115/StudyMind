package com.studymind.backend.repository;

import com.studymind.backend.model.Subject;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    List<Subject> findByUserIdOrderByNameAsc(UUID userId);
    Optional<Subject> findByIdAndUserId(UUID id, UUID userId);
    Optional<Subject> findByUserIdAndNameIgnoreCase(UUID userId, String name);
}
