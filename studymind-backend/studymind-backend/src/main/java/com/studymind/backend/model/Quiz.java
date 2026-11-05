package com.studymind.backend.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "quizzes")
public class Quiz {
    @Id @GeneratedValue public UUID id;
    @Column(nullable = false) public UUID userId;
    public UUID documentId;
    public UUID subjectId;
    @Column(nullable = false) public String title;
    @Column(nullable = false) public String difficulty = "medium";
    @Column(nullable = false) public Instant createdAt = Instant.now();
}
