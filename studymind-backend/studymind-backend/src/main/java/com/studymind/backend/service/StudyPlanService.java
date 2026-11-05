package com.studymind.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studymind.backend.config.AppTime;
import com.studymind.backend.dto.Dtos.*;
import com.studymind.backend.exception.ApiException;
import com.studymind.backend.logic.StudyPlanner;
import com.studymind.backend.model.Exam;
import com.studymind.backend.model.StudyPlan;
import com.studymind.backend.model.Subject;
import com.studymind.backend.model.User;
import com.studymind.backend.repository.ExamRepository;
import com.studymind.backend.repository.StudyPlanRepository;
import com.studymind.backend.repository.SubjectRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Builds the day-by-day study schedule. The hours are worked out by StudyPlanner (deterministic, always works);
 * the AI, when available, only adds the coaching text on top.
 */
@Service
public class StudyPlanService {
    private static final Logger log = LoggerFactory.getLogger(StudyPlanService.class);
    private static final String COACH_SYSTEM = """
        You are a supportive study coach for a college student. You get their upcoming exams with days left, importance (1-5),
        weakness (from past marks, 0-100%) and the hours already planned. Write a short plan summary (2-3 sentences: what to start with and why)
        and up to 5 concrete, practical tips for this student. Do not invent exams or numbers.
        Reply with JSON only: {"summary": "...", "tips": ["...", "..."]}
        """;

    private final ExamRepository exams;
    private final SubjectRepository subjectRepo;
    private final StudyPlanRepository plans;
    private final ExamService examService;
    private final TimetableService timetable;
    private final AuthService auth;
    private final AiService ai;
    private final ActivityService activity;
    private final ObjectMapper mapper;
    private final AppTime time;

    public StudyPlanService(ExamRepository exams, SubjectRepository subjectRepo, StudyPlanRepository plans, ExamService examService,
                            TimetableService timetable, AuthService auth, AiService ai, ActivityService activity, ObjectMapper mapper, AppTime time) {
        this.exams = exams;
        this.subjectRepo = subjectRepo;
        this.plans = plans;
        this.examService = examService;
        this.timetable = timetable;
        this.auth = auth;
        this.ai = ai;
        this.activity = activity;
        this.mapper = mapper;
        this.time = time;
    }

    public Optional<StudyPlanResponse> latest(UUID userId) {
        return plans.findFirstByUserIdOrderByGeneratedAtDesc(userId).map(p -> {
            try {
                return mapper.readValue(p.planJson, StudyPlanResponse.class);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Stored study plan is unreadable", e);
            }
        });
    }

    public StudyPlanResponse generate(UUID userId) {
        User user = auth.get(userId);
        LocalDate today = time.today();
        Map<UUID, Subject> subs = new HashMap<>();
        for (Subject s : subjectRepo.findByUserIdOrderByNameAsc(userId)) subs.put(s.id, s);
        Map<UUID, Double> weakness = examService.weaknessBySubject(userId);

        List<StudyPlanner.ExamIn> upcoming = new ArrayList<>();
        for (Exam e : exams.findByUserIdOrderByExamDateAsc(userId)) {
            if (e.obtainedMarks != null || e.examDate.isBefore(today)) continue;   // finished exams don't need planning
            Subject s = subs.get(e.subjectId);
            upcoming.add(new StudyPlanner.ExamIn(e.id.toString(), s == null ? e.title : s.name, e.examDate, e.importance, weakness.getOrDefault(e.subjectId, 0.5)));
        }
        double dailyHours = user.dailyGoalMinutes / 60.0;
        StudyPlanner.Plan plan = StudyPlanner.plan(today, upcoming, dailyHours, timetable.classHoursByDay(userId), 45);

        List<PlanSubject> subjectDtos = plan.subjects().stream()
            .map(s -> new PlanSubject(s.subject(), UUID.fromString(s.examId()), s.examDate(), s.daysLeft(), s.hours(), (int) Math.round(s.weakness() * 100), s.priority()))
            .toList();
        List<PlanDay> dayDtos = plan.days().stream().map(d -> new PlanDay(d.date(), d.date().getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH),
            d.capacityHours(), d.items().stream().map(i -> new PlanItem(i.subject(), UUID.fromString(i.examId()), i.hours(), i.focus())).toList())).toList();

        Advice advice = advise(subjectDtos, dailyHours);
        StudyPlanResponse out = new StudyPlanResponse(Instant.now(), dailyHours, advice.summary(), advice.tips(), subjectDtos, dayDtos, advice.by());

        StudyPlan row = new StudyPlan();
        row.userId = userId;
        try {
            row.planJson = mapper.writeValueAsString(out);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Couldn't save the study plan");
        }
        row.generatedAt = out.generatedAt();
        plans.save(row);
        activity.log(userId, "STUDY_PLAN", "Study plan updated", subjectDtos.size() + " upcoming exams planned");
        return out;
    }

    private record Advice(String summary, List<String> tips, String by) {}

    private Advice advise(List<PlanSubject> subjects, double dailyHours) {
        if (subjects.isEmpty()) {
            return new Advice("You have no upcoming exams yet. Add your exams and I'll build a day-by-day study plan.", List.of(), "rules");
        }
        if (ai.isConfigured()) {
            try {
                StringBuilder p = new StringBuilder("Daily study time: " + dailyHours + " hours\nExams:\n");
                for (PlanSubject s : subjects) {
                    p.append("- ").append(s.subject()).append(": ").append(s.daysLeft()).append(" days left, weakness ")
                        .append(s.weaknessPercent()).append("%, planned ").append(s.plannedHours()).append(" h, priority ").append(s.priority()).append('\n');
                }
                JsonNode j = ai.completeJson(COACH_SYSTEM, p.toString());
                String summary = j.path("summary").asText("").trim();
                List<String> tips = new ArrayList<>();
                for (JsonNode t : j.path("tips")) if (!t.asText("").isBlank() && tips.size() < 5) tips.add(t.asText().trim());
                if (!summary.isEmpty()) return new Advice(summary, tips, "ai");
            } catch (ApiException e) {
                log.info("AI coaching text unavailable ({}), using the built-in text", e.getMessage());
            }
        }
        PlanSubject first = subjects.stream().filter(s -> s.daysLeft() > 0).min(Comparator.comparingInt(PlanSubject::daysLeft)).orElse(subjects.get(0));
        String summary = "You have " + subjects.size() + " upcoming exam" + (subjects.size() == 1 ? "" : "s") + ". The next one is "
            + first.subject() + " in " + first.daysLeft() + " day" + (first.daysLeft() == 1 ? "" : "s")
            + ". Each day's time is split by how close each exam is, how important it is and how you did in that subject before.";
        return new Advice(summary, List.of(
            "Start each day with the highest-priority subject while your mind is fresh.",
            "Use flashcards for definitions and quizzes to test yourself before every exam.",
            "Take a 10-minute break after every 50 minutes of study.",
            "Enter your marks after each exam so the plan can focus on your weaker subjects."), "rules");
    }
}
