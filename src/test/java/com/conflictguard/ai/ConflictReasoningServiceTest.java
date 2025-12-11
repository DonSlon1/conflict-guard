package com.conflictguard.ai;

import com.conflictguard.domain.ConflictSeverity;
import com.conflictguard.domain.Entity;
import com.conflictguard.domain.EntityType;
import com.conflictguard.dto.ConflictAnalysisDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConflictReasoningService")
class ConflictReasoningServiceTest {

    @Mock
    private OpenRouterClient openRouterClient;

    @Mock
    private PromptTemplates promptTemplates;

    private ObjectMapper objectMapper;
    private ConflictReasoningService reasoningService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        reasoningService = new ConflictReasoningService(objectMapper, openRouterClient, promptTemplates);
    }

    @Nested
    @DisplayName("analyzeConflicts")
    class AnalyzeConflictsTests {

        @Test
        @DisplayName("should analyze entities and return conflicts")
        void shouldAnalyzeAndReturnConflicts() {
            when(promptTemplates.getConflictReasoningSystemPrompt()).thenReturn("system");
            when(promptTemplates.getConflictReasoningUserPrompt(any())).thenReturn("user");

            String aiResponse = """
                {
                    "conflicts": [
                        {
                            "description": "Payment term conflict",
                            "severity": "HIGH",
                            "reasoning": "14 days vs 30 days",
                            "legalPrinciple": "Lex Specialis",
                            "involvedEntityNames": ["Payment Term A", "Payment Term B"]
                        }
                    ],
                    "overallSummary": "One conflict found"
                }
                """;
            when(openRouterClient.chat(any(), any())).thenReturn(aiResponse);
            when(openRouterClient.cleanJsonResponse(any())).thenReturn(aiResponse.trim());

            Entity e1 = createEntity("Payment Term A", EntityType.TIME_PERIOD, "14 days");
            Entity e2 = createEntity("Payment Term B", EntityType.TIME_PERIOD, "30 days");

            ConflictAnalysisDto result = reasoningService.analyzeConflicts(List.of(e1, e2));

            assertThat(result.getConflicts()).hasSize(1);
            assertThat(result.getConflicts().get(0).getSeverity()).isEqualTo(ConflictSeverity.HIGH);
            assertThat(result.getConflicts().get(0).getLegalPrinciple()).isEqualTo("Lex Specialis");
            assertThat(result.getOverallSummary()).isEqualTo("One conflict found");
        }

        @Test
        @DisplayName("should handle AI exception gracefully")
        void shouldHandleAiException() {
            when(promptTemplates.getConflictReasoningSystemPrompt()).thenReturn("system");
            when(promptTemplates.getConflictReasoningUserPrompt(any())).thenReturn("user");
            when(openRouterClient.chat(any(), any()))
                    .thenThrow(new OpenRouterClient.OpenRouterException("API error", null));

            Entity entity = createEntity("Test", EntityType.CLAUSE, "value");

            ConflictAnalysisDto result = reasoningService.analyzeConflicts(List.of(entity));

            assertThat(result.getConflicts()).isEmpty();
            assertThat(result.getOverallSummary()).contains("AI analysis failed");
        }

        @Test
        @DisplayName("should handle invalid JSON response")
        void shouldHandleInvalidJson() {
            when(promptTemplates.getConflictReasoningSystemPrompt()).thenReturn("system");
            when(promptTemplates.getConflictReasoningUserPrompt(any())).thenReturn("user");
            when(openRouterClient.chat(any(), any())).thenReturn("Invalid JSON response");
            when(openRouterClient.cleanJsonResponse(any())).thenReturn("Invalid JSON response");

            Entity entity = createEntity("Test", EntityType.OBLIGATION, "value");

            ConflictAnalysisDto result = reasoningService.analyzeConflicts(List.of(entity));

            assertThat(result.getConflicts()).isEmpty();
            assertThat(result.getOverallSummary()).contains("Failed to parse");
        }

        @Test
        @DisplayName("should format entities correctly for prompt")
        void shouldFormatEntitiesForPrompt() {
            when(promptTemplates.getConflictReasoningSystemPrompt()).thenReturn("system");
            when(openRouterClient.chat(any(), any())).thenReturn("{\"conflicts\":[], \"overallSummary\":\"None\"}");
            when(openRouterClient.cleanJsonResponse(any())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
            when(promptTemplates.getConflictReasoningUserPrompt(promptCaptor.capture()))
                    .thenReturn("user prompt");

            Entity entity = Entity.builder()
                    .name("Payment Term")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("14 days")
                    .sourceContext("Payment in 14 days")
                    .build();

            reasoningService.analyzeConflicts(List.of(entity));

            String formattedEntities = promptCaptor.getValue();
            assertThat(formattedEntities).contains("Payment Term");
            assertThat(formattedEntities).contains("TIME_PERIOD");
            assertThat(formattedEntities).contains("14 days");
            assertThat(formattedEntities).contains("Payment in 14 days");
        }

        @Test
        @DisplayName("should handle multiple conflicts")
        void shouldHandleMultipleConflicts() {
            when(promptTemplates.getConflictReasoningSystemPrompt()).thenReturn("system");
            when(promptTemplates.getConflictReasoningUserPrompt(any())).thenReturn("user");

            String aiResponse = """
                {
                    "conflicts": [
                        {"description": "Conflict 1", "severity": "CRITICAL", "involvedEntityNames": []},
                        {"description": "Conflict 2", "severity": "HIGH", "involvedEntityNames": []},
                        {"description": "Conflict 3", "severity": "MEDIUM", "involvedEntityNames": []}
                    ],
                    "overallSummary": "Three conflicts detected"
                }
                """;
            when(openRouterClient.chat(any(), any())).thenReturn(aiResponse);
            when(openRouterClient.cleanJsonResponse(any())).thenReturn(aiResponse.trim());

            Entity entity = createEntity("Test", EntityType.CLAUSE, "value");

            ConflictAnalysisDto result = reasoningService.analyzeConflicts(List.of(entity));

            assertThat(result.getConflicts()).hasSize(3);
            assertThat(result.getConflicts()).extracting("severity")
                    .containsExactly(ConflictSeverity.CRITICAL, ConflictSeverity.HIGH, ConflictSeverity.MEDIUM);
        }

        @Test
        @DisplayName("should handle empty conflicts list")
        void shouldHandleEmptyConflicts() {
            when(promptTemplates.getConflictReasoningSystemPrompt()).thenReturn("system");
            when(promptTemplates.getConflictReasoningUserPrompt(any())).thenReturn("user");

            String aiResponse = """
                {
                    "conflicts": [],
                    "overallSummary": "No conflicts found"
                }
                """;
            when(openRouterClient.chat(any(), any())).thenReturn(aiResponse);
            when(openRouterClient.cleanJsonResponse(any())).thenReturn(aiResponse.trim());

            Entity entity = createEntity("Compatible", EntityType.CLAUSE, "same value");

            ConflictAnalysisDto result = reasoningService.analyzeConflicts(List.of(entity));

            assertThat(result.getConflicts()).isEmpty();
            assertThat(result.getOverallSummary()).isEqualTo("No conflicts found");
        }
    }

    private Entity createEntity(String name, EntityType type, String value) {
        return Entity.builder()
                .id("test-" + name.hashCode())
                .name(name)
                .entityType(type)
                .value(value)
                .sourceContext("Context for " + name)
                .build();
    }
}
