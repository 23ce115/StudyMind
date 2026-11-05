package com.studymind.backend.repository;

import com.studymind.backend.model.Chat;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRepository extends JpaRepository<Chat, UUID> {
    List<Chat> findByUserIdOrderByUpdatedAtDesc(UUID userId);
    Optional<Chat> findByIdAndUserId(UUID id, UUID userId);
    Optional<Chat> findFirstByUserIdAndDocumentIdOrderByUpdatedAtDesc(UUID userId, UUID documentId);
    long countByUserId(UUID userId);
}
