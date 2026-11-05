package com.studymind.backend.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LogicTest {

    @Test void chunkerKeepsPageNumbersAndOverlaps() {
        String sentence = "Normalization removes redundancy from relations. ";
        List<TextChunker.Chunk> c = TextChunker.chunk(List.of(sentence.repeat(60), "short", sentence.repeat(10)), 500, 80);
        assertTrue(c.size() > 4);
        assertEquals(1, c.get(0).page());
        assertEquals(3, c.get(c.size() - 1).page());          // page 2 is too short to keep
        assertTrue(c.stream().noneMatch(x -> x.page() == 2));
        assertTrue(c.stream().allMatch(x -> x.text().length() <= 500));
        for (int i = 0; i < c.size(); i++) assertEquals(i, c.get(i).index());
    }

    @Test void chunkerHandlesTextWithoutSpaces() {
        assertFalse(TextChunker.chunk(List.of("x".repeat(2000)), 500, 50).isEmpty());
    }

    @Test void tsQueryDropsStopwordsAndSymbols() {
        assertEquals("3nf | bcnf", QueryBuilder.toTsQuery("What's the main difference between 3NF and BCNF?"));
        assertEquals("", QueryBuilder.toTsQuery("what is the"));
        assertFalse(QueryBuilder.toTsQuery("drop table users; --'").contains(";"));
    }

    @Test void streakCountsConsecutiveDaysAndGraceForToday() {
        LocalDate today = LocalDate.of(2026, 10, 14); // Wednesday
        Set<LocalDate> days = Set.of(today.minusDays(1), today.minusDays(2), today.minusDays(3), today.minusDays(10), today.minusDays(11));
        StreakCalculator.Streak s = StreakCalculator.compute(days, today);
        assertEquals(3, s.current());       // yesterday still counts although today has no activity yet
        assertEquals(3, s.best());
        assertTrue(s.week()[0] && s.week()[1] && !s.week()[2]); // Mon, Tue yes; Wed (today) not yet
        assertEquals(0, StreakCalculator.compute(Set.of(today.minusDays(3)), today).current());
    }

    @Test void parsesTimetableLines() {
        String t = "Monday: 9-10 DBMS, 10-11 Operating Systems (Room 204)\nTue 09:00-10:30 Machine Learning\nWednesday\n2-3pm AI\n12-1 Lunch";
        List<TimetableParser.Slot> s = TimetableParser.parse(t);
        assertEquals(4, s.size());
        assertEquals("DBMS", s.get(0).subject());
        assertEquals(LocalTime.of(9, 0), s.get(0).start());
        assertEquals("Operating Systems", s.get(1).subject());
        assertEquals("Room 204", s.get(1).location());
        assertEquals(2, s.get(2).day());
        assertEquals(LocalTime.of(10, 30), s.get(2).end());
        assertEquals(LocalTime.of(14, 0), s.get(3).start());   // 2-3pm
    }

    @Test void afternoonHoursWithoutAmPmAreGuessed() {
        TimetableParser.Slot s = TimetableParser.parse("Fri 1-2 AI").get(0);
        assertEquals(LocalTime.of(13, 0), s.start());
        assertEquals(LocalTime.of(14, 0), s.end());
    }

    @Test void plannerFavoursCloserAndWeakerExams() {
        LocalDate today = LocalDate.of(2026, 10, 12);
        var near = new StudyPlanner.ExamIn("e1", "AI", today.plusDays(5), 3, 0.2);
        var far = new StudyPlanner.ExamIn("e2", "ML", today.plusDays(25), 3, 0.2);
        StudyPlanner.Plan p = StudyPlanner.plan(today, List.of(near, far), 4, null, 60);
        assertEquals(25, p.days().size());                         // today .. day before the last exam (inclusive)
        double dayOneAi = hours(p.days().get(0), "AI"), dayOneMl = hours(p.days().get(0), "ML");
        assertTrue(dayOneAi > dayOneMl);
        for (var d : p.days()) assertEquals(d.capacityHours(), d.items().stream().mapToDouble(StudyPlanner.Item::hours).sum(), 1e-9);
        assertTrue(p.days().get(10).items().stream().allMatch(i -> i.subject().equals("ML"))); // AI exam is over by then
        assertEquals("HIGH", p.subjects().get(0).priority());
        assertEquals("LOW", p.subjects().get(1).priority());
        var weak = new StudyPlanner.ExamIn("e3", "OS", today.plusDays(5), 3, 0.9);
        var a = StudyPlanner.plan(today, List.of(near, weak), 4, null, 60).days().get(0);
        assertTrue(hours(a, "OS") > hours(a, "AI"));
    }

    @Test void plannerWithNothingUpcomingIsEmptyAndClassDaysShrinkCapacity() {
        LocalDate today = LocalDate.of(2026, 10, 12); // Monday
        assertTrue(StudyPlanner.plan(today, List.of(new StudyPlanner.ExamIn("x", "A", today, 3, .5)), 4, null, 30).days().isEmpty());
        assertTrue(StudyPlanner.capacity(today, 4, new double[]{7, 0, 0, 0, 0, 0, 0}) < 4);
    }

    private static double hours(StudyPlanner.Day d, String subject) {
        return d.items().stream().filter(i -> i.subject().equals(subject)).mapToDouble(StudyPlanner.Item::hours).sum();
    }
}
