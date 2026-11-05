package com.studymind.backend.logic;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule-based timetable reader used when no AI key is configured (or the AI reply can't be parsed).
 * Understands lines such as:
 *   Monday: 9-10 DBMS, 10-11 Operating Systems (Room 204)
 *   Tue 09:00-10:30 Machine Learning
 *   or a day heading on its own line followed by "9:00-10:00 DBMS" lines.
 */
public final class TimetableParser {
    private TimetableParser() {}

    /** day: 1 = Monday .. 7 = Sunday */
    public record Slot(int day, LocalTime start, LocalTime end, String subject, String location) {}

    private static final Pattern DAY = Pattern.compile("^\\s*(mon|tue|wed|thu|fri|sat|sun)[a-z]*\\b\\s*[:\\-]?\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RANGE = Pattern.compile(
        "^\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\s*(?:-|\u2013|to)\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\s*[:\\-\u2013]?\\s*(.+?)\\s*$",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern LOC = Pattern.compile("\\(([^)]+)\\)\\s*$");
    private static final String[] DAYS = {"mon", "tue", "wed", "thu", "fri", "sat", "sun"};

    public static List<Slot> parse(String text) {
        List<Slot> out = new ArrayList<>();
        if (text == null) return out;
        int day = 0;
        for (String line : text.split("\\R")) {
            String rest = line;
            Matcher d = DAY.matcher(line);
            if (d.matches()) {
                String key = line.trim().substring(0, 3).toLowerCase(Locale.ROOT);
                for (int i = 0; i < 7; i++) if (DAYS[i].equals(key)) day = i + 1;
                rest = d.group(2);
            }
            if (day == 0 || rest == null || rest.isBlank()) continue;
            for (String part : rest.split("[,;|]")) {
                Matcher m = RANGE.matcher(part);
                if (!m.matches()) continue;
                LocalTime start = time(m.group(1), m.group(2), m.group(3), false);
                LocalTime end = time(m.group(4), m.group(5), m.group(6), m.group(3) == null && m.group(6) == null);
                if (start == null || end == null) continue;
                if (!end.isAfter(start) && m.group(6) == null && end.getHour() < 12) end = end.plusHours(12);
                if (!end.isAfter(start)) continue;
                String subject = m.group(7).trim();
                String location = null;
                Matcher l = LOC.matcher(subject);
                if (l.find()) { location = l.group(1).trim(); subject = subject.substring(0, l.start()).trim(); }
                String low = subject.toLowerCase(Locale.ROOT);
                if (subject.isEmpty() || low.contains("lunch") || low.contains("break") || low.contains("recess")) continue;
                out.add(new Slot(day, start, end, subject, location));
            }
        }
        return out;
    }

    /** No am/pm given: hours 1-6 are assumed to be afternoon (classes rarely start at 3 a.m.). */
    private static LocalTime time(String h, String min, String ampm, boolean guess) {
        int hour = Integer.parseInt(h), minute = min == null ? 0 : Integer.parseInt(min);
        if (hour > 23 || minute > 59) return null;
        if (ampm != null) {
            boolean pm = ampm.equalsIgnoreCase("pm");
            if (hour == 12) hour = pm ? 12 : 0; else if (pm) hour += 12;
        } else if (hour >= 1 && hour <= 6) {
            hour += 12;
        }
        return LocalTime.of(hour, minute);
    }
}
