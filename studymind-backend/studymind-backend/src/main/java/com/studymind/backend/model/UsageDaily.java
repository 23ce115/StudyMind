package com.studymind.backend.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "usage_daily")
public class UsageDaily {
    @Id @GeneratedValue public UUID id;
    @Column(nullable = false) public UUID userId;
    @Column(nullable = false) public LocalDate usageDate;
    @Column(nullable = false) public int questionsAsked = 0;
}
