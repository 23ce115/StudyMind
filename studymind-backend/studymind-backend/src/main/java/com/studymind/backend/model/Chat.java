package com.studymind.backend.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "chats")
public class Chat {
    @Id @GeneratedValue public UUID id;
    @Column(nullable = false) public UUID userId;
    @Column(nullable = false) public String title = "New chat";
    public UUID documentId;
    public UUID subjectId;
    @Column(nullable = false) public Instant createdAt = Instant.now();
    @Column(nullable = false) public Instant updatedAt = Instant.now();
}
