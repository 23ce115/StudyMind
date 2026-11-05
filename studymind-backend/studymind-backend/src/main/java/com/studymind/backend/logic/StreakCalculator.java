package com.studymind.backend.logic;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

/** Study streak from the set of days that had any study activity. */
public final class StreakCalculator {
    private StreakCalculator() {}

    /** @param week Monday..Sunday of the current week: was the student active that day? */
    public record Streak(int current, int best, boolean[] week) {}

    public static Streak compute(Set<LocalDate> days, LocalDate today) {
        int current = 0;
        LocalDate d = days.contains(today) ? today : today.minusDays(1); // a missed "today" doesn't break the streak yet
        while (days.contains(d)) { current++; d = d.minusDays(1); }

        int best = 0, run = 0;
        LocalDate prev = null;
        for (LocalDate x : new TreeSet<>(days)) {
            run = (prev != null && prev.plusDays(1).equals(x)) ? run + 1 : 1;
            best = Math.max(best, run);
            prev = x;
        }

        boolean[] week = new boolean[7];
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        for (int i = 0; i < 7; i++) week[i] = days.contains(monday.plusDays(i));
        return new Streak(current, Math.max(best, current), week);
    }
}
