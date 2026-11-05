package com.studymind.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studymind.backend.dto.Dtos.*;
import com.studymind.backend.exception.ApiException;
import com.studymind.backend.model.Quiz;
import com.studymind.backend.model.QuizAttempt;
import com.studymind.backend.model.QuizQuestion;
import com.studymind.backend.repository.QuizAttemptRepository;
import com.studymind.backend.repository.QuizQuestionRepository;
import com.studymind.backend.repository.QuizRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizService {
    private final QuizRepository quizzes;
    private final QuizQuestionRepository questions;
    private final QuizAttemptRepository attempts;
    private final ObjectMapper mapper;
    private final ActivityService activity;

    public QuizService(QuizRepository quizzes, QuizQuestionRepository questions, QuizAttemptRepository attempts,
                       ObjectMapper mapper, ActivityService activity) {
        this.quizzes = quizzes;
        this.questions = questions;
        this.attempts = attempts;
        this.mapper = mapper;
        this.activity = activity;
    }

    private Quiz get(UUID userId, UUID id) {
        return quizzes.findByIdAndUserId(id, userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Quiz not found"));
    }

    private List<String> options(QuizQuestion q) {
        try {
            return mapper.readValue(q.optionsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupt quiz options", e);
        }
    }

    public QuizResponse toResponse(UUID userId, Quiz q) {
        Integer last = attempts.findFirstByQuizIdAndUserIdOrderByTakenAtDesc(q.id, userId)
            .map(a -> a.total == 0 ? 0 : Math.round(a.score * 100f / a.total)).orElse(null);
        return new QuizResponse(q.id, q.title, q.documentId, q.difficulty, (int) questions.countByQuizId(q.id), last, q.createdAt);
    }

    public List<QuizResponse> list(UUID userId) {
        return quizzes.findByUserIdOrderByCreatedAtDesc(userId).stream().map(q -> toResponse(userId, q)).toList();
    }

    /** The questions without the answers, so the browser can't peek. */
    public QuizDetail detail(UUID userId, UUID id) {
        Quiz q = get(userId, id);
        List<QuestionDto> qs = questions.findByQuizIdOrderByOrderIndexAsc(id).stream()
            .map(x -> new QuestionDto(x.id, x.prompt, options(x), x.page)).toList();
        return new QuizDetail(q.id, q.title, q.documentId, q.difficulty, qs);
    }

    /** @param answers chosen option index per question, in order (null = skipped) */
    public AttemptResponse submit(UUID userId, UUID id, List<Integer> answers) {
        Quiz quiz = get(userId, id);
        List<QuizQuestion> qs = questions.findByQuizIdOrderByOrderIndexAsc(id);
        if (answers.size() != qs.size()) throw new ApiException(HttpStatus.BAD_REQUEST, "Send one answer per question (" + qs.size() + ")");

        List<QuestionResult> results = new ArrayList<>();
        int score = 0;
        for (int i = 0; i < qs.size(); i++) {
            QuizQuestion q = qs.get(i);
            Integer chosen = answers.get(i);
            boolean ok = chosen != null && chosen == q.correctIndex;
            if (ok) score++;
            results.add(new QuestionResult(q.id, chosen, q.correctIndex, ok, q.explanation, q.page));
        }
        QuizAttempt a = new QuizAttempt();
        a.quizId = id;
        a.userId = userId;
        a.score = score;
        a.total = qs.size();
        try {
            a.answersJson = mapper.writeValueAsString(answers);
        } catch (JsonProcessingException e) {
            a.answersJson = null;
        }
        a = attempts.save(a);
        activity.log(userId, "QUIZ", quiz.title, "Scored " + score + "/" + qs.size());
        int percent = qs.isEmpty() ? 0 : Math.round(score * 100f / qs.size());
        return new AttemptResponse(a.id, score, qs.size(), percent, results);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Quiz q = get(userId, id);
        attempts.deleteByQuizId(id);
        questions.deleteByQuizId(id);
        quizzes.delete(q);
    }
}
