package com.studymind.backend.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "flashcards")
public class Flashcard {
    @Id @GeneratedValue public UUID id;
    @Column(nullable = false) public UUID userId;
    public UUID documentId;
    public UUID chatId;
    public UUID subjectId;
    @Column(nullable = false, columnDefinition = "text") public String front;
    @Column(nullable = false, columnDefinition = "text") public String back;
    @Column(nullable = false) public int box = 0;               // Leitner box 0..5
    @Column(nullable = false) public Instant nextReviewAt = Instant.now();
    @Column(nullable = false) public int reviewCount = 0;
    public Instant lastReviewedAt;
    @Column(nullable = false) public Instant createdAt = Instant.now();
}
