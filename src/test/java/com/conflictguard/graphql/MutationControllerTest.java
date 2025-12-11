package com.conflictguard.graphql;

import com.conflictguard.domain.DocumentType;
import com.conflictguard.graphql.exception.GraphQLValidationException;
import com.conflictguard.service.ConflictService;
import com.conflictguard.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MutationController validation logic.
 */
@ExtendWith(MockitoExtension.class)
class MutationControllerTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private ConflictService conflictService;

    private MutationController controller;

    @BeforeEach
    void setUp() {
        controller = new MutationController(documentService, conflictService);
    }

    @Nested
    @DisplayName("Document Input Validation")
    class DocumentInputValidationTests {

        @Test
        @DisplayName("should reject null document name")
        void shouldRejectNullName() {
            MutationController.DocumentInput input = new MutationController.DocumentInput(
                    null, "Some content", DocumentType.CONTRACT);

            GraphQLValidationException ex = assertThrows(
                    GraphQLValidationException.class,
                    () -> controller.ingestDocument(input)
            );

            assertTrue(ex.getMessage().contains("name cannot be empty"));
            assertEquals("input.name", ex.getExtensions().get("field"));
        }

        @Test
        @DisplayName("should reject blank document name")
        void shouldRejectBlankName() {
            MutationController.DocumentInput input = new MutationController.DocumentInput(
                    "   ", "Some content", DocumentType.CONTRACT);

            GraphQLValidationException ex = assertThrows(
                    GraphQLValidationException.class,
                    () -> controller.ingestDocument(input)
            );

            assertTrue(ex.getMessage().contains("name cannot be empty"));
        }

        @Test
        @DisplayName("should reject document name exceeding max length")
        void shouldRejectLongName() {
            String longName = "x".repeat(300);
            MutationController.DocumentInput input = new MutationController.DocumentInput(
                    longName, "Some content", DocumentType.CONTRACT);

            GraphQLValidationException ex = assertThrows(
                    GraphQLValidationException.class,
                    () -> controller.ingestDocument(input)
            );

            assertTrue(ex.getMessage().contains("exceeds maximum length"));
        }

        @Test
        @DisplayName("should reject null document content")
        void shouldRejectNullContent() {
            MutationController.DocumentInput input = new MutationController.DocumentInput(
                    "Valid Name", null, DocumentType.CONTRACT);

            GraphQLValidationException ex = assertThrows(
                    GraphQLValidationException.class,
                    () -> controller.ingestDocument(input)
            );

            assertTrue(ex.getMessage().contains("content cannot be empty"));
        }

        @Test
        @DisplayName("should reject content exceeding max length")
        void shouldRejectLongContent() {
            String longContent = "x".repeat(150_000);
            MutationController.DocumentInput input = new MutationController.DocumentInput(
                    "Valid Name", longContent, DocumentType.CONTRACT);

            GraphQLValidationException ex = assertThrows(
                    GraphQLValidationException.class,
                    () -> controller.ingestDocument(input)
            );

            assertTrue(ex.getMessage().contains("content exceeds maximum length"));
        }

        @Test
        @DisplayName("should reject null document type")
        void shouldRejectNullType() {
            MutationController.DocumentInput input = new MutationController.DocumentInput(
                    "Valid Name", "Valid content", null);

            GraphQLValidationException ex = assertThrows(
                    GraphQLValidationException.class,
                    () -> controller.ingestDocument(input)
            );

            assertTrue(ex.getMessage().contains("Document type is required"));
        }
    }

    @Nested
    @DisplayName("Document IDs Validation")
    class DocumentIdsValidationTests {

        @Test
        @DisplayName("should reject null document IDs list")
        void shouldRejectNullIds() {
            GraphQLValidationException ex = assertThrows(
                    GraphQLValidationException.class,
                    () -> controller.analyzeConflicts(null)
            );

            assertTrue(ex.getMessage().contains("At least one document ID is required"));
        }

        @Test
        @DisplayName("should reject empty document IDs list")
        void shouldRejectEmptyIds() {
            GraphQLValidationException ex = assertThrows(
                    GraphQLValidationException.class,
                    () -> controller.analyzeConflicts(Collections.emptyList())
            );

            assertTrue(ex.getMessage().contains("At least one document ID is required"));
        }

        @Test
        @DisplayName("should reject too many document IDs")
        void shouldRejectTooManyIds() {
            List<String> tooManyIds = List.of(
                    "id1", "id2", "id3", "id4", "id5",
                    "id6", "id7", "id8", "id9", "id10", "id11"
            );

            GraphQLValidationException ex = assertThrows(
                    GraphQLValidationException.class,
                    () -> controller.analyzeConflicts(tooManyIds)
            );

            assertTrue(ex.getMessage().contains("Cannot analyze more than"));
        }
    }

    @Nested
    @DisplayName("GraphQL Exception Format")
    class GraphQLExceptionFormatTests {

        @Test
        @DisplayName("should include proper error classification")
        void shouldIncludeErrorClassification() {
            GraphQLValidationException ex = new GraphQLValidationException(
                    "Test message", "test.field");

            assertNotNull(ex.getErrorType());
            assertEquals("VALIDATION_ERROR", ex.getExtensions().get("code"));
        }

        @Test
        @DisplayName("should include field in extensions")
        void shouldIncludeFieldInExtensions() {
            GraphQLValidationException ex = new GraphQLValidationException(
                    "Test message", "input.name");

            assertEquals("input.name", ex.getExtensions().get("field"));
        }
    }
}
