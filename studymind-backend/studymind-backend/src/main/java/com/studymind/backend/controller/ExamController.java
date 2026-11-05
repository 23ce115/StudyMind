package com.studymind.backend.controller;

import com.studymind.backend.dto.Dtos.*;
import com.studymind.backend.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.studymind.backend.service.ExamService;

@RestController
@RequestMapping("/api/exams")
public class ExamController {
    private final ExamService exams;
    private final CurrentUser me;

    public ExamController(ExamService exams, CurrentUser me) { this.exams = exams; this.me = me; }

    @GetMapping
    public List<ExamResponse> list() { return exams.list(me.id()); }

    @GetMapping("/summary")
    public ExamSummary summary() { return exams.summary(me.id()); }

    @PostMapping
    public ExamResponse create(@Valid @RequestBody ExamRequest req) { return exams.create(me.id(), req); }

    @PutMapping("/{id}")
    public ExamResponse update(@PathVariable UUID id, @Valid @RequestBody ExamRequest req) { return exams.update(me.id(), id, req); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) { exams.delete(me.id(), id); return ResponseEntity.noContent().build(); }
}
