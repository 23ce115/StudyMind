package com.studymind.backend.controller;

import com.studymind.backend.dto.Dtos.*;
import com.studymind.backend.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.studymind.backend.model.Chat;
import com.studymind.backend.model.Message;
import com.studymind.backend.service.ChatService;

@RestController
@RequestMapping("/api/chats")
public class ChatController {
    private final ChatService chats;
    private final CurrentUser me;

    public ChatController(ChatService chats, CurrentUser me) { this.chats = chats; this.me = me; }

    private static ChatResponse toDto(Chat c) {
        return new ChatResponse(c.id, c.title, c.documentId, c.subjectId, c.createdAt, c.updatedAt);
    }

    static MessageResponse toDto(Message m) {
        SnippetDto snippet = m.snippetCode == null ? null : new SnippetDto(m.snippetLabel, m.snippetCode, m.snippetExplanation);
        return new MessageResponse(m.id, m.role, m.kind, m.text, m.heading, m.body, snippet, m.citation, m.createdAt);
    }

    @GetMapping
    public List<ChatResponse> list() { return chats.list(me.id()).stream().map(ChatController::toDto).toList(); }

    /** Optional body: {"title": "...", "documentId": "...", "subjectId": "..."} to limit the chat to one document or subject. */
    @PostMapping
    public ChatResponse create(@Valid @RequestBody(required = false) CreateChatRequest req) { return toDto(chats.create(me.id(), req)); }

    @GetMapping("/{id}")
    public ChatResponse get(@PathVariable UUID id) { return toDto(chats.getOwned(me.id(), id)); }

    @GetMapping("/{id}/messages")
    public List<MessageResponse> messages(@PathVariable UUID id) { return chats.messages(me.id(), id).stream().map(ChatController::toDto).toList(); }

    /** Ask a question. Returns the assistant's answer (heading, body, optional snippet, page citations). */
    @PostMapping("/{id}/messages")
    public MessageResponse ask(@PathVariable UUID id, @Valid @RequestBody SendMessageRequest req) {
        return toDto(chats.ask(me.id(), id, req.text().trim()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) { chats.delete(me.id(), id); return ResponseEntity.noContent().build(); }
}
