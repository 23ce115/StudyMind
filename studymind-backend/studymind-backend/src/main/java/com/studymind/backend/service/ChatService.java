package com.studymind.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.studymind.backend.dto.Dtos.CreateChatRequest;
import com.studymind.backend.exception.ApiException;
import com.studymind.backend.model.Chat;
import com.studymind.backend.model.Document;
import com.studymind.backend.model.Message;
import com.studymind.backend.repository.ChatRepository;
import com.studymind.backend.repository.DocumentRepository;
import com.studymind.backend.repository.MessageRepository;
import com.studymind.backend.service.RetrievalService.Passage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {
    private static final String SYSTEM = """
        You are StudyMind, a study assistant for students (like NotebookLM). Answer ONLY from the numbered source excerpts you are given.
        If the excerpts do not contain the answer, say so plainly and suggest what to look for. Never invent facts, numbers or page references.
        Write for a student: a short heading, then a clear explanation (use short steps or bullets for processes, markdown allowed).
        Mention excerpt numbers inline like [1] when you use them. If a formula, definition, code line or worked example helps, put it in "snippet".
        Reply with JSON only, in exactly this shape:
        {"heading": "max 10 words", "body": "the answer", "snippet": null or {"label": "short title", "code": "the formula/definition/example", "explanation": "why it matters"}, "sources": [numbers of the excerpts you used]}
        """;

    private final ChatRepository chats;
    private final MessageRepository messages;
    private final DocumentRepository documents;
    private final DocumentService documentService;
    private final SubjectService subjects;
    private final RetrievalService retrieval;
    private final AiService ai;
    private final UsageService usage;
    private final ActivityService activity;

    public ChatService(ChatRepository chats, MessageRepository messages, DocumentRepository documents, DocumentService documentService,
                       SubjectService subjects, RetrievalService retrieval, AiService ai, UsageService usage, ActivityService activity) {
        this.chats = chats;
        this.messages = messages;
        this.documents = documents;
        this.documentService = documentService;
        this.subjects = subjects;
        this.retrieval = retrieval;
        this.ai = ai;
        this.usage = usage;
        this.activity = activity;
    }

    public List<Chat> list(UUID userId) { return chats.findByUserIdOrderByUpdatedAtDesc(userId); }

    public Chat getOwned(UUID userId, UUID chatId) {
        return chats.findByIdAndUserId(chatId, userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));
    }

    public Chat create(UUID userId, CreateChatRequest req) {
        Chat chat = new Chat();
        chat.userId = userId;
        String scope = "your documents";
        if (req != null && req.documentId() != null) {
            Document d = documentService.get(userId, req.documentId());
            chat.documentId = d.id;
            scope = d.name;
        } else if (req != null && req.subjectId() != null) {
            chat.subjectId = subjects.get(userId, req.subjectId()).id;
            scope = "this subject's documents";
        }
        if (req != null && req.title() != null && !req.title().isBlank()) chat.title = req.title().trim();
        chat = chats.save(chat);

        Message intro = new Message();
        intro.chatId = chat.id;
        intro.role = "assistant";
        intro.kind = "intro";
        intro.text = "Hi! Ask me anything about " + scope + ". I'll answer from your notes and tell you which page I used.";
        messages.save(intro);
        return chat;
    }

    public List<Message> messages(UUID userId, UUID chatId) {
        getOwned(userId, chatId);
        return messages.findByChatIdOrderByCreatedAtAsc(chatId);
    }

    @Transactional
    public void delete(UUID userId, UUID chatId) {
        Chat chat = getOwned(userId, chatId);
        messages.deleteByChatId(chatId);
        chats.delete(chat);
    }

    /** Answers a question from the student's documents. Saves the question and the answer, and returns the answer. */
    public Message ask(UUID userId, UUID chatId, String question) {
        Chat chat = getOwned(userId, chatId);
        usage.checkAllowed(userId);
        Instant asked = Instant.now();
        List<Passage> passages = retrieval.retrieve(userId, question, chat.documentId, chat.subjectId, 8);

        Message reply = new Message();
        reply.chatId = chatId;
        reply.role = "assistant";
        reply.kind = "synthesis";
        List<Passage> cited = new ArrayList<>();
        if (passages.isEmpty()) {
            reply.heading = "Nothing to search yet";
            reply.body = "I couldn't find any readable text to answer from. Upload a PDF (or wait for it to finish processing) and ask again.";
        } else if (!ai.isConfigured()) {
            reply.heading = "Most relevant passages";
            reply.body = extractive(passages);
            cited.addAll(passages.subList(0, Math.min(3, passages.size())));
        } else {
            cited.addAll(askAi(reply, question, passages, history(chatId)));
        }
        reply.citation = citation(cited);

        Message q = new Message();
        q.chatId = chatId;
        q.role = "user";
        q.kind = "plain";
        q.text = question;
        q.createdAt = asked;
        messages.save(q);
        reply.createdAt = Instant.now();
        reply = messages.save(reply);

        usage.increment(userId);
        if ("New chat".equals(chat.title)) chat.title = question.length() > 60 ? question.substring(0, 57) + "..." : question;
        chat.updatedAt = Instant.now();
        chats.save(chat);
        if (!cited.isEmpty()) markStudied(cited.get(0));
        activity.log(userId, "CHAT", chat.title, "You asked a question");
        return reply;
    }

    private List<Passage> askAi(Message reply, String question, List<Passage> passages, String history) {
        StringBuilder p = new StringBuilder();
        if (!history.isBlank()) p.append("Conversation so far:\n").append(history).append("\n\n");
        p.append("Source excerpts:\n");
        for (int i = 0; i < passages.size(); i++) {
            Passage s = passages.get(i);
            p.append('[').append(i + 1).append("] (").append(s.documentName()).append(", page ").append(s.page()).append(")\n")
                .append(s.text()).append("\n\n");
        }
        p.append("Student's question: ").append(question);

        JsonNode j = ai.completeJson(SYSTEM, p.toString());
        String body = j.path("body").asText("").trim();
        if (body.isEmpty()) throw new ApiException(HttpStatus.BAD_GATEWAY, "The AI's reply was empty. Please try again.");
        reply.heading = j.path("heading").asText("").trim();
        reply.body = body;
        JsonNode sn = j.path("snippet");
        if (sn.isObject() && !sn.path("code").asText("").isBlank()) {
            reply.snippetLabel = sn.path("label").asText("Example");
            reply.snippetCode = sn.path("code").asText();
            reply.snippetExplanation = sn.path("explanation").asText("");
        }
        // citations are built here from the excerpts the model says it used, so page numbers can't be invented
        Set<Integer> used = new LinkedHashSet<>();
        for (JsonNode n : j.path("sources")) {
            int k = n.asInt(0);
            if (k >= 1 && k <= passages.size()) used.add(k - 1);
        }
        List<Passage> cited = new ArrayList<>();
        for (int i : used) cited.add(passages.get(i));
        return cited;
    }

    private String history(UUID chatId) {
        List<Message> all = messages.findByChatIdOrderByCreatedAtAsc(chatId).stream().filter(m -> !"intro".equals(m.kind)).toList();
        StringBuilder sb = new StringBuilder();
        for (Message m : all.subList(Math.max(0, all.size() - 6), all.size())) {
            String t = "user".equals(m.role) ? m.text : m.body;
            if (t == null) continue;
            sb.append("user".equals(m.role) ? "Student: " : "StudyMind: ").append(t.length() > 500 ? t.substring(0, 500) + "..." : t).append('\n');
        }
        return sb.toString();
    }

    private static String extractive(List<Passage> passages) {
        StringBuilder sb = new StringBuilder();
        for (Passage s : passages.subList(0, Math.min(3, passages.size()))) {
            String t = s.text().replace('\n', ' ');
            sb.append("**").append(s.documentName()).append(", p.").append(s.page()).append("**\n> ")
                .append(t.length() > 450 ? t.substring(0, 450) + "..." : t).append("\n\n");
        }
        return sb.append("_AI answers are switched off, so these are the passages that best match your question._").toString();
    }

    private static String citation(List<Passage> cited) {
        Set<String> out = new LinkedHashSet<>();
        for (Passage s : cited) out.add(s.documentName() + ", p." + s.page());
        return out.isEmpty() ? null : String.join("; ", out);
    }

    /** Remembers where the student last read, for the "Continue learning" card. */
    private void markStudied(Passage p) {
        documents.findById(p.documentId()).ifPresent(d -> {
            d.lastPage = p.page();
            d.lastStudiedAt = Instant.now();
            documents.save(d);
        });
    }
}
