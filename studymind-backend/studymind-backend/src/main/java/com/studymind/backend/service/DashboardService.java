package com.studymind.backend.service;

import com.studymind.backend.config.AppTime;
import com.studymind.backend.dto.Dtos.*;
import com.studymind.backend.logic.StreakCalculator;
import com.studymind.backend.model.Chat;
import com.studymind.backend.model.Document;
import com.studymind.backend.model.StudySession;
import com.studymind.backend.model.User;
import com.studymind.backend.repository.ChatRepository;
import com.studymind.backend.repository.DocumentRepository;
import com.studymind.backend.repository.FlashcardRepository;
import com.studymind.backend.repository.MessageRepository;
import com.studymind.backend.repository.ActivityLogRepository;
import com.studymind.backend.repository.StudySessionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Everything the Home screen shows, in one call. */
@Service
public class DashboardService {
    private final AuthService auth;
    private final DocumentRepository documents;
    private final ChatRepository chats;
    private final MessageRepository messages;
    private final FlashcardRepository flashcards;
    private final ActivityLogRepository activityLog;
    private final StudySessionRepository sessions;
    private final ExamService exams;
    private final ActivityService activity;
    private final UsageService usage;
    private final AppTime time;

    public DashboardService(AuthService auth, DocumentRepository documents, ChatRepository chats, MessageRepository messages,
                            FlashcardRepository flashcards, ActivityLogRepository activityLog, StudySessionRepository sessions,
                            ExamService exams, ActivityService activity, UsageService usage, AppTime time) {
        this.auth = auth;
        this.documents = documents;
        this.chats = chats;
        this.messages = messages;
        this.flashcards = flashcards;
        this.activityLog = activityLog;
        this.sessions = sessions;
        this.exams = exams;
        this.activity = activity;
        this.usage = usage;
        this.time = time;
    }

    public DashboardResponse build(UUID userId) {
        User user = auth.get(userId);
        LocalDate today = time.today();

        StatsDto stats = new StatsDto(documents.countByUserId(userId), messages.countAnswersForUser(userId), chats.countByUserId(userId),
            flashcards.countByUserId(userId), flashcards.totalReviews(userId));
        UsageDto usageDto = new UsageDto(usage.questionsToday(userId), usage.limit());

        Instant since = Instant.now().minus(400, ChronoUnit.DAYS);
        Set<LocalDate> days = new HashSet<>();
        activityLog.timestampsSince(userId, since).forEach(t -> days.add(time.dateOf(t)));
        sessions.timestampsSince(userId, since).forEach(t -> days.add(time.dateOf(t)));
        StreakCalculator.Streak st = StreakCalculator.compute(days, today);

        long minutes = sessions.minutesBetween(userId, time.startOfDay(today), time.startOfDay(today.plusDays(1)));
        List<ExamBrief> upcoming = exams.list(userId).stream().filter(e -> !"DONE".equals(e.status())).limit(4)
            .map(e -> new ExamBrief(e.id(), e.subject(), e.title(), e.examDate(), e.daysLeft(), e.color())).toList();
        List<ActivityDto> feed = activity.recent(userId).stream().map(a -> new ActivityDto(a.id, a.type, a.title, a.detail, a.createdAt)).toList();

        return new DashboardResponse(UserResponse.from(user), stats, usageDto, new StreakDto(st.current(), st.best(), st.week()),
            new TodayDto(minutes, user.dailyGoalMinutes), upcoming, feed, resume(userId).orElse(null));
    }

    /** "Continue learning": the document the student last asked about, and the page they reached. */
    private Optional<ResumeDto> resume(UUID userId) {
        return documents.findFirstByUserIdAndLastStudiedAtNotNullOrderByLastStudiedAtDesc(userId).map(d -> {
            UUID chatId = chats.findFirstByUserIdAndDocumentIdOrderByUpdatedAtDesc(userId, d.id).map(c -> c.id).orElse(null);
            int pct = d.pageCount != null && d.pageCount > 0 && d.lastPage != null ? Math.min(100, Math.round(d.lastPage * 100f / d.pageCount)) : 0;
            return new ResumeDto(d.id, d.name, chatId, d.lastStudiedAt, d.lastPage, d.pageCount, pct);
        });
    }

    public void logStudySession(UUID userId, int minutes) {
        StudySession s = new StudySession();
        s.userId = userId;
        s.minutes = minutes;
        sessions.save(s);
        activity.log(userId, "STUDY_SESSION", "Study session", minutes + " min studied");
    }
}
