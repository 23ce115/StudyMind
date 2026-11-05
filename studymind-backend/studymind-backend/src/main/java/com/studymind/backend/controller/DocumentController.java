package com.studymind.backend.controller;

import com.studymind.backend.dto.Dtos.*;
import com.studymind.backend.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.studymind.backend.model.Document;
import com.studymind.backend.service.DocumentService;
import com.studymind.backend.service.FlashcardService;
import com.studymind.backend.service.GenerationService;
import com.studymind.backend.service.QuizService;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    public record MoveRequest(UUID subjectId) {}

    private final DocumentService documents;
    private final GenerationService generation;
    private final QuizService quizzes;
    private final CurrentUser me;

    public DocumentController(DocumentService documents, GenerationService generation, QuizService quizzes, CurrentUser me) {
        this.documents = documents;
        this.generation = generation;
        this.quizzes = quizzes;
        this.me = me;
    }

    @GetMapping
    public List<DocumentResponse> list() { return documents.list(me.id()); }

    /** multipart/form-data: file (PDF, TXT or MD) and optional subjectId. */
    @PostMapping(consumes = "multipart/form-data")
    public DocumentResponse upload(@RequestParam("file") MultipartFile file, @RequestParam(value = "subjectId", required = false) UUID subjectId) {
        Document d = documents.upload(me.id(), file, subjectId);
        return documents.toResponse(me.id(), d);
    }

    /** Move a document to another subject (or none). */
    @PatchMapping("/{id}")
    public DocumentResponse move(@PathVariable UUID id, @RequestBody MoveRequest req) {
        return documents.toResponse(me.id(), documents.setSubject(me.id(), id, req.subjectId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) { documents.delete(me.id(), id); return ResponseEntity.noContent().build(); }

    /** The "Summarize PDF" tool. Cached; add ?refresh=true to write a new one. */
    @PostMapping("/{id}/summary")
    public SummaryResponse summary(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean refresh) {
        return documents.summarize(me.id(), id, refresh);
    }

    @PostMapping("/{id}/flashcards/generate")
    public List<FlashcardResponse> flashcards(@PathVariable UUID id, @RequestBody(required = false) GenerateRequest req) {
        return generation.generateFlashcards(me.id(), id, req == null ? null : req.count()).stream().map(FlashcardService::toResponse).toList();
    }

    @PostMapping("/{id}/quizzes/generate")
    public QuizResponse quiz(@PathVariable UUID id, @RequestBody(required = false) GenerateRequest req) {
        var quiz = generation.generateQuiz(me.id(), id, req == null ? null : req.count(), req == null ? null : req.difficulty());
        return quizzes.toResponse(me.id(), quiz);
    }
}
