package com.studymind.backend.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {
    @Id @GeneratedValue public UUID id;
    @Column(nullable = false) public UUID quizId;
    @Column(nullable = false) public UUID userId;
    @Column(nullable = false) public int score;
    @Column(nullable = false) public int total;
    @Column(columnDefinition = "text") public String answersJson;
    @Column(nullable = false) public Instant takenAt = Instant.now();
}
