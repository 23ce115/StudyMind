package com.studymind.backend.config;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** One place that knows the app's time zone, so "today" and streaks mean the student's today. */
@Component
public class AppTime {
    private final ZoneId zone;

    public AppTime(@Value("${app.timezone}") String zone) {
        this.zone = ZoneId.of(zone);
    }

    public ZoneId zone() { return zone; }
    public LocalDate today() { return LocalDate.now(zone); }
    public LocalDate dateOf(Instant i) { return i.atZone(zone).toLocalDate(); }
    public Instant startOfDay(LocalDate d) { return d.atStartOfDay(zone).toInstant(); }
}
