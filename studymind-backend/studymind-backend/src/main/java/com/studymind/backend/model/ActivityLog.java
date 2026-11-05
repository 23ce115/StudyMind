package com.studymind.backend.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "activity_log")
public class ActivityLog {
    @Id @GeneratedValue public UUID id;
    @Column(nullable = false) public UUID userId;
    @Column(nullable = false) public String type;
    @Column(nullable = false) public String title;
    public String detail;
    @Column(nullable = false) public Instant createdAt = Instant.now();
}
