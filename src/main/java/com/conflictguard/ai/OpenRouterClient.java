package com.conflictguard.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Centralized client for OpenRouter API communication.
 * Eliminates code duplication between EntityExtractionService and ConflictReasoningService.
 */
@Component
@Slf4j
public class OpenRouterClient {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${openrouter.api-key:demo}")
    private String apiKey;

    @Value("${openrouter.model:tngtech/deepseek-r1t2-chimera:free}")
    private String model;

    @Value("${openrouter.base-url:https://openrouter.ai/api/v1}")
    private String baseUrl;

    @Value("${openrouter.temperature:0.2}")
    private double temperature;

    @Value("${openrouter.max-tokens:2000}")
    private int maxTokens;

    public OpenRouterClient(ObjectMapper objectMapper, RestTemplateBuilder restTemplateBuilder) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofSeconds(120))
                .build();
    }

    /**
     * Sends a chat completion request to OpenRouter API.
     *
     * @param systemPrompt The system prompt defining AI behavior
     * @param userPrompt   The user prompt with actual content
     * @return The AI response content as a string
     * @throws OpenRouterException if the API call fails
     */
    public String chat(String systemPrompt, String userPrompt) {
        HttpHeaders headers = buildHeaders();
        Map<String, Object> body = buildRequestBody(systemPrompt, userPrompt);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/chat/completions",
                    HttpMethod.POST,
                    request,
                    String.class);

            return extractContentFromResponse(response.getBody());
        } catch (RestClientException e) {
            log.error("OpenRouter API call failed: {}", e.getMessage());
            throw new OpenRouterException("Failed to communicate with OpenRouter API", e);
        }
    }

    /**
     * Parses JSON response from AI, handling markdown code blocks.
     *
     * @param response Raw response string from AI
     * @return Cleaned JSON string ready for parsing
     */
    public String cleanJsonResponse(String response) {
        if (response == null || response.isBlank()) {
            return "{}";
        }
        String cleaned = response.trim();

        // Handle markdown code blocks
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("HTTP-Referer", "http://localhost:8080");
        headers.set("X-Title", "ConflictGuard");
        return headers;
    }

    private Map<String, Object> buildRequestBody(String systemPrompt, String userPrompt) {
        Map<String, Object> systemMessage = Map.of("role", "system", "content", systemPrompt);
        Map<String, Object> userMessage = Map.of("role", "user", "content", userPrompt);

        return Map.of(
                "model", model,
                "messages", List.of(systemMessage, userMessage),
                "temperature", temperature,
                "max_tokens", maxTokens
        );
    }

    private String extractContentFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").path(0).path("message").path("content").asText();

            if (content == null || content.isBlank()) {
                throw new OpenRouterException("Empty response from OpenRouter API");
            }

            return content;
        } catch (Exception e) {
            log.error("Failed to parse OpenRouter response: {}", e.getMessage());
            throw new OpenRouterException("Failed to parse AI response", e);
        }
    }

    /**
     * Custom exception for OpenRouter API errors.
     */
    public static class OpenRouterException extends RuntimeException {
        public OpenRouterException(String message) {
            super(message);
        }

        public OpenRouterException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
