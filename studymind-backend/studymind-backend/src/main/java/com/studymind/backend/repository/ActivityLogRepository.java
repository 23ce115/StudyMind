package com.studymind.backend.repository;

import com.studymind.backend.model.ActivityLog;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {
    List<ActivityLog> findTop6ByUserIdOrderByCreatedAtDesc(UUID userId);
    boolean existsByUserIdAndTypeAndCreatedAtAfter(UUID userId, String type, Instant after);

    @Query("select a.createdAt from ActivityLog a where a.userId = :userId and a.createdAt >= :since")
    List<Instant> timestampsSince(@Param("userId") UUID userId, @Param("since") Instant since);
}
