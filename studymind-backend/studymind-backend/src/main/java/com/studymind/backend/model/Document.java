package com.studymind.backend.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document {
    @Id @GeneratedValue public UUID id;
    @Column(nullable = false) public UUID userId;
    public UUID subjectId;
    @Column(nullable = false) public String name;
    public String storagePath;
    public Long sizeBytes;
    public Integer pageCount;
    @Column(nullable = false) public String status = "processing";   // ready | processing | low_confidence
    @Column(columnDefinition = "text") public String summary;
    public Integer lastPage;
    public Instant lastStudiedAt;
    @Column(nullable = false) public Instant createdAt = Instant.now();
}
