package com.studymind.backend.repository;

import com.studymind.backend.model.UsageDaily;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsageDailyRepository extends JpaRepository<UsageDaily, UUID> {
    Optional<UsageDaily> findByUserIdAndUsageDate(UUID userId, java.time.LocalDate usageDate);
}
