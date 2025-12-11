package com.conflictguard.graphql;

import com.conflictguard.domain.*;
import com.conflictguard.service.ConflictService;
import com.conflictguard.service.DocumentService;
import com.conflictguard.service.EntityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueryController")
class QueryControllerTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private EntityService entityService;

    @Mock
    private ConflictService conflictService;

    private QueryController queryController;

    @BeforeEach
    void setUp() {
        queryController = new QueryController(documentService, entityService, conflictService);
    }

    @Nested
    @DisplayName("documents query")
    class DocumentsQueryTests {

        @Test
        @DisplayName("should return all documents")
        void shouldReturnAllDocuments() {
            Document doc1 = Document.builder().id("1").name("Doc 1").build();
            Document doc2 = Document.builder().id("2").name("Doc 2").build();
            when(documentService.getAllDocuments()).thenReturn(List.of(doc1, doc2));

            List<Document> result = queryController.documents();

            assertThat(result).hasSize(2);
            verify(documentService).getAllDocuments();
        }

        @Test
        @DisplayName("should return empty list when no documents")
        void shouldReturnEmptyList() {
            when(documentService.getAllDocuments()).thenReturn(List.of());

            List<Document> result = queryController.documents();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("document query")
    class DocumentQueryTests {

        @Test
        @DisplayName("should return document by ID")
        void shouldReturnDocumentById() {
            Document doc = Document.builder()
                    .id("123")
                    .name("Test Contract")
                    .documentType(DocumentType.CONTRACT)
                    .build();
            when(documentService.getDocumentById("123")).thenReturn(Optional.of(doc));

            Document result = queryController.document("123");

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Test Contract");
        }

        @Test
        @DisplayName("should return null when document not found")
        void shouldReturnNullWhenNotFound() {
            when(documentService.getDocumentById("999")).thenReturn(Optional.empty());

            Document result = queryController.document("999");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("entities query")
    class EntitiesQueryTests {

        @Test
        @DisplayName("should return all entities when type is null")
        void shouldReturnAllEntities() {
            Entity e1 = Entity.builder().id("1").entityType(EntityType.TIME_PERIOD).build();
            Entity e2 = Entity.builder().id("2").entityType(EntityType.MONETARY_VALUE).build();
            when(entityService.getEntities(null)).thenReturn(List.of(e1, e2));

            List<Entity> result = queryController.entities(null);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should filter entities by type")
        void shouldFilterByType() {
            Entity e1 = Entity.builder().id("1").entityType(EntityType.TIME_PERIOD).build();
            when(entityService.getEntities(EntityType.TIME_PERIOD)).thenReturn(List.of(e1));

            List<Entity> result = queryController.entities(EntityType.TIME_PERIOD);

            assertThat(result).hasSize(1);
            verify(entityService).getEntities(EntityType.TIME_PERIOD);
        }
    }

    @Nested
    @DisplayName("entity query")
    class EntityQueryTests {

        @Test
        @DisplayName("should return entity by ID")
        void shouldReturnEntityById() {
            Entity entity = Entity.builder()
                    .id("456")
                    .name("Payment Term")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("14 days")
                    .build();
            when(entityService.getEntityById("456")).thenReturn(Optional.of(entity));

            Entity result = queryController.entity("456");

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Payment Term");
        }

        @Test
        @DisplayName("should return null when entity not found")
        void shouldReturnNullWhenNotFound() {
            when(entityService.getEntityById("nonexistent")).thenReturn(Optional.empty());

            Entity result = queryController.entity("nonexistent");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("conflicts query")
    class ConflictsQueryTests {

        @Test
        @DisplayName("should return all conflicts when severity is null")
        void shouldReturnAllConflicts() {
            Conflict c1 = Conflict.builder().id("1").severity(ConflictSeverity.HIGH).build();
            Conflict c2 = Conflict.builder().id("2").severity(ConflictSeverity.LOW).build();
            when(conflictService.getConflicts(null)).thenReturn(List.of(c1, c2));

            List<Conflict> result = queryController.conflicts(null);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should filter conflicts by severity")
        void shouldFilterBySeverity() {
            Conflict c1 = Conflict.builder().id("1").severity(ConflictSeverity.CRITICAL).build();
            when(conflictService.getConflicts(ConflictSeverity.CRITICAL)).thenReturn(List.of(c1));

            List<Conflict> result = queryController.conflicts(ConflictSeverity.CRITICAL);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSeverity()).isEqualTo(ConflictSeverity.CRITICAL);
        }
    }

    @Nested
    @DisplayName("conflictsForDocuments query")
    class ConflictsForDocumentsQueryTests {

        @Test
        @DisplayName("should return conflicts for specific documents")
        void shouldReturnConflictsForDocuments() {
            List<String> docIds = List.of("doc1", "doc2");
            Conflict conflict = Conflict.builder()
                    .id("c1")
                    .description("Payment term conflict")
                    .severity(ConflictSeverity.HIGH)
                    .build();
            when(conflictService.getConflictsForDocuments(docIds)).thenReturn(List.of(conflict));

            List<Conflict> result = queryController.conflictsForDocuments(docIds);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDescription()).isEqualTo("Payment term conflict");
        }
    }

    @Nested
    @DisplayName("ConflictAnalysisResult record")
    class ConflictAnalysisResultTests {

        @Test
        @DisplayName("should format analysis result correctly")
        void shouldFormatResultCorrectly() {
            // Testing the record structure for the DTO
            Conflict conflict = Conflict.builder()
                    .id("c1")
                    .description("Test conflict")
                    .severity(ConflictSeverity.HIGH)
                    .build();

            ConflictService.ConflictAnalysisResult analysisResult =
                    new ConflictService.ConflictAnalysisResult(
                            List.of(conflict),
                            "One conflict found",
                            LocalDateTime.of(2024, 1, 15, 10, 30));

            // Note: analyzeConflicts was moved to MutationController
            // This test verifies the record works correctly
            assertThat(analysisResult.conflicts()).hasSize(1);
            assertThat(analysisResult.summary()).isEqualTo("One conflict found");
        }
    }
}
