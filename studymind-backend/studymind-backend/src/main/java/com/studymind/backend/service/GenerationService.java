package com.studymind.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studymind.backend.exception.ApiException;
import com.studymind.backend.model.Document;
import com.studymind.backend.model.Flashcard;
import com.studymind.backend.model.Quiz;
import com.studymind.backend.model.QuizQuestion;
import com.studymind.backend.repository.FlashcardRepository;
import com.studymind.backend.repository.QuizQuestionRepository;
import com.studymind.backend.repository.QuizRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** Turns a document into flashcards or a quiz using the AI. */
@Service
public class GenerationService {
    private static final String CARDS_SYSTEM = """
        You create study flashcards from a student's notes. Use ONLY facts that are in the notes.
        Each card: a precise question or term on the front, a short self-contained answer on the back (max 40 words).
        Cover the most important definitions, concepts, formulas and comparisons. No duplicates.
        Reply with JSON only: {"cards": [{"front": "...", "back": "..."}]}
        """;
    private static final String QUIZ_SYSTEM = """
        You write multiple-choice quizzes from a student's notes. Use ONLY facts that are in the notes.
        Every question has exactly 4 options, exactly one correct, and plausible wrong options. correctIndex is 0-3.
        explanation: 1-2 sentences saying why the answer is right. page: the page number from the nearest [p.N] marker, or null.
        Reply with JSON only: {"title": "short quiz title", "questions": [{"question": "...", "options": ["...","...","...","..."], "correctIndex": 0, "explanation": "...", "page": 3}]}
        """;

    private final DocumentService documents;
    private final FlashcardRepository flashcards;
    private final QuizRepository quizzes;
    private final QuizQuestionRepository questions;
    private final AiService ai;
    private final ObjectMapper mapper;
    private final ActivityService activity;
    private final TransactionTemplate tx;

    public GenerationService(DocumentService documents, FlashcardRepository flashcards, QuizRepository quizzes,
                             QuizQuestionRepository questions, AiService ai, ObjectMapper mapper, ActivityService activity, TransactionTemplate tx) {
        this.documents = documents;
        this.flashcards = flashcards;
        this.quizzes = quizzes;
        this.questions = questions;
        this.ai = ai;
        this.mapper = mapper;
        this.activity = activity;
        this.tx = tx;
    }

    private static int clamp(Integer v, int def, int min, int max) { return Math.max(min, Math.min(max, v == null ? def : v)); }

    private static String cut(String s, int max) {
        String t = s == null ? "" : s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    private DocumentService.Sample notesOf(Document d) {
        DocumentService.Sample s = documents.sample(d.id, 18000);
        if (s.text().isBlank()) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "This document has no readable text to work from");
        return s;
    }

    public List<Flashcard> generateFlashcards(UUID userId, UUID documentId, Integer count) {
        Document d = documents.get(userId, documentId);
        int n = clamp(count, 12, 5, 30);
        JsonNode j = ai.completeJson(CARDS_SYSTEM, "Create exactly " + n + " flashcards from the notes of \"" + d.name + "\".\n\nNOTES:\n" + notesOf(d).text());
        JsonNode arr = j.isArray() ? j : j.path("cards");

        List<Flashcard> out = new ArrayList<>();
        for (JsonNode c : arr) {
            String front = cut(c.path("front").asText(""), 900), back = cut(c.path("back").asText(""), 2500);
            if (front.isEmpty() || back.isEmpty()) continue;
            if (flashcards.existsByUserIdAndDocumentIdAndFrontIgnoreCase(userId, d.id, front)) continue;
            Flashcard f = new Flashcard();
            f.userId = userId;
            f.documentId = d.id;
            f.subjectId = d.subjectId;
            f.front = front;
            f.back = back;
            out.add(f);
            if (out.size() >= n) break;
        }
        if (out.isEmpty()) throw new ApiException(HttpStatus.BAD_GATEWAY, "The AI didn't produce any new flashcards. Try again.");
        List<Flashcard> saved = flashcards.saveAll(out);
        activity.log(userId, "FLASHCARDS", d.name, "Generated " + saved.size() + " flashcards");
        return saved;
    }

    public Quiz generateQuiz(UUID userId, UUID documentId, Integer count, String difficulty) {
        Document d = documents.get(userId, documentId);
        int n = clamp(count, 8, 3, 20);
        String level = difficulty == null ? "medium" : difficulty.toLowerCase(Locale.ROOT);
        if (!List.of("easy", "medium", "hard").contains(level)) level = "medium";
        JsonNode j = ai.completeJson(QUIZ_SYSTEM, "Write exactly " + n + " " + level + " multiple-choice questions from the notes of \""
            + d.name + "\".\n\nNOTES:\n" + notesOf(d).text());

        List<QuizQuestion> valid = new ArrayList<>();
        for (JsonNode q : j.path("questions")) {
            JsonNode opts = q.path("options");
            int correct = q.path("correctIndex").asInt(-1);
            String prompt = cut(q.path("question").asText(""), 1500);
            if (prompt.isEmpty() || !opts.isArray() || opts.size() != 4 || correct < 0 || correct > 3) continue;   // drop malformed questions
            List<String> options = new ArrayList<>();
            for (JsonNode o : opts) options.add(cut(o.asText(""), 500));
            if (options.stream().anyMatch(String::isEmpty)) continue;
            QuizQuestion qq = new QuizQuestion();
            qq.orderIndex = valid.size();
            qq.prompt = prompt;
            qq.optionsJson = toJson(options);
            qq.correctIndex = correct;
            qq.explanation = cut(q.path("explanation").asText(""), 1500);
            qq.page = q.path("page").isInt() ? q.path("page").asInt() : null;
            valid.add(qq);
            if (valid.size() >= n) break;
        }
        if (valid.isEmpty()) throw new ApiException(HttpStatus.BAD_GATEWAY, "The AI didn't produce a usable quiz. Try again.");

        Quiz quiz = new Quiz();
        quiz.userId = userId;
        quiz.documentId = d.id;
        quiz.subjectId = d.subjectId;
        quiz.title = cut(j.path("title").asText(""), 150).isEmpty() ? "Quiz: " + d.name : cut(j.path("title").asText(""), 150);
        quiz.difficulty = level;
        Quiz saved = tx.execute(status -> {
            Quiz s = quizzes.save(quiz);
            for (QuizQuestion qq : valid) qq.quizId = s.id;
            questions.saveAll(valid);
            return s;
        });
        activity.log(userId, "QUIZ_CREATED", d.name, "Created a " + valid.size() + "-question quiz");
        return saved;
    }

    private String toJson(List<String> options) {
        try {
            return mapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
