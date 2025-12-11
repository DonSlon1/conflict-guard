package com.conflictguard.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PromptTemplates")
class PromptTemplatesTest {

    private PromptTemplates promptTemplates;

    @BeforeEach
    void setUp() throws IOException {
        promptTemplates = new PromptTemplates();

        // Set up resources using reflection (simulating @Value injection)
        ReflectionTestUtils.setField(promptTemplates, "entityExtractionSystemResource",
                new ClassPathResource("prompts/entity-extraction-system.txt"));
        ReflectionTestUtils.setField(promptTemplates, "entityExtractionUserResource",
                new ClassPathResource("prompts/entity-extraction-user.txt"));
        ReflectionTestUtils.setField(promptTemplates, "conflictReasoningSystemResource",
                new ClassPathResource("prompts/conflict-reasoning-system.txt"));
        ReflectionTestUtils.setField(promptTemplates, "conflictReasoningUserResource",
                new ClassPathResource("prompts/conflict-reasoning-user.txt"));

        promptTemplates.loadTemplates();
    }

    @Nested
    @DisplayName("loadTemplates")
    class LoadTemplatesTests {

        @Test
        @DisplayName("should load entity extraction system prompt")
        void shouldLoadEntityExtractionSystemPrompt() {
            String prompt = promptTemplates.getEntityExtractionSystemPrompt();

            assertThat(prompt).isNotNull();
            assertThat(prompt).isNotEmpty();
            assertThat(prompt).contains("legal document analyzer");
            assertThat(prompt).contains("ENTITY TYPES");
        }

        @Test
        @DisplayName("should load conflict reasoning system prompt")
        void shouldLoadConflictReasoningSystemPrompt() {
            String prompt = promptTemplates.getConflictReasoningSystemPrompt();

            assertThat(prompt).isNotNull();
            assertThat(prompt).isNotEmpty();
            assertThat(prompt).contains("legal reasoning engine");
            assertThat(prompt).contains("LEGAL PRINCIPLES");
        }
    }

    @Nested
    @DisplayName("getEntityExtractionUserPrompt")
    class GetEntityExtractionUserPromptTests {

        @Test
        @DisplayName("should substitute document type placeholder")
        void shouldSubstituteDocumentType() {
            String prompt = promptTemplates.getEntityExtractionUserPrompt(
                    "CONTRACT", "Test Doc", "Test content");

            assertThat(prompt).contains("CONTRACT");
            assertThat(prompt).doesNotContain("{documentType}");
        }

        @Test
        @DisplayName("should substitute document name placeholder")
        void shouldSubstituteDocumentName() {
            String prompt = promptTemplates.getEntityExtractionUserPrompt(
                    "CONTRACT", "My Important Contract", "Test content");

            assertThat(prompt).contains("My Important Contract");
            assertThat(prompt).doesNotContain("{documentName}");
        }

        @Test
        @DisplayName("should substitute content placeholder")
        void shouldSubstituteContent() {
            String content = "Payment terms are 30 days from invoice date.";
            String prompt = promptTemplates.getEntityExtractionUserPrompt(
                    "CONTRACT", "Test", content);

            assertThat(prompt).contains(content);
            assertThat(prompt).doesNotContain("{content}");
        }

        @Test
        @DisplayName("should handle special characters in content")
        void shouldHandleSpecialCharacters() {
            String content = "Splatnost faktur je 14 dní. Pokuta: 0.05% denně.";
            String prompt = promptTemplates.getEntityExtractionUserPrompt(
                    "CONTRACT", "Smlouva", content);

            assertThat(prompt).contains(content);
        }
    }

    @Nested
    @DisplayName("getConflictReasoningUserPrompt")
    class GetConflictReasoningUserPromptTests {

        @Test
        @DisplayName("should substitute entities placeholder")
        void shouldSubstituteEntities() {
            String entities = "- Payment Term (TIME_PERIOD): 14 days\n- Standard Term (TIME_PERIOD): 30 days";
            String prompt = promptTemplates.getConflictReasoningUserPrompt(entities);

            assertThat(prompt).contains(entities);
            assertThat(prompt).doesNotContain("{entities}");
        }

        @Test
        @DisplayName("should handle empty entities list")
        void shouldHandleEmptyEntities() {
            String prompt = promptTemplates.getConflictReasoningUserPrompt("");

            assertThat(prompt).doesNotContain("{entities}");
        }

        @Test
        @DisplayName("should handle multiline entities")
        void shouldHandleMultilineEntities() {
            String entities = """
                    - Entity 1 (TYPE_A): Value 1 [context: Source 1]
                    - Entity 2 (TYPE_B): Value 2 [context: Source 2]
                    - Entity 3 (TYPE_C): Value 3 [context: Source 3]
                    """;
            String prompt = promptTemplates.getConflictReasoningUserPrompt(entities);

            assertThat(prompt).contains("Entity 1");
            assertThat(prompt).contains("Entity 2");
            assertThat(prompt).contains("Entity 3");
        }
    }
}
