package com.studymind.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studymind.backend.exception.ApiException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * One door to the language model. Providers: gemini (free tier available), anthropic, openai.
 * With AI_PROVIDER=none the app still works (chat falls back to showing the best-matching passages).
 */
@Service
public class AiService {
    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final RestClient http;
    private final ObjectMapper mapper;
    private final String provider;
    private final String apiKey;
    private final String model;

    public AiService(RestClient http, ObjectMapper mapper, @Value("${ai.provider}") String provider,
                     @Value("${ai.api-key}") String apiKey, @Value("${ai.model}") String model) {
        this.http = http;
        this.mapper = mapper;
        this.provider = provider == null ? "none" : provider.trim().toLowerCase(Locale.ROOT);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
    }

    public boolean isConfigured() { return !provider.equals("none") && !apiKey.isBlank(); }
    public String providerName() { return isConfigured() ? provider : "none"; }

    /** Plain-text (or markdown) answer. */
    public String complete(String system, String user) { return call(system, user, false); }

    /** The model's reply parsed as JSON (tolerates ```json fences and chatter around the JSON). */
    public JsonNode completeJson(String system, String user) {
        String text = call(system, user, true).trim();
        int a = firstIndex(text);
        int b = Math.max(text.lastIndexOf('}'), text.lastIndexOf(']'));
        if (a < 0 || b <= a) throw badFormat();
        try {
            return mapper.readTree(text.substring(a, b + 1));
        } catch (JsonProcessingException e) {
            log.warn("AI reply was not valid JSON: {}", text.length() > 300 ? text.substring(0, 300) : text);
            throw badFormat();
        }
    }

    private static int firstIndex(String t) {
        int o = t.indexOf('{'), a = t.indexOf('[');
        if (o < 0) return a;
        if (a < 0) return o;
        return Math.min(o, a);
    }

    private static ApiException badFormat() {
        return new ApiException(HttpStatus.BAD_GATEWAY, "The AI's reply wasn't in the expected format. Please try again.");
    }

    private String call(String system, String user, boolean json) {
        if (!isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "AI isn't set up yet. Set AI_PROVIDER and AI_API_KEY on the backend (a free Gemini key works).");
        }
        try {
            String text = switch (provider) {
                case "gemini" -> gemini(system, user, json);
                case "anthropic" -> anthropic(system, user);
                case "openai" -> openai(system, user, json);
                default -> throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Unknown AI_PROVIDER '" + provider + "' (use gemini, anthropic or openai)");
            };
            if (text == null || text.isBlank()) throw new ApiException(HttpStatus.BAD_GATEWAY, "The AI returned an empty answer. Please try again.");
            return text;
        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            log.warn("AI provider {} returned HTTP {}: {}", provider, code, e.getResponseBodyAsString());
            if (code == 429) throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "The AI is busy or the free limit was reached. Wait a minute and try again.");
            if (code == 400 || code == 401 || code == 403 || code == 404) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "The AI provider rejected the request. Check AI_API_KEY and AI_MODEL on the backend.");
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "The AI service had a problem. Please try again.");
        } catch (ResourceAccessException e) {
            log.warn("AI provider unreachable: {}", e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Couldn't reach the AI service (timeout or no internet). Please try again.");
        }
    }

    private String gemini(String system, String user, boolean json) {
        Map<String, Object> gen = new LinkedHashMap<>();
        gen.put("temperature", 0.3);
        gen.put("maxOutputTokens", 8192);
        if (json) gen.put("responseMimeType", "application/json");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", system))));
        body.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", user)))));
        body.put("generationConfig", gen);
        JsonNode res = http.post()
            .uri("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent")
            .header("x-goog-api-key", apiKey).contentType(MediaType.APPLICATION_JSON).body(body)
            .retrieve().body(JsonNode.class);
        return res == null ? null : res.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
    }

    private String anthropic(String system, String user) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", 4096);
        body.put("temperature", 0.3);
        body.put("system", system);
        body.put("messages", List.of(Map.of("role", "user", "content", user)));
        JsonNode res = http.post().uri("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey).header("anthropic-version", "2023-06-01")
            .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
        return res == null ? null : res.path("content").path(0).path("text").asText("");
    }

    private String openai(String system, String user, boolean json) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0.3);
        body.put("messages", List.of(Map.of("role", "system", "content", system), Map.of("role", "user", "content", user)));
        if (json) body.put("response_format", Map.of("type", "json_object"));
        JsonNode res = http.post().uri("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer " + apiKey)
            .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
        return res == null ? null : res.path("choices").path(0).path("message").path("content").asText("");
    }
}
