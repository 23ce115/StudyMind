package com.studymind.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.studymind.backend.dto.Dtos.*;
import com.studymind.backend.exception.ApiException;
import com.studymind.backend.logic.TimetableParser;
import com.studymind.backend.logic.TimetableParser.Slot;
import com.studymind.backend.model.Subject;
import com.studymind.backend.model.Timetable;
import com.studymind.backend.model.TimetableEntry;
import com.studymind.backend.repository.SubjectRepository;
import com.studymind.backend.repository.TimetableEntryRepository;
import com.studymind.backend.repository.TimetableRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** The student types their timetable in plain words; the AI (or a built-in reader) turns it into subjects and class slots. */
@Service
public class TimetableService {
    private static final Logger log = LoggerFactory.getLogger(TimetableService.class);
    static final String[] DAY_NAMES = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private static final String SYSTEM = """
        Extract the weekly class timetable from the student's text. Ignore breaks, lunch and free periods.
        Reply with JSON only: {"entries": [{"subject": "full subject name as written", "day": 1-7 (1=Monday ... 7=Sunday), "start": "HH:MM" (24h), "end": "HH:MM" (24h), "location": "room or null"}]}
        If a time has no am/pm, assume class hours between 08:00 and 18:00. If a line lists several days (e.g. "Mon/Wed"), create one entry per day.
        """;

    private final TimetableEntryRepository entries;
    private final TimetableRepository timetables;
    private final SubjectRepository subjectRepo;
    private final SubjectService subjects;
    private final AiService ai;
    private final ActivityService activity;
    private final TransactionTemplate tx;

    public TimetableService(TimetableEntryRepository entries, TimetableRepository timetables, SubjectRepository subjectRepo,
                            SubjectService subjects, AiService ai, ActivityService activity, TransactionTemplate tx) {
        this.entries = entries;
        this.timetables = timetables;
        this.subjectRepo = subjectRepo;
        this.subjects = subjects;
        this.ai = ai;
        this.activity = activity;
        this.tx = tx;
    }

    public TimetableResponse get(UUID userId) {
        String text = timetables.findById(userId).map(t -> t.rawText).orElse("");
        return view(userId, text, "saved");
    }

    /** Replaces the student's timetable with whatever can be read from {@code text}. */
    public TimetableResponse save(UUID userId, String text) {
        String by = "rules";
        List<Slot> slots = List.of();
        if (ai.isConfigured()) {
            try {
                slots = fromAi(text);
                by = "ai";
            } catch (ApiException e) {
                log.info("AI timetable reading failed ({}), using the built-in reader", e.getMessage());
            }
        }
        if (slots.isEmpty()) {
            slots = TimetableParser.parse(text);
            by = "rules";
        }
        if (slots.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "I couldn't find any classes in that text. Try e.g. \"Monday: 9-10 DBMS, 10-11 Operating Systems\" (one line per day).");
        }
        final List<Slot> found = slots;
        tx.execute(status -> {
            entries.deleteByUserId(userId);
            Map<String, Subject> cache = new HashMap<>();
            for (Slot s : found) {
                Subject subject = cache.computeIfAbsent(s.subject().toLowerCase(Locale.ROOT), k -> subjects.findOrCreate(userId, s.subject()));
                TimetableEntry e = new TimetableEntry();
                e.userId = userId;
                e.subjectId = subject.id;
                e.dayOfWeek = s.day();
                e.startTime = s.start();
                e.endTime = s.end();
                e.location = s.location();
                entries.save(e);
            }
            Timetable t = timetables.findById(userId).orElseGet(Timetable::new);
            t.userId = userId;
            t.rawText = text;
            t.updatedAt = Instant.now();
            timetables.save(t);
            return null;
        });
        activity.log(userId, "TIMETABLE", "Timetable updated", found.size() + " classes found");
        return view(userId, text, by);
    }

    /** Lecture hours for each weekday (index 0 = Monday); the study planner uses this. */
    public double[] classHoursByDay(UUID userId) {
        double[] hours = new double[7];
        for (TimetableEntry e : entries.findByUserIdOrderByDayOfWeekAscStartTimeAsc(userId)) {
            hours[e.dayOfWeek - 1] += Duration.between(e.startTime, e.endTime).toMinutes() / 60.0;
        }
        return hours;
    }

    private List<Slot> fromAi(String text) {
        JsonNode j = ai.completeJson(SYSTEM, text);
        List<Slot> out = new ArrayList<>();
        JsonNode arr = j.isArray() ? j : j.path("entries");
        for (JsonNode n : arr) {
            String subject = SubjectService.cleanName(n.path("subject").asText(""));
            int day = n.path("day").asInt(0);
            if (subject.isEmpty() || day < 1 || day > 7) continue;
            try {
                LocalTime start = LocalTime.parse(n.path("start").asText(""));
                LocalTime end = LocalTime.parse(n.path("end").asText(""));
                if (!end.isAfter(start)) continue;
                String loc = n.path("location").isTextual() && !n.path("location").asText().isBlank() ? n.path("location").asText().trim() : null;
                out.add(new Slot(day, start, end, subject, loc));
            } catch (java.time.format.DateTimeParseException ignored) {
                // skip a malformed row instead of failing the whole timetable
            }
        }
        return out;
    }

    private TimetableResponse view(UUID userId, String text, String parsedBy) {
        Map<UUID, Subject> byId = new HashMap<>();
        for (Subject s : subjectRepo.findByUserIdOrderByNameAsc(userId)) byId.put(s.id, s);
        List<TimetableEntry> rows = entries.findByUserIdOrderByDayOfWeekAscStartTimeAsc(userId);

        List<TimetableEntryDto> dtos = new ArrayList<>();
        Map<UUID, int[]> counts = new LinkedHashMap<>();       // subject -> [classes, minutes]
        double[] byDay = new double[7];
        for (TimetableEntry e : rows) {
            Subject s = byId.get(e.subjectId);
            String name = s == null ? "Unknown" : s.name;
            dtos.add(new TimetableEntryDto(e.id, e.dayOfWeek, DAY_NAMES[e.dayOfWeek - 1], e.startTime.toString(), e.endTime.toString(), e.subjectId, name, e.location));
            int minutes = (int) Duration.between(e.startTime, e.endTime).toMinutes();
            int[] c = counts.computeIfAbsent(e.subjectId, k -> new int[2]);
            c[0]++;
            c[1] += minutes;
            byDay[e.dayOfWeek - 1] += minutes / 60.0;
        }
        List<SubjectLoad> loads = new ArrayList<>();
        double total = 0;
        for (Map.Entry<UUID, int[]> en : counts.entrySet()) {
            Subject s = byId.get(en.getKey());
            double hours = Math.round(en.getValue()[1] / 60.0 * 100) / 100.0;
            total += hours;
            loads.add(new SubjectLoad(en.getKey(), s == null ? "Unknown" : s.name, s == null ? null : s.color, en.getValue()[0], hours));
        }
        return new TimetableResponse(text, dtos, loads, loads.size(), Math.round(total * 100) / 100.0, byDay, parsedBy);
    }
}
