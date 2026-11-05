package com.studymind.backend.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk {
    @Id @GeneratedValue public UUID id;
    @Column(nullable = false) public UUID documentId;
    @Column(nullable = false) public UUID userId;
    @Column(nullable = false) public int pageNumber;
    @Column(nullable = false) public int chunkIndex;
    @Column(nullable = false, columnDefinition = "text") public String content;
}
