package com.conflictguard.service;

import com.conflictguard.ai.EntityExtractionService;
import com.conflictguard.domain.Document;
import com.conflictguard.domain.DocumentType;
import com.conflictguard.domain.Entity;
import com.conflictguard.domain.EntityType;
import com.conflictguard.dto.ExtractionResult;
import com.conflictguard.dto.ExtractedEntity;
import com.conflictguard.repository.DocumentRepository;
import com.conflictguard.repository.EntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentService")
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private EntityRepository entityRepository;

    @Mock
    private EntityExtractionService extractionService;

    @Captor
    private ArgumentCaptor<Document> documentCaptor;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(documentRepository, entityRepository, extractionService);
    }

    @Nested
    @DisplayName("ingestDocument")
    class IngestDocumentTests {

        @Test
        @DisplayName("should create document with extracted entities")
        void shouldCreateDocumentWithEntities() {
            // Given
            String name = "Test Contract";
            String content = "Payment due in 14 days";
            DocumentType type = DocumentType.CONTRACT;

            ExtractedEntity extractedEntity = ExtractedEntity.builder()
                    .name("Payment Term")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("14 days")
                    .sourceContext("Payment due in 14 days")
                    .build();

            ExtractionResult extractionResult = ExtractionResult.builder()
                    .entities(List.of(extractedEntity))
                    .documentSummary("Contract about payment")
                    .build();

            when(extractionService.extractEntities(name, type, content)).thenReturn(extractionResult);
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
                Document doc = inv.getArgument(0);
                doc.setId("doc-123");
                return doc;
            });

            // When
            Document result = documentService.ingestDocument(name, content, type);

            // Then
            verify(documentRepository).save(documentCaptor.capture());
            Document savedDoc = documentCaptor.getValue();

            assertThat(savedDoc.getName()).isEqualTo(name);
            assertThat(savedDoc.getContent()).isEqualTo(content);
            assertThat(savedDoc.getDocumentType()).isEqualTo(type);
            assertThat(savedDoc.getEntities()).hasSize(1);
            assertThat(savedDoc.getEntities().get(0).getName()).isEqualTo("Payment Term");
        }

        @Test
        @DisplayName("should handle empty extraction result")
        void shouldHandleEmptyExtractionResult() {
            // Given
            ExtractionResult emptyResult = ExtractionResult.builder()
                    .entities(List.of())
                    .documentSummary("No entities found")
                    .build();

            when(extractionService.extractEntities(any(), any(), any())).thenReturn(emptyResult);
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Document result = documentService.ingestDocument("Test", "Content", DocumentType.OTHER);

            // Then
            verify(documentRepository).save(documentCaptor.capture());
            assertThat(documentCaptor.getValue().getEntities()).isEmpty();
        }

        @Test
        @DisplayName("should set up entity relationships from extraction")
        void shouldSetUpEntityRelationships() {
            // Given
            ExtractedEntity.ExtractedRelation relation = ExtractedEntity.ExtractedRelation.builder()
                    .targetEntityName("Invoice")
                    .relationshipType(com.conflictguard.domain.RelationshipType.REFERENCES)
                    .build();

            ExtractedEntity entity1 = ExtractedEntity.builder()
                    .name("Payment Term")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("14 days")
                    .relationships(List.of(relation))
                    .build();

            ExtractedEntity entity2 = ExtractedEntity.builder()
                    .name("Invoice")
                    .entityType(EntityType.CLAUSE)
                    .value("Invoice document")
                    .build();

            ExtractionResult result = ExtractionResult.builder()
                    .entities(List.of(entity1, entity2))
                    .build();

            when(extractionService.extractEntities(any(), any(), any())).thenReturn(result);
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            documentService.ingestDocument("Test", "Content", DocumentType.CONTRACT);

            // Then
            verify(documentRepository).save(documentCaptor.capture());
            Document savedDoc = documentCaptor.getValue();

            assertThat(savedDoc.getEntities()).hasSize(2);
            Entity paymentTerm = savedDoc.getEntities().stream()
                    .filter(e -> e.getName().equals("Payment Term"))
                    .findFirst()
                    .orElseThrow();
            assertThat(paymentTerm.getRelatedEntities()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getAllDocuments")
    class GetAllDocumentsTests {

        @Test
        @DisplayName("should return all documents sorted by date")
        void shouldReturnAllDocuments() {
            // Given
            Document doc1 = Document.builder().id("1").name("Doc 1").build();
            Document doc2 = Document.builder().id("2").name("Doc 2").build();
            when(documentRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(doc2, doc1));

            // When
            List<Document> result = documentService.getAllDocuments();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Doc 2");
        }

        @Test
        @DisplayName("should return empty list when no documents")
        void shouldReturnEmptyList() {
            when(documentRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

            List<Document> result = documentService.getAllDocuments();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDocumentById")
    class GetDocumentByIdTests {

        @Test
        @DisplayName("should return document when found")
        void shouldReturnDocumentWhenFound() {
            Document doc = Document.builder().id("123").name("Test").build();
            when(documentRepository.findById("123")).thenReturn(Optional.of(doc));

            Optional<Document> result = documentService.getDocumentById("123");

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Test");
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            when(documentRepository.findById("999")).thenReturn(Optional.empty());

            Optional<Document> result = documentService.getDocumentById("999");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteDocument")
    class DeleteDocumentTests {

        @Test
        @DisplayName("should delete existing document")
        void shouldDeleteExistingDocument() {
            when(documentRepository.existsById("123")).thenReturn(true);

            boolean result = documentService.deleteDocument("123");

            assertThat(result).isTrue();
            verify(documentRepository).deleteById("123");
        }

        @Test
        @DisplayName("should return false for non-existing document")
        void shouldReturnFalseForNonExisting() {
            when(documentRepository.existsById("999")).thenReturn(false);

            boolean result = documentService.deleteDocument("999");

            assertThat(result).isFalse();
            verify(documentRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("getDocumentsByIds")
    class GetDocumentsByIdsTests {

        @Test
        @DisplayName("should return documents for given IDs")
        void shouldReturnDocumentsForIds() {
            List<String> ids = List.of("1", "2");
            Document doc1 = Document.builder().id("1").build();
            Document doc2 = Document.builder().id("2").build();
            when(documentRepository.findAllById(ids)).thenReturn(List.of(doc1, doc2));

            List<Document> result = documentService.getDocumentsByIds(ids);

            assertThat(result).hasSize(2);
        }
    }
}
