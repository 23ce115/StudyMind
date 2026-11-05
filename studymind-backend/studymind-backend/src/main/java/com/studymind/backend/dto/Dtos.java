package com.studymind.backend.dto;

import com.studymind.backend.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Every request/response body of the API in one place. */
public final class Dtos {
    private Dtos() {}

    // ---------- auth & profile ----------
    public record RegisterRequest(@NotBlank @Email String email, @NotBlank @Size(min = 8, max = 100) String password,
                                  @NotBlank @Size(max = 80) String name, @Size(max = 120) String program) {}
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record UpdateProfileRequest(@Size(max = 80) String name, @Size(max = 120) String program,
                                       @Min(15) @Max(720) Integer dailyGoalMinutes) {}
    public record UserResponse(UUID id, String email, String name, String program, int dailyGoalMinutes) {
        public static UserResponse from(User u) {
            return new UserResponse(u.id, u.email, u.name, u.program, u.dailyGoalMinutes);
        }
    }
    public record AuthResponse(String token, UserResponse user) {}

    // ---------- subjects ----------
    public record SubjectRequest(@NotBlank @Size(max = 80) String name, String color) {}
    public record SubjectResponse(UUID id, String name, String color, long documents) {}

    // ---------- timetable ----------
    public record TimetableRequest(@NotBlank @Size(max = 6000) String text) {}
    public record TimetableEntryDto(UUID id, int day, String dayName, String start, String end,
                                    UUID subjectId, String subject, String location) {}
    public record SubjectLoad(UUID id, String name, String color, int classesPerWeek, double weeklyHours) {}
    /** parsedBy: "ai", "rules" (built-in reader) or "saved" (just reading what was stored). */
    public record TimetableResponse(String text, List<TimetableEntryDto> entries, List<SubjectLoad> subjects,
                                    int subjectCount, double totalWeeklyHours, double[] classHoursByDay, String parsedBy) {}

    // ---------- exams ----------
    public record ExamRequest(UUID subjectId, @Size(max = 80) String subjectName, @Size(max = 120) String title,
                              @NotNull LocalDate examDate, @Min(1) @Max(1000) Integer maxMarks,
                              @Min(1) @Max(5) Integer importance, @Min(0) Integer obtainedMarks, @Size(max = 1000) String notes) {}
    /** status: UPCOMING | TODAY | DONE */
    public record ExamResponse(UUID id, UUID subjectId, String subject, String color, String title, LocalDate examDate,
                               long daysLeft, int maxMarks, Integer obtainedMarks, Double percentage, int importance,
                               String notes, String status) {}
    public record SubjectPerformance(String subject, double averagePercent, int exams) {}
    public record ExamSummary(int upcoming, int completed, Double averagePercent, ExamResponse next,
                              List<SubjectPerformance> subjects) {}

    // ---------- documents ----------
    public record DocumentResponse(UUID id, String name, UUID subjectId, String subject, String status, Long sizeBytes,
                                   Integer pageCount, Instant createdAt, boolean hasSummary) {}
    public record SummaryResponse(String summary) {}
    public record GenerateRequest(Integer count, String difficulty) {}

    // ---------- chat ----------
    public record CreateChatRequest(@Size(max = 120) String title, UUID documentId, UUID subjectId) {}
    public record ChatResponse(UUID id, String title, UUID documentId, UUID subjectId, Instant createdAt, Instant updatedAt) {}
    public record SnippetDto(String label, String code, String explanation) {}
    public record MessageResponse(UUID id, String role, String kind, String text, String heading, String body,
                                  SnippetDto snippet, String citation, Instant createdAt) {}
    public record SendMessageRequest(@NotBlank @Size(max = 2000) String text) {}

    // ---------- flashcards ----------
    public record FlashcardRequest(@NotBlank @Size(max = 1000) String front, @NotBlank @Size(max = 3000) String back, UUID documentId) {}
    public record ReviewRequest(@NotBlank String rating) {}   // again | good | easy
    public record FlashcardResponse(UUID id, String front, String back, UUID documentId, int box, int reviewCount,
                                    Instant nextReviewAt, Instant createdAt) {}

    // ---------- quizzes ----------
    public record QuizResponse(UUID id, String title, UUID documentId, String difficulty, int questions,
                               Integer lastScorePercent, Instant createdAt) {}
    public record QuestionDto(UUID id, String prompt, List<String> options, Integer page) {}
    public record QuizDetail(UUID id, String title, UUID documentId, String difficulty, List<QuestionDto> questions) {}
    public record AttemptRequest(@NotNull List<Integer> answers) {}
    public record QuestionResult(UUID questionId, Integer chosen, int correctIndex, boolean correct, String explanation, Integer page) {}
    public record AttemptResponse(UUID attemptId, int score, int total, int percent, List<QuestionResult> results) {}

    // ---------- study plan ----------
    public record PlanItem(String subject, UUID examId, double hours, String focus) {}
    public record PlanDay(LocalDate date, String dayName, double capacityHours, List<PlanItem> items) {}
    public record PlanSubject(String subject, UUID examId, LocalDate examDate, int daysLeft, double plannedHours,
                              int weaknessPercent, String priority) {}
    public record StudyPlanResponse(Instant generatedAt, double dailyHours, String summary, List<String> tips,
                                    List<PlanSubject> subjects, List<PlanDay> days, String advisedBy) {}

    // ---------- dashboard ----------
    public record StatsDto(long documents, long insights, long conversations, long flashcards, long flashcardsReviewed) {}
    public record UsageDto(int questionsToday, int questionsLimit) {}
    public record StreakDto(int days, int best, boolean[] week) {}
    public record TodayDto(long minutes, int goalMinutes) {}
    public record ExamBrief(UUID id, String subject, String title, LocalDate date, long left, String color) {}
    public record ActivityDto(UUID id, String type, String title, String detail, Instant createdAt) {}
    public record ResumeDto(UUID documentId, String title, UUID chatId, Instant lastStudiedAt, Integer page, Integer pageCount, int percent) {}
    public record DashboardResponse(UserResponse user, StatsDto stats, UsageDto usage, StreakDto streak, TodayDto today,
                                    List<ExamBrief> exams, List<ActivityDto> activity, ResumeDto resume) {}
    public record StudySessionRequest(@Min(1) @Max(600) int minutes) {}
}
