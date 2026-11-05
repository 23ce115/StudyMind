package com.studymind.backend.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "study_sessions")
public class StudySession {
    @Id @GeneratedValue public UUID id;
    @Column(nullable = false) public UUID userId;
    @Column(nullable = false) public int minutes;
    @Column(nullable = false) public Instant createdAt = Instant.now();
}
