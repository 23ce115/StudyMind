package com.studymind.backend.controller;

import com.studymind.backend.dto.Dtos.*;
import com.studymind.backend.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.studymind.backend.service.QuizService;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {
    private final QuizService quizzes;
    private final CurrentUser me;

    public QuizController(QuizService quizzes, CurrentUser me) { this.quizzes = quizzes; this.me = me; }

    @GetMapping
    public List<QuizResponse> list() { return quizzes.list(me.id()); }

    /** Questions and options only; the answers come back after you submit. */
    @GetMapping("/{id}")
    public QuizDetail get(@PathVariable UUID id) { return quizzes.detail(me.id(), id); }

    /** Body: {"answers": [2, 0, null, 1]}: chosen option index per question, in order. */
    @PostMapping("/{id}/attempts")
    public AttemptResponse submit(@PathVariable UUID id, @Valid @RequestBody AttemptRequest req) { return quizzes.submit(me.id(), id, req.answers()); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) { quizzes.delete(me.id(), id); return ResponseEntity.noContent().build(); }
}
