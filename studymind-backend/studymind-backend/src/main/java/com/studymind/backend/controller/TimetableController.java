package com.studymind.backend.controller;

import com.studymind.backend.dto.Dtos.*;
import com.studymind.backend.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.studymind.backend.service.TimetableService;

@RestController
@RequestMapping("/api/timetable")
public class TimetableController {
    private final TimetableService timetable;
    private final CurrentUser me;

    public TimetableController(TimetableService timetable, CurrentUser me) { this.timetable = timetable; this.me = me; }

    @GetMapping
    public TimetableResponse get() { return timetable.get(me.id()); }

    /** Body: {"text": "Monday: 9-10 DBMS, 10-11 OS ..."}. Replaces the whole timetable and returns the analysis. */
    @PutMapping
    public TimetableResponse save(@Valid @RequestBody TimetableRequest req) { return timetable.save(me.id(), req.text()); }
}
