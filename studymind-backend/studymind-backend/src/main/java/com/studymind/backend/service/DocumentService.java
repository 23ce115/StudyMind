package com.studymind.backend.service;

import com.studymind.backend.dto.Dtos.*;
import com.studymind.backend.exception.ApiException;
import com.studymind.backend.logic.TextChunker;
import com.studymind.backend.model.Document;
import com.studymind.backend.model.DocumentChunk;
import com.studymind.backend.model.Subject;
import com.studymind.backend.repository.DocumentChunkRepository;
import com.studymind.backend.repository.DocumentRepository;
import com.studymind.backend.repository.SubjectRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final String SUMMARY_SYSTEM = "You help students revise. Summarise the student's notes using ONLY what the notes say. "
        + "Write markdown: a 2-3 sentence overview, then '## Key points' as bullets, then '## Important terms' as bullets (term: meaning). "
        + "Notes contain page markers like [p.12]; you may mention pages in brackets.";

    /** Text of a document ready to hand to the AI. */
    public record Sample(String text, boolean truncated) {}

    private final DocumentRepository documents;
    private final DocumentChunkRepository chunks;
    private final SubjectRepository subjectRepo;
    private final SubjectService subjects;
    private final PdfService pdf;
    private final SupabaseStorageService storage;
    private final AiService ai;
    private final ActivityService activity;

    public DocumentService(DocumentRepository documents, DocumentChunkRepository chunks, SubjectRepository subjectRepo,
                           SubjectService subjects, PdfService pdf, SupabaseStorageService storage, AiService ai, ActivityService activity) {
        this.documents = documents;
        this.chunks = chunks;
        this.subjectRepo = subjectRepo;
        this.subjects = subjects;
        this.pdf = pdf;
        this.storage = storage;
        this.ai = ai;
        this.activity = activity;
    }

    public Document get(UUID userId, UUID id) {
        return documents.findByIdAndUserId(id, userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    public List<DocumentResponse> list(UUID userId) {
        Map<UUID, String> names = subjectRepo.findByUserIdOrderByNameAsc(userId).stream().collect(Collectors.toMap(s -> s.id, s -> s.name));
        return documents.findByUserIdOrderByCreatedAtDesc(userId).stream().map(d -> toResponse(d, names.get(d.subjectId))).toList();
    }

    public DocumentResponse toResponse(Document d, String subjectName) {
        return new DocumentResponse(d.id, d.name, d.subjectId, subjectName, d.status, d.sizeBytes, d.pageCount, d.createdAt, d.summary != null);
    }

    public DocumentResponse toResponse(UUID userId, Document d) {
        String subject = d.subjectId == null ? null : subjectRepo.findByIdAndUserId(d.subjectId, userId).map(s -> s.name).orElse(null);
        return toResponse(d, subject);
    }

    /** Reads the file, splits it into searchable chunks and stores everything. */
    @Transactional
    public Document upload(UUID userId, MultipartFile file, UUID subjectId) {
        if (file == null || file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Please choose a file to upload");
        String name = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank() ? "document.pdf" : file.getOriginalFilename();
        String lower = name.toLowerCase(Locale.ROOT);
        if (!(lower.endsWith(".pdf") || lower.endsWith(".txt") || lower.endsWith(".md"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only PDF, TXT and MD files are supported for now");
        }
        Subject subject = subjectId == null ? null : subjects.get(userId, subjectId);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Couldn't read the uploaded file");
        }
        List<String> pages = pdf.extractPages(bytes, name);
        List<TextChunker.Chunk> pieces = TextChunker.chunk(pages, 900, 120);
        int totalChars = pages.stream().mapToInt(p -> TextChunker.normalize(p).length()).sum();

        Document d = new Document();
        d.userId = userId;
        d.subjectId = subject == null ? null : subject.id;
        d.name = name;
        d.sizeBytes = file.getSize();
        d.pageCount = pages.size();
        // scanned PDFs have (almost) no text layer: flag them so the UI can say "needs review"
        d.status = pieces.isEmpty() || totalChars < 200 || totalChars / Math.max(1, pages.size()) < 60 ? "low_confidence" : "ready";
        d = documents.save(d);

        List<DocumentChunk> rows = new ArrayList<>(pieces.size());
        for (TextChunker.Chunk c : pieces) {
            DocumentChunk row = new DocumentChunk();
            row.documentId = d.id;
            row.userId = userId;
            row.pageNumber = c.page();
            row.chunkIndex = c.index();
            row.content = c.text();
            rows.add(row);
        }
        chunks.saveAll(rows);

        if (storage.isConfigured()) {   // keeping the original file is a nice-to-have, never a reason to fail
            try {
                String path = userId + "/" + d.id + "-" + name.replaceAll("[^A-Za-z0-9._-]", "_");
                storage.upload(path, bytes, file.getContentType());
                d.storagePath = path;
                d = documents.save(d);
            } catch (RuntimeException e) {
                log.warn("Could not store original file in Supabase Storage: {}", e.getMessage());
            }
        }
        activity.log(userId, "DOCUMENT", name, "Uploaded a document");
        return d;
    }

    public Document setSubject(UUID userId, UUID id, UUID subjectId) {
        Document d = get(userId, id);
        d.subjectId = subjectId == null ? null : subjects.get(userId, subjectId).id;
        return documents.save(d);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Document d = get(userId, id);
        chunks.deleteByDocumentId(id);
        if (d.storagePath != null && storage.isConfigured()) {
            try { storage.delete(d.storagePath); } catch (RuntimeException e) { log.warn("Could not delete stored file: {}", e.getMessage()); }
        }
        documents.delete(d);
    }

    public SummaryResponse summarize(UUID userId, UUID id, boolean refresh) {
        Document d = get(userId, id);
        if (!refresh && d.summary != null) return new SummaryResponse(d.summary);
        Sample s = sample(d.id, 18000);
        if (s.text().isBlank()) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "This document has no readable text to summarise");
        d.summary = ai.complete(SUMMARY_SYSTEM, "Notes from \"" + d.name + "\":\n\n" + s.text());
        documents.save(d);
        activity.log(userId, "SUMMARY", d.name, "Summarised a document");
        return new SummaryResponse(d.summary);
    }

    /**
     * The document's text with [p.N] markers, capped at maxChars. Long documents are sampled evenly
     * (every k-th chunk) so the AI sees the whole range of topics, not just the first chapters.
     */
    public Sample sample(UUID documentId, int maxChars) {
        List<DocumentChunk> all = chunks.findByDocumentIdOrderByChunkIndexAsc(documentId);
        int total = all.stream().mapToInt(c -> c.content.length() + 10).sum();
        boolean truncated = total > maxChars;
        int step = truncated ? (int) Math.ceil((double) total / maxChars) : 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < all.size(); i += step) {
            DocumentChunk c = all.get(i);
            sb.append("[p.").append(c.pageNumber).append("] ").append(c.content).append("\n\n");
            if (sb.length() > maxChars + 2000) break;
        }
        return new Sample(sb.toString(), truncated);
    }
}
