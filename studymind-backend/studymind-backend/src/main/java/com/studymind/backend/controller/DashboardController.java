package com.studymind.backend.controller;

import com.studymind.backend.dto.Dtos.*;
import com.studymind.backend.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.studymind.backend.service.DashboardService;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private final DashboardService dashboard;
    private final CurrentUser me;

    public DashboardController(DashboardService dashboard, CurrentUser me) { this.dashboard = dashboard; this.me = me; }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() { return dashboard.build(me.id()); }

    /** Log time spent studying, e.g. {"minutes": 25}. Feeds the "minutes studied today" bar and the streak. */
    @PostMapping("/study-sessions")
    public ResponseEntity<Void> session(@Valid @RequestBody StudySessionRequest req) {
        dashboard.logStudySession(me.id(), req.minutes());
        return ResponseEntity.noContent().build();
    }
}
