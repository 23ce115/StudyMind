package com.studymind.backend.service;

import com.studymind.backend.config.AppTime;
import com.studymind.backend.exception.ApiException;
import com.studymind.backend.model.UsageDaily;
import com.studymind.backend.repository.UsageDailyRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Daily cap on chat questions (protects the AI free tier). */
@Service
public class UsageService {
    private final UsageDailyRepository repo;
    private final AppTime time;
    private final int dailyLimit;

    public UsageService(UsageDailyRepository repo, AppTime time, @Value("${app.usage.daily-question-limit}") int dailyLimit) {
        this.repo = repo;
        this.time = time;
        this.dailyLimit = dailyLimit;
    }

    public int limit() { return dailyLimit; }

    public int questionsToday(UUID userId) {
        return repo.findByUserIdAndUsageDate(userId, time.today()).map(u -> u.questionsAsked).orElse(0);
    }

    public void checkAllowed(UUID userId) {
        if (questionsToday(userId) >= dailyLimit) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "You've used all " + dailyLimit + " questions for today. The limit resets at midnight.");
        }
    }

    public void increment(UUID userId) {
        LocalDate d = time.today();
        UsageDaily u = repo.findByUserIdAndUsageDate(userId, d).orElseGet(() -> {
            UsageDaily n = new UsageDaily();
            n.userId = userId;
            n.usageDate = d;
            return n;
        });
        u.questionsAsked++;
        repo.save(u);
    }
}
