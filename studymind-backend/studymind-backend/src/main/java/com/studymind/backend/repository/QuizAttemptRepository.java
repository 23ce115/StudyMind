package com.studymind.backend.repository;

import com.studymind.backend.model.QuizAttempt;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {
    Optional<QuizAttempt> findFirstByQuizIdAndUserIdOrderByTakenAtDesc(UUID quizId, UUID userId);
    List<QuizAttempt> findByQuizIdAndUserIdOrderByTakenAtDesc(UUID quizId, UUID userId);
    void deleteByQuizId(UUID quizId);
}
