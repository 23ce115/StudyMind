package com.studymind.backend.controller;

import com.studymind.backend.dto.Dtos.*;
import com.studymind.backend.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.studymind.backend.service.StudyPlanService;

@RestController
@RequestMapping("/api/study-plan")
public class StudyPlanController {
    private final StudyPlanService plans;
    private final CurrentUser me;

    public StudyPlanController(StudyPlanService plans, CurrentUser me) { this.plans = plans; this.me = me; }

    /** The last generated plan, or 204 if there isn't one yet. */
    @GetMapping
    public ResponseEntity<StudyPlanResponse> latest() {
        return plans.latest(me.id()).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Builds a fresh plan from the current exams, timetable and past marks. */
    @PostMapping("/generate")
    public StudyPlanResponse generate() { return plans.generate(me.id()); }
}
