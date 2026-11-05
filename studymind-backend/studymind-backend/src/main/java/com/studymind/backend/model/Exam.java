package com.studymind.backend.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "exams")
public class Exam {
    @Id @GeneratedValue public UUID id;
    @Column(nullable = false) public UUID userId;
    @Column(nullable = false) public UUID subjectId;
    @Column(nullable = false) public String title;
    @Column(nullable = false) public LocalDate examDate;
    @Column(nullable = false) public int maxMarks = 100;
    public Integer obtainedMarks;
    @Column(nullable = false) public int importance = 3;   // 1..5
    @Column(columnDefinition = "text") public String notes;
    @Column(nullable = false) public Instant createdAt = Instant.now();
}
