package com.studymind.backend.repository;

import com.studymind.backend.model.DocumentChunk;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {
    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);
    List<DocumentChunk> findByDocumentIdIn(Collection<UUID> documentIds, Pageable pageable);
    void deleteByDocumentId(UUID documentId);

    /** Full-text search (OR of the question's keywords), best match first. Uses the GIN index from schema.sql. */
    @Query(value = "select * from document_chunks c where c.document_id in (:docIds) "
        + "and to_tsvector('english', c.content) @@ to_tsquery('english', :q) "
        + "order by ts_rank(to_tsvector('english', c.content), to_tsquery('english', :q)) desc limit :lim", nativeQuery = true)
    List<DocumentChunk> search(@Param("docIds") Collection<UUID> docIds, @Param("q") String q, @Param("lim") int lim);
}
