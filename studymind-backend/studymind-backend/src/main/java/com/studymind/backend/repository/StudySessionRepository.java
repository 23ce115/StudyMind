package com.studymind.backend.repository;

import com.studymind.backend.model.StudySession;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudySessionRepository extends JpaRepository<StudySession, UUID> {
    @Query("select coalesce(sum(s.minutes), 0L) from StudySession s where s.userId = :userId and s.createdAt >= :fromTs and s.createdAt < :toTs")
    long minutesBetween(@Param("userId") UUID userId, @Param("fromTs") Instant fromTs, @Param("toTs") Instant toTs);

    @Query("select s.createdAt from StudySession s where s.userId = :userId and s.createdAt >= :since")
    List<Instant> timestampsSince(@Param("userId") UUID userId, @Param("since") Instant since);
}
