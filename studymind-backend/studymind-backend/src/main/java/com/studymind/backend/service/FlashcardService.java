package com.studymind.backend.service;

import com.studymind.backend.dto.Dtos.*;
import com.studymind.backend.exception.ApiException;
import com.studymind.backend.model.Flashcard;
import com.studymind.backend.repository.FlashcardRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Flashcards with a simple Leitner schedule: right answers move a card to a box that is shown less often. */
@Service
public class FlashcardService {
    private static final int[] BOX_DAYS = {0, 1, 3, 7, 14, 30};

    private final FlashcardRepository repo;
    private final DocumentService documents;
    private final ActivityService activity;

    public FlashcardService(FlashcardRepository repo, DocumentService documents, ActivityService activity) {
        this.repo = repo;
        this.documents = documents;
        this.activity = activity;
    }

    public static FlashcardResponse toResponse(Flashcard f) {
        return new FlashcardResponse(f.id, f.front, f.back, f.documentId, f.box, f.reviewCount, f.nextReviewAt, f.createdAt);
    }

    public List<Flashcard> list(UUID userId, UUID documentId) {
        return documentId == null ? repo.findByUserIdOrderByCreatedAtDesc(userId)
            : repo.findByUserIdAndDocumentIdOrderByCreatedAtDesc(userId, documentId);
    }

    public List<Flashcard> due(UUID userId) { return repo.findByUserIdAndNextReviewAtBeforeOrderByNextReviewAtAsc(userId, Instant.now()); }

    public Flashcard create(UUID userId, FlashcardRequest req) {
        Flashcard f = new Flashcard();
        f.userId = userId;
        if (req.documentId() != null) {
            var d = documents.get(userId, req.documentId());
            f.documentId = d.id;
            f.subjectId = d.subjectId;
        }
        f.front = req.front().trim();
        f.back = req.back().trim();
        return repo.save(f);
    }

    public Flashcard review(UUID userId, UUID id, String rating) {
        Flashcard f = repo.findByIdAndUserId(id, userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Flashcard not found"));
        switch (rating.toLowerCase()) {
            case "again" -> f.box = 0;
            case "good" -> f.box = Math.min(5, f.box + 1);
            case "easy" -> f.box = Math.min(5, f.box + 2);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "rating must be again, good or easy");
        }
        f.reviewCount++;
        f.lastReviewedAt = Instant.now();
        f.nextReviewAt = f.box == 0 ? f.lastReviewedAt.plus(10, ChronoUnit.MINUTES) : f.lastReviewedAt.plus(BOX_DAYS[f.box], ChronoUnit.DAYS);
        f = repo.save(f);
        activity.logOnce(userId, "FLASHCARD_REVIEW", "Flashcard review", "Reviewed flashcards", 30);
        return f;
    }

    public void delete(UUID userId, UUID id) {
        repo.delete(repo.findByIdAndUserId(id, userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Flashcard not found")));
    }
}
