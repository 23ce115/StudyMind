package com.studymind.backend.service;

import com.studymind.backend.logic.QueryBuilder;
import com.studymind.backend.model.Document;
import com.studymind.backend.model.DocumentChunk;
import com.studymind.backend.repository.DocumentChunkRepository;
import com.studymind.backend.repository.DocumentRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/** Finds the passages of the student's own documents that best match a question (Postgres full-text search). */
@Service
public class RetrievalService {
    public record Passage(UUID documentId, String documentName, int page, String text) {}

    private final DocumentRepository documents;
    private final DocumentChunkRepository chunks;

    public RetrievalService(DocumentRepository documents, DocumentChunkRepository chunks) {
        this.documents = documents;
        this.chunks = chunks;
    }

    /** Scope: one document, or one subject, or (both null) everything the student has uploaded. */
    public List<Passage> retrieve(UUID userId, String question, UUID documentId, UUID subjectId, int limit) {
        List<Document> docs;
        if (documentId != null) docs = documents.findByIdAndUserId(documentId, userId).map(List::of).orElse(List.of());
        else if (subjectId != null) docs = documents.findByUserIdAndSubjectId(userId, subjectId);
        else docs = documents.findByUserIdOrderByCreatedAtDesc(userId);
        docs = docs.stream().filter(d -> !"processing".equals(d.status)).toList();
        if (docs.isEmpty()) return List.of();

        Map<UUID, Document> byId = docs.stream().collect(Collectors.toMap(d -> d.id, Function.identity()));
        String tsQuery = QueryBuilder.toTsQuery(question);
        List<DocumentChunk> found = tsQuery.isEmpty() ? List.of() : chunks.search(byId.keySet(), tsQuery, limit);
        if (found.isEmpty()) {   // no keyword hit: fall back to the start of the documents
            found = chunks.findByDocumentIdIn(byId.keySet(), PageRequest.of(0, limit, Sort.by("documentId", "chunkIndex")));
        }
        return found.stream().map(c -> new Passage(c.documentId, byId.get(c.documentId).name, c.pageNumber, c.content)).toList();
    }
}
