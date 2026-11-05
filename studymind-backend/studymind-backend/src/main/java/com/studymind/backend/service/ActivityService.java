package com.studymind.backend.service;

import com.studymind.backend.model.ActivityLog;
import com.studymind.backend.repository.ActivityLogRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** The "recent activity" feed. Every row also counts as a study day for the streak. */
@Service
public class ActivityService {
    private final ActivityLogRepository repo;

    public ActivityService(ActivityLogRepository repo) { this.repo = repo; }

    public void log(UUID userId, String type, String title, String detail) {
        ActivityLog a = new ActivityLog();
        a.userId = userId;
        a.type = type;
        a.title = title.length() > 200 ? title.substring(0, 200) : title;
        a.detail = detail;
        repo.save(a);
    }

    /** Like log(), but skipped if the same type was already logged in the last {@code minutes} (avoids feed spam). */
    public void logOnce(UUID userId, String type, String title, String detail, int minutes) {
        if (repo.existsByUserIdAndTypeAndCreatedAtAfter(userId, type, Instant.now().minus(minutes, ChronoUnit.MINUTES))) return;
        log(userId, type, title, detail);
    }

    public List<ActivityLog> recent(UUID userId) { return repo.findTop6ByUserIdOrderByCreatedAtDesc(userId); }
}
