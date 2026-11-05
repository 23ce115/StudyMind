package com.studymind.backend.service;

import com.studymind.backend.config.AppTime;
import com.studymind.backend.dto.Dtos.*;
import com.studymind.backend.exception.ApiException;
import com.studymind.backend.model.Exam;
import com.studymind.backend.model.Subject;
import com.studymind.backend.repository.ExamRepository;
import com.studymind.backend.repository.SubjectRepository;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ExamService {
    private final ExamRepository exams;
    private final SubjectRepository subjectRepo;
    private final SubjectService subjects;
    private final ActivityService activity;
    private final AppTime time;

    public ExamService(ExamRepository exams, SubjectRepository subjectRepo, SubjectService subjects, ActivityService activity, AppTime time) {
        this.exams = exams;
        this.subjectRepo = subjectRepo;
        this.subjects = subjects;
        this.activity = activity;
        this.time = time;
    }

    private Map<UUID, Subject> subjectMap(UUID userId) {
        Map<UUID, Subject> m = new HashMap<>();
        for (Subject s : subjectRepo.findByUserIdOrderByNameAsc(userId)) m.put(s.id, s);
        return m;
    }

    private ExamResponse toResponse(Exam e, Map<UUID, Subject> subs) {
        Subject s = subs.get(e.subjectId);
        long left = ChronoUnit.DAYS.between(time.today(), e.examDate);
        Double pct = e.obtainedMarks == null ? null : Math.round(e.obtainedMarks * 1000.0 / e.maxMarks) / 10.0;
        String status = e.obtainedMarks != null || left < 0 ? "DONE" : left == 0 ? "TODAY" : "UPCOMING";
        return new ExamResponse(e.id, e.subjectId, s == null ? "Unknown" : s.name, s == null ? null : s.color, e.title, e.examDate,
            left, e.maxMarks, e.obtainedMarks, pct, e.importance, e.notes, status);
    }

    public List<ExamResponse> list(UUID userId) {
        Map<UUID, Subject> subs = subjectMap(userId);
        return exams.findByUserIdOrderByExamDateAsc(userId).stream().map(e -> toResponse(e, subs)).toList();
    }

    public ExamResponse create(UUID userId, ExamRequest req) {
        Exam e = new Exam();
        e.userId = userId;
        ExamResponse r = toResponse(save(userId, e, req), subjectMap(userId));
        activity.log(userId, "EXAM", r.subject() + " exam added", "On " + r.examDate());
        return r;
    }

    public ExamResponse update(UUID userId, UUID id, ExamRequest req) {
        Exam e = exams.findByIdAndUserId(id, userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam not found"));
        return toResponse(save(userId, e, req), subjectMap(userId));
    }

    private Exam save(UUID userId, Exam e, ExamRequest req) {
        Subject subject;
        if (req.subjectId() != null) subject = subjects.get(userId, req.subjectId());
        else if (req.subjectName() != null && !req.subjectName().isBlank()) subject = subjects.findOrCreate(userId, req.subjectName());
        else throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a subject (subjectId or subjectName)");

        int max = req.maxMarks() == null ? 100 : req.maxMarks();
        if (req.obtainedMarks() != null && req.obtainedMarks() > max) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Marks obtained can't be more than the maximum marks");
        }
        e.subjectId = subject.id;
        e.title = req.title() == null || req.title().isBlank() ? subject.name + " exam" : req.title().trim();
        e.examDate = req.examDate();
        e.maxMarks = max;
        e.obtainedMarks = req.obtainedMarks();
        e.importance = req.importance() == null ? 3 : req.importance();
        e.notes = req.notes();
        return exams.save(e);
    }

    public void delete(UUID userId, UUID id) {
        exams.delete(exams.findByIdAndUserId(id, userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam not found")));
    }

    /** How many exams are coming, how the finished ones went, and the average per subject. */
    public ExamSummary summary(UUID userId) {
        List<ExamResponse> all = list(userId);
        List<ExamResponse> upcoming = all.stream().filter(e -> !"DONE".equals(e.status())).toList();
        List<ExamResponse> done = all.stream().filter(e -> e.percentage() != null).toList();
        Double avg = done.isEmpty() ? null : Math.round(done.stream().mapToDouble(ExamResponse::percentage).average().orElse(0) * 10) / 10.0;

        Map<String, List<Double>> bySubject = new HashMap<>();
        for (ExamResponse e : done) bySubject.computeIfAbsent(e.subject(), k -> new ArrayList<>()).add(e.percentage());
        List<SubjectPerformance> perf = bySubject.entrySet().stream()
            .map(en -> new SubjectPerformance(en.getKey(), Math.round(en.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0) * 10) / 10.0, en.getValue().size()))
            .sorted((a, b) -> Double.compare(a.averagePercent(), b.averagePercent())).toList();
        return new ExamSummary(upcoming.size(), done.size(), avg, upcoming.isEmpty() ? null : upcoming.get(0), perf);
    }

    /** 0 = strong .. 1 = weak, from past marks; subjects without any marks get a neutral 0.5. */
    public Map<UUID, Double> weaknessBySubject(UUID userId) {
        Map<UUID, List<Double>> pcts = new HashMap<>();
        for (Exam e : exams.findByUserIdOrderByExamDateAsc(userId)) {
            if (e.obtainedMarks != null) pcts.computeIfAbsent(e.subjectId, k -> new ArrayList<>()).add(e.obtainedMarks * 100.0 / e.maxMarks);
        }
        Map<UUID, Double> out = new HashMap<>();
        pcts.forEach((id, list) -> out.put(id, Math.max(0, Math.min(1, 1 - list.stream().mapToDouble(Double::doubleValue).average().orElse(50) / 100.0))));
        return out;
    }
}
