package com.studymind.backend.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "timetable_entries")
public class TimetableEntry {
    @Id @GeneratedValue public UUID id;
    @Column(nullable = false) public UUID userId;
    @Column(nullable = false) public UUID subjectId;
    @Column(nullable = false) public int dayOfWeek;   // 1 = Monday .. 7 = Sunday
    @Column(nullable = false) public LocalTime startTime;
    @Column(nullable = false) public LocalTime endTime;
    public String location;
}
