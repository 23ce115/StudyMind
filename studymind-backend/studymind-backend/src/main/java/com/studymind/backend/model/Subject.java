package com.studymind.backend.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "subjects")
public class Subject {
    @Id @GeneratedValue public UUID id;
    @Column(nullable = false) public UUID userId;
    @Column(nullable = false) public String name;
    public String color;
    @Column(nullable = false) public Instant createdAt = Instant.now();
}
