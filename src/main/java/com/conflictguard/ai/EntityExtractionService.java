package com.conflictguard.ai;

import com.conflictguard.domain.DocumentType;
import com.conflictguard.dto.ExtractionResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for extracting structured entities from documents using AI.
 * Delegates API communication to OpenRouterClient.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EntityExtractionService {

    private final ObjectMapper objectMapper;
    private final OpenRouterClient openRouterClient;
    private final PromptTemplates promptTemplates;

    /**
     * Extracts entities from a document using AI.
     */
    public ExtractionResult extractEntities(String documentName, DocumentType documentType, String content) {
        log.info("Extracting entities from document: {} (type: {})", documentName, documentType);

        String systemPrompt = promptTemplates.getEntityExtractionSystemPrompt();
        String userPrompt = promptTemplates.getEntityExtractionUserPrompt(
                documentType.name(), documentName, content);

        try {
            String response = openRouterClient.chat(systemPrompt, userPrompt);
            log.debug("AI extraction response: {}", response);
            return parseExtractionResult(response);
        } catch (OpenRouterClient.OpenRouterException e) {
            log.error("AI extraction failed: {}", e.getMessage());
            return ExtractionResult.builder()
                    .entities(List.of())
                    .documentSummary("AI extraction failed: " + e.getMessage())
                    .build();
        }
    }

    private ExtractionResult parseExtractionResult(String jsonResponse) {
        try {
            String cleanJson = openRouterClient.cleanJsonResponse(jsonResponse);
            return objectMapper.readValue(cleanJson, ExtractionResult.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse extraction result: {}", e.getMessage());
            log.debug("Raw response: {}", jsonResponse);
            return ExtractionResult.builder()
                    .entities(List.of())
                    .documentSummary("Failed to parse extraction result")
                    .build();
        }
    }
}
