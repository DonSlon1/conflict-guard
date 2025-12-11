package com.conflictguard.ai;

import com.conflictguard.domain.Entity;
import com.conflictguard.dto.ConflictAnalysisDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for analyzing entities and detecting conflicts using AI reasoning.
 * Delegates API communication to OpenRouterClient.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConflictReasoningService {

    private final ObjectMapper objectMapper;
    private final OpenRouterClient openRouterClient;
    private final PromptTemplates promptTemplates;

    /**
     * Analyzes entities for conflicts using AI reasoning.
     */
    public ConflictAnalysisDto analyzeConflicts(List<Entity> entities) {
        log.info("Analyzing {} entities for conflicts", entities.size());

        String entitiesText = formatEntitiesForPrompt(entities);
        String systemPrompt = promptTemplates.getConflictReasoningSystemPrompt();
        String userPrompt = promptTemplates.getConflictReasoningUserPrompt(entitiesText);

        try {
            String response = openRouterClient.chat(systemPrompt, userPrompt);
            log.debug("AI conflict analysis response: {}", response);
            return parseConflictAnalysis(response);
        } catch (OpenRouterClient.OpenRouterException e) {
            log.error("AI conflict analysis failed: {}", e.getMessage());
            return ConflictAnalysisDto.builder()
                    .conflicts(List.of())
                    .overallSummary("AI analysis failed: " + e.getMessage())
                    .build();
        }
    }

    private String formatEntitiesForPrompt(List<Entity> entities) {
        return entities.stream()
                .map(e -> String.format("- %s (%s): %s [context: %s]",
                        e.getName(),
                        e.getEntityType(),
                        e.getValue(),
                        e.getSourceContext()))
                .collect(Collectors.joining("\n"));
    }

    private ConflictAnalysisDto parseConflictAnalysis(String jsonResponse) {
        try {
            String cleanJson = openRouterClient.cleanJsonResponse(jsonResponse);
            return objectMapper.readValue(cleanJson, ConflictAnalysisDto.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse conflict analysis: {}", e.getMessage());
            log.debug("Raw response: {}", jsonResponse);
            return ConflictAnalysisDto.builder()
                    .conflicts(List.of())
                    .overallSummary("Failed to parse conflict analysis")
                    .build();
        }
    }
}
