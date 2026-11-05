package com.studymind.backend.controller;

import com.studymind.backend.dto.Dtos.*;
import com.studymind.backend.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.studymind.backend.service.SubjectService;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {
    private final SubjectService subjects;
    private final CurrentUser me;

    public SubjectController(SubjectService subjects, CurrentUser me) { this.subjects = subjects; this.me = me; }

    @GetMapping
    public List<SubjectResponse> list() { return subjects.list(me.id()); }

    @PostMapping
    public SubjectResponse add(@Valid @RequestBody SubjectRequest req) { return subjects.add(me.id(), req); }

    @PutMapping("/{id}")
    public SubjectResponse update(@PathVariable UUID id, @Valid @RequestBody SubjectRequest req) { return subjects.update(me.id(), id, req); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) { subjects.delete(me.id(), id); return ResponseEntity.noContent().build(); }
}
