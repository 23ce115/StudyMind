package com.studymind.backend.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "study_plans")
public class StudyPlan {
    @Id @GeneratedValue public UUID id;
    @Column(nullable = false) public UUID userId;
    @Column(nullable = false, columnDefinition = "text") public String planJson;
    @Column(nullable = false) public Instant generatedAt = Instant.now();
}
