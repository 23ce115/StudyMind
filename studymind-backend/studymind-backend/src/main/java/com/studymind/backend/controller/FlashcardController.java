package com.studymind.backend.controller;

import com.studymind.backend.dto.Dtos.*;
import com.studymind.backend.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.studymind.backend.service.FlashcardService;

@RestController
@RequestMapping("/api/flashcards")
public class FlashcardController {
    private final FlashcardService cards;
    private final CurrentUser me;

    public FlashcardController(FlashcardService cards, CurrentUser me) { this.cards = cards; this.me = me; }

    @GetMapping
    public List<FlashcardResponse> list(@RequestParam(required = false) UUID documentId) {
        return cards.list(me.id(), documentId).stream().map(FlashcardService::toResponse).toList();
    }

    /** Cards that are due for review right now. */
    @GetMapping("/due")
    public List<FlashcardResponse> due() { return cards.due(me.id()).stream().map(FlashcardService::toResponse).toList(); }

    @PostMapping
    public FlashcardResponse create(@Valid @RequestBody FlashcardRequest req) { return FlashcardService.toResponse(cards.create(me.id(), req)); }

    /** Body: {"rating": "again" | "good" | "easy"}. */
    @PostMapping("/{id}/review")
    public FlashcardResponse review(@PathVariable UUID id, @Valid @RequestBody ReviewRequest req) {
        return FlashcardService.toResponse(cards.review(me.id(), id, req.rating()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) { cards.delete(me.id(), id); return ResponseEntity.noContent().build(); }
}
