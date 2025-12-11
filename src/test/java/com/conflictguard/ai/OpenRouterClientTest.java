package com.conflictguard.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OpenRouterClient.
 * Tests JSON cleaning logic without requiring actual API calls.
 */
class OpenRouterClientTest {

    @Nested
    @DisplayName("cleanJsonResponse")
    class CleanJsonResponseTests {

        private final OpenRouterClient client = new OpenRouterClient(
                new ObjectMapper(), new RestTemplateBuilder());

        @Test
        @DisplayName("should return empty object for null input")
        void shouldReturnEmptyObjectForNull() {
            String result = client.cleanJsonResponse(null);
            assertEquals("{}", result);
        }

        @Test
        @DisplayName("should return empty object for blank input")
        void shouldReturnEmptyObjectForBlank() {
            String result = client.cleanJsonResponse("   ");
            assertEquals("{}", result);
        }

        @Test
        @DisplayName("should remove ```json prefix and ``` suffix")
        void shouldRemoveJsonCodeBlock() {
            String input = "```json\n{\"key\": \"value\"}\n```";
            String result = client.cleanJsonResponse(input);
            assertEquals("{\"key\": \"value\"}", result);
        }

        @Test
        @DisplayName("should remove ``` prefix and suffix without json marker")
        void shouldRemoveGenericCodeBlock() {
            String input = "```\n{\"entities\": []}\n```";
            String result = client.cleanJsonResponse(input);
            assertEquals("{\"entities\": []}", result);
        }

        @Test
        @DisplayName("should handle JSON without code blocks")
        void shouldHandlePlainJson() {
            String input = "{\"conflicts\": []}";
            String result = client.cleanJsonResponse(input);
            assertEquals("{\"conflicts\": []}", result);
        }

        @Test
        @DisplayName("should trim whitespace around JSON")
        void shouldTrimWhitespace() {
            String input = "  \n{\"data\": true}\n  ";
            String result = client.cleanJsonResponse(input);
            assertEquals("{\"data\": true}", result);
        }

        @Test
        @DisplayName("should handle nested code blocks correctly")
        void shouldHandleComplexJson() {
            String input = "```json\n{\n  \"entities\": [\n    {\"name\": \"Test\"}\n  ]\n}\n```";
            String result = client.cleanJsonResponse(input);
            assertTrue(result.contains("\"entities\""));
            assertTrue(result.contains("\"name\": \"Test\""));
            assertFalse(result.contains("```"));
        }
    }
}
