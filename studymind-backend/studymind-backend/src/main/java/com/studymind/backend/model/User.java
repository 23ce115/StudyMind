package com.studymind.backend.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue public UUID id;
    @Column(nullable = false, unique = true) public String email;
    @Column(nullable = false) public String passwordHash;
    @Column(nullable = false) public String name;
    public String program;
    public String avatarUrl;
    @Column(nullable = false) public int dailyGoalMinutes = 120;
    @Column(nullable = false) public Instant createdAt = Instant.now();
}
