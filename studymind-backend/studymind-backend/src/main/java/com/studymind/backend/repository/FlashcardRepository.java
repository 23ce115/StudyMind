package com.studymind.backend.repository;

import com.studymind.backend.model.Flashcard;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FlashcardRepository extends JpaRepository<Flashcard, UUID> {
    List<Flashcard> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Flashcard> findByUserIdAndDocumentIdOrderByCreatedAtDesc(UUID userId, UUID documentId);
    List<Flashcard> findByUserIdAndNextReviewAtBeforeOrderByNextReviewAtAsc(UUID userId, Instant now);
    Optional<Flashcard> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByUserIdAndDocumentIdAndFrontIgnoreCase(UUID userId, UUID documentId, String front);
    long countByUserId(UUID userId);

    @Query("select coalesce(sum(f.reviewCount), 0L) from Flashcard f where f.userId = :userId")
    long totalReviews(@Param("userId") UUID userId);
}
