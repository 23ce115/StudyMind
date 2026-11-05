package com.studymind.backend.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message {
    @Id @GeneratedValue public UUID id;
    @Column(nullable = false) public UUID chatId;
    @Column(nullable = false) public String role;            // user | assistant
    public String kind = "plain";                            // plain | intro | synthesis
    @Column(columnDefinition = "text") public String text;
    @Column(columnDefinition = "text") public String heading;
    @Column(columnDefinition = "text") public String body;
    public String snippetLabel;
    @Column(columnDefinition = "text") public String snippetCode;
    @Column(columnDefinition = "text") public String snippetExplanation;
    @Column(columnDefinition = "text") public String citation;
    @Column(nullable = false) public Instant createdAt = Instant.now();
}
