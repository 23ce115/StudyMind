package com.studymind.backend.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "timetables")
public class Timetable {
    @Id public UUID userId;
    @Column(columnDefinition = "text") public String rawText;
    @Column(nullable = false) public Instant updatedAt = Instant.now();
}
