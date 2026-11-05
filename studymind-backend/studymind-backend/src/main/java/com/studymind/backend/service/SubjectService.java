package com.studymind.backend.service;

import com.studymind.backend.dto.Dtos.*;
import com.studymind.backend.exception.ApiException;
import com.studymind.backend.model.Document;
import com.studymind.backend.model.Subject;
import com.studymind.backend.repository.DocumentRepository;
import com.studymind.backend.repository.ExamRepository;
import com.studymind.backend.repository.SubjectRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectService {
    /** Same names as the colour classes in the frontend. */
    static final List<String> TONES = List.of("blue", "green", "orange", "purple", "red", "yellow");

    private final SubjectRepository subjects;
    private final DocumentRepository documents;
    private final ExamRepository exams;

    public SubjectService(SubjectRepository subjects, DocumentRepository documents, ExamRepository exams) {
        this.subjects = subjects;
        this.documents = documents;
        this.exams = exams;
    }

    public static String cleanName(String s) { return s == null ? "" : s.trim().replaceAll("\\s+", " "); }

    public Subject get(UUID userId, UUID id) {
        return subjects.findByIdAndUserId(id, userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subject not found"));
    }

    /** Existing subject with that name (any capitalisation) or a new one. */
    public Subject findOrCreate(UUID userId, String rawName) {
        String name = cleanName(rawName);
        if (name.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Subject name is required");
        return subjects.findByUserIdAndNameIgnoreCase(userId, name).orElseGet(() -> create(userId, name, null));
    }

    private Subject create(UUID userId, String name, String color) {
        Subject s = new Subject();
        s.userId = userId;
        s.name = name;
        s.color = color != null && TONES.contains(color) ? color : TONES.get(subjects.findByUserIdOrderByNameAsc(userId).size() % TONES.size());
        return subjects.save(s);
    }

    public List<SubjectResponse> list(UUID userId) {
        return subjects.findByUserIdOrderByNameAsc(userId).stream()
            .map(s -> new SubjectResponse(s.id, s.name, s.color, documents.countBySubjectId(s.id))).toList();
    }

    public SubjectResponse add(UUID userId, SubjectRequest req) {
        String name = cleanName(req.name());
        if (subjects.findByUserIdAndNameIgnoreCase(userId, name).isPresent()) throw new ApiException(HttpStatus.CONFLICT, "You already have that subject");
        Subject s = create(userId, name, req.color());
        return new SubjectResponse(s.id, s.name, s.color, 0);
    }

    public SubjectResponse update(UUID userId, UUID id, SubjectRequest req) {
        Subject s = get(userId, id);
        String name = cleanName(req.name());
        subjects.findByUserIdAndNameIgnoreCase(userId, name).ifPresent(other -> {
            if (!other.id.equals(id)) throw new ApiException(HttpStatus.CONFLICT, "You already have that subject");
        });
        s.name = name;
        if (req.color() != null && TONES.contains(req.color())) s.color = req.color();
        s = subjects.save(s);
        return new SubjectResponse(s.id, s.name, s.color, documents.countBySubjectId(s.id));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Subject s = get(userId, id);
        if (exams.existsBySubjectId(id)) throw new ApiException(HttpStatus.CONFLICT, "Delete this subject's exams first");
        for (Document d : documents.findByUserIdAndSubjectId(userId, id)) { d.subjectId = null; documents.save(d); }
        subjects.delete(s);
    }
}
