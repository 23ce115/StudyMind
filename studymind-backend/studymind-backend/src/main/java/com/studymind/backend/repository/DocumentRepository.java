package com.studymind.backend.repository;

import com.studymind.backend.model.Document;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Document> findByUserIdAndSubjectId(UUID userId, UUID subjectId);
    Optional<Document> findByIdAndUserId(UUID id, UUID userId);
    long countByUserId(UUID userId);
    long countBySubjectId(UUID subjectId);
    Optional<Document> findFirstByUserIdAndLastStudiedAtNotNullOrderByLastStudiedAtDesc(UUID userId);
}
