package com.studymind.backend.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {
    @Id @GeneratedValue public UUID id;
    @Column(nullable = false) public UUID quizId;
    @Column(nullable = false) public int orderIndex;
    @Column(nullable = false, columnDefinition = "text") public String prompt;
    @Column(nullable = false, columnDefinition = "text") public String optionsJson;   // JSON array of 4 strings
    @Column(nullable = false) public int correctIndex;
    @Column(columnDefinition = "text") public String explanation;
    public Integer page;
}
