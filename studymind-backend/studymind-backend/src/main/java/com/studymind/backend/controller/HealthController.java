package com.studymind.backend.controller;

import com.studymind.backend.service.AiService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final AiService ai;

    public HealthController(AiService ai) { this.ai = ai; }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "ai", ai.providerName(), "aiConfigured", ai.isConfigured());
    }
}
