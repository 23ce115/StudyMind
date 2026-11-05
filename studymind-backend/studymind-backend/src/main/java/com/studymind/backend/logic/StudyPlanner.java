package com.studymind.backend.logic;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turns upcoming exams into a day-by-day study schedule.
 * An exam's share of a day's study time grows with its importance and the student's weakness in that subject,
 * and with how close the exam is (1 / days left). Time is handed out in 30-minute blocks (largest-remainder method).
 */
public final class StudyPlanner {
    private StudyPlanner() {}

    /** weakness: 0 (strong) .. 1 (weak). importance: 1..5. */
    public record ExamIn(String id, String subject, LocalDate date, int importance, double weakness) {}
    public record Item(String subject, String examId, double hours, String focus) {}
    public record Day(LocalDate date, double capacityHours, List<Item> items) {}
    public record SubjectSummary(String subject, String examId, LocalDate examDate, int daysLeft, double hours, double weakness, String priority) {}
    public record Plan(List<Day> days, List<SubjectSummary> subjects) {}

    /**
     * @param dailyHours     hours the student wants to study on a normal day
     * @param classHoursByDay hours of lectures per weekday, index 0 = Monday (may be null)
     */
    public static Plan plan(LocalDate today, List<ExamIn> exams, double dailyHours, double[] classHoursByDay, int maxDays) {
        List<ExamIn> upcoming = exams.stream().filter(e -> !e.date().isBefore(today))
                .sorted(Comparator.comparing(ExamIn::date)).toList();
        List<ExamIn> allocatable = upcoming.stream().filter(e -> e.date().isAfter(today)).toList();
        if (allocatable.isEmpty()) return new Plan(List.of(), summaries(upcoming, today, new double[upcoming.size()]));

        LocalDate last = allocatable.get(allocatable.size() - 1).date().minusDays(1);
        LocalDate horizon = last.isBefore(today.plusDays(maxDays - 1L)) ? last : today.plusDays(maxDays - 1L);
        double[] total = new double[upcoming.size()];
        List<Day> days = new ArrayList<>();

        for (LocalDate d = today; !d.isAfter(horizon); d = d.plusDays(1)) {
            double cap = capacity(d, dailyHours, classHoursByDay);
            List<ExamIn> active = new ArrayList<>();
            List<Double> urgency = new ArrayList<>();
            double sum = 0;
            for (ExamIn e : allocatable) {
                if (!e.date().isAfter(d)) continue;
                long left = ChronoUnit.DAYS.between(d, e.date());
                double u = Math.max(1, e.importance()) * (1 + clamp(e.weakness())) / left;
                active.add(e); urgency.add(u); sum += u;
            }
            if (active.isEmpty()) continue;

            int blocks = (int) Math.round(cap * 2);
            int[] given = new int[active.size()];
            double[] frac = new double[active.size()];
            int used = 0;
            for (int i = 0; i < active.size(); i++) {
                double raw = blocks * urgency.get(i) / sum;
                given[i] = (int) Math.floor(raw); frac[i] = raw - given[i]; used += given[i];
            }
            for (int r = blocks - used; r > 0; r--) {
                int best = 0;
                for (int i = 1; i < frac.length; i++) if (frac[i] > frac[best]) best = i;
                given[best]++; frac[best] = -1;
            }
            List<Item> items = new ArrayList<>();
            for (int i = 0; i < active.size(); i++) {
                if (given[i] == 0) continue;
                ExamIn e = active.get(i);
                double hours = given[i] / 2.0;
                items.add(new Item(e.subject(), e.id(), hours, focus((int) ChronoUnit.DAYS.between(d, e.date()))));
                total[upcoming.indexOf(e)] += hours;
            }
            items.sort(Comparator.comparingDouble(Item::hours).reversed());
            days.add(new Day(d, cap, items));
        }
        return new Plan(days, summaries(upcoming, today, total));
    }

    static double capacity(LocalDate d, double dailyHours, double[] classHours) {
        double ch = (classHours == null || classHours.length < 7) ? 0 : classHours[d.getDayOfWeek().getValue() - 1];
        double cap = ch >= 6 ? dailyHours * 0.6 : ch >= 4 ? dailyHours * 0.8 : dailyHours;
        if (d.getDayOfWeek() == DayOfWeek.SUNDAY) cap = Math.max(cap, dailyHours); // free day: full time
        return Math.max(1.0, Math.floor(cap * 2) / 2.0);
    }

    static String focus(int daysLeft) {
        if (daysLeft <= 1) return "Last-minute revision: key points and a quick quiz";
        if (daysLeft <= 3) return "Revise everything and practise with a quiz";
        if (daysLeft <= 7) return "Revise notes and go through flashcards";
        return "Learn and understand the topics in depth";
    }

    private static List<SubjectSummary> summaries(List<ExamIn> upcoming, LocalDate today, double[] total) {
        List<SubjectSummary> out = new ArrayList<>();
        for (int i = 0; i < upcoming.size(); i++) {
            ExamIn e = upcoming.get(i);
            int left = (int) ChronoUnit.DAYS.between(today, e.date());
            double w = clamp(e.weakness());
            String p = left == 0 ? "TODAY" : (left <= 7 || (w >= 0.5 && left <= 14)) ? "HIGH" : (left > 21 && w < 0.3) ? "LOW" : "MEDIUM";
            out.add(new SubjectSummary(e.subject(), e.id(), e.date(), left, total[i], w, p));
        }
        return out;
    }

    private static double clamp(double v) { return Math.max(0, Math.min(1, v)); }
}
