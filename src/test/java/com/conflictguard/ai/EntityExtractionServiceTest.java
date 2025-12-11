package com.conflictguard.ai;

import com.conflictguard.domain.DocumentType;
import com.conflictguard.domain.EntityType;
import com.conflictguard.dto.ExtractionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntityExtractionService")
class EntityExtractionServiceTest {

    @Mock
    private OpenRouterClient openRouterClient;

    @Mock
    private PromptTemplates promptTemplates;

    private ObjectMapper objectMapper;
    private EntityExtractionService extractionService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        extractionService = new EntityExtractionService(objectMapper, openRouterClient, promptTemplates);
    }

    @Nested
    @DisplayName("extractEntities")
    class ExtractEntitiesTests {

        @Test
        @DisplayName("should extract entities from AI response")
        void shouldExtractEntities() {
            // Setup prompts
            when(promptTemplates.getEntityExtractionSystemPrompt()).thenReturn("system prompt");
            when(promptTemplates.getEntityExtractionUserPrompt(any(), any(), any()))
                    .thenReturn("user prompt");

            // AI returns valid JSON
            String aiResponse = """
                {
                    "entities": [
                        {
                            "name": "Payment Term",
                            "entityType": "TIME_PERIOD",
                            "value": "14 days",
                            "sourceContext": "Payment within 14 days"
                        }
                    ],
                    "documentSummary": "Contract with payment terms"
                }
                """;
            when(openRouterClient.chat(any(), any())).thenReturn(aiResponse);
            when(openRouterClient.cleanJsonResponse(any())).thenReturn(aiResponse.trim());

            ExtractionResult result = extractionService.extractEntities(
                    "Test Contract", DocumentType.CONTRACT, "Contract content");

            assertThat(result.getEntities()).hasSize(1);
            assertThat(result.getEntities().get(0).getName()).isEqualTo("Payment Term");
            assertThat(result.getEntities().get(0).getEntityType()).isEqualTo(EntityType.TIME_PERIOD);
            assertThat(result.getDocumentSummary()).isEqualTo("Contract with payment terms");
        }

        @Test
        @DisplayName("should handle AI exception gracefully")
        void shouldHandleAiException() {
            when(promptTemplates.getEntityExtractionSystemPrompt()).thenReturn("system");
            when(promptTemplates.getEntityExtractionUserPrompt(any(), any(), any())).thenReturn("user");
            when(openRouterClient.chat(any(), any()))
                    .thenThrow(new OpenRouterClient.OpenRouterException("API error", null));

            ExtractionResult result = extractionService.extractEntities(
                    "Doc", DocumentType.CONTRACT, "Content");

            assertThat(result.getEntities()).isEmpty();
            assertThat(result.getDocumentSummary()).contains("AI extraction failed");
        }

        @Test
        @DisplayName("should handle invalid JSON response")
        void shouldHandleInvalidJson() {
            when(promptTemplates.getEntityExtractionSystemPrompt()).thenReturn("system");
            when(promptTemplates.getEntityExtractionUserPrompt(any(), any(), any())).thenReturn("user");
            when(openRouterClient.chat(any(), any())).thenReturn("Not valid JSON");
            when(openRouterClient.cleanJsonResponse(any())).thenReturn("Not valid JSON");

            ExtractionResult result = extractionService.extractEntities(
                    "Doc", DocumentType.REGULATION, "Content");

            assertThat(result.getEntities()).isEmpty();
            assertThat(result.getDocumentSummary()).contains("Failed to parse");
        }

        @Test
        @DisplayName("should extract multiple entities")
        void shouldExtractMultipleEntities() {
            when(promptTemplates.getEntityExtractionSystemPrompt()).thenReturn("system");
            when(promptTemplates.getEntityExtractionUserPrompt(any(), any(), any())).thenReturn("user");

            String aiResponse = """
                {
                    "entities": [
                        {"name": "Payment Term", "entityType": "TIME_PERIOD", "value": "14 days"},
                        {"name": "Penalty", "entityType": "PENALTY", "value": "5%"},
                        {"name": "Contract Value", "entityType": "MONETARY_VALUE", "value": "$10000"}
                    ],
                    "documentSummary": "Complex contract"
                }
                """;
            when(openRouterClient.chat(any(), any())).thenReturn(aiResponse);
            when(openRouterClient.cleanJsonResponse(any())).thenReturn(aiResponse.trim());

            ExtractionResult result = extractionService.extractEntities(
                    "Complex Contract", DocumentType.CONTRACT, "Content");

            assertThat(result.getEntities()).hasSize(3);
            assertThat(result.getEntities()).extracting("entityType")
                    .containsExactly(EntityType.TIME_PERIOD, EntityType.PENALTY, EntityType.MONETARY_VALUE);
        }

        @Test
        @DisplayName("should pass correct prompts to AI client")
        void shouldPassCorrectPrompts() {
            when(promptTemplates.getEntityExtractionSystemPrompt()).thenReturn("SYSTEM");
            when(promptTemplates.getEntityExtractionUserPrompt("CONTRACT", "My Doc", "Content"))
                    .thenReturn("USER");
            when(openRouterClient.chat("SYSTEM", "USER")).thenReturn("{\"entities\":[], \"documentSummary\":\"\"}");
            when(openRouterClient.cleanJsonResponse(any())).thenAnswer(inv -> inv.getArgument(0));

            extractionService.extractEntities("My Doc", DocumentType.CONTRACT, "Content");

            verify(openRouterClient).chat("SYSTEM", "USER");
        }
    }
}
