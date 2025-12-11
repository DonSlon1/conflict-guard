package com.conflictguard.repository;

import com.conflictguard.domain.Document;
import com.conflictguard.domain.DocumentType;
import com.conflictguard.domain.Entity;
import com.conflictguard.domain.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataNeo4jTest
@Testcontainers
@DisplayName("DocumentRepository Integration Tests")
class DocumentRepositoryIntegrationTest {

    @Container
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5-community")
            .withAdminPassword("testpassword");

    @DynamicPropertySource
    static void neo4jProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", neo4jContainer::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> "testpassword");
    }

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private EntityRepository entityRepository;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        entityRepository.deleteAll();
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class CrudOperationsTests {

        @Test
        @DisplayName("should save and retrieve document")
        void shouldSaveAndRetrieveDocument() {
            Document document = Document.builder()
                    .name("Test Contract")
                    .content("Contract content here")
                    .documentType(DocumentType.CONTRACT)
                    .createdAt(LocalDateTime.now())
                    .build();

            Document saved = documentRepository.save(document);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo("Test Contract");
            assertThat(saved.getDocumentType()).isEqualTo(DocumentType.CONTRACT);
        }

        @Test
        @DisplayName("should update document")
        void shouldUpdateDocument() {
            Document document = Document.builder()
                    .name("Original Name")
                    .content("Content")
                    .documentType(DocumentType.CONTRACT)
                    .build();
            Document saved = documentRepository.save(document);

            saved.setName("Updated Name");
            documentRepository.save(saved);

            Optional<Document> retrieved = documentRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getName()).isEqualTo("Updated Name");
        }

        @Test
        @DisplayName("should delete document")
        void shouldDeleteDocument() {
            Document document = Document.builder()
                    .name("To Delete")
                    .content("Content")
                    .documentType(DocumentType.OTHER)
                    .build();
            Document saved = documentRepository.save(document);
            String id = saved.getId();

            documentRepository.deleteById(id);

            assertThat(documentRepository.findById(id)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByName")
    class FindByNameTests {

        @Test
        @DisplayName("should find document by exact name")
        void shouldFindByName() {
            Document document = Document.builder()
                    .name("Unique Contract Name")
                    .content("Content")
                    .documentType(DocumentType.CONTRACT)
                    .build();
            documentRepository.save(document);

            Optional<Document> found = documentRepository.findByName("Unique Contract Name");

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Unique Contract Name");
        }

        @Test
        @DisplayName("should return empty when name not found")
        void shouldReturnEmptyWhenNotFound() {
            Optional<Document> found = documentRepository.findByName("Nonexistent");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllByOrderByCreatedAtDesc")
    class FindAllOrderedTests {

        @Test
        @DisplayName("should return documents ordered by creation date descending")
        void shouldReturnOrderedByCreatedAtDesc() {
            Document older = Document.builder()
                    .name("Older Document")
                    .content("Content 1")
                    .documentType(DocumentType.CONTRACT)
                    .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                    .build();

            Document newer = Document.builder()
                    .name("Newer Document")
                    .content("Content 2")
                    .documentType(DocumentType.REGULATION)
                    .createdAt(LocalDateTime.of(2024, 6, 15, 10, 0))
                    .build();

            documentRepository.save(older);
            documentRepository.save(newer);

            List<Document> documents = documentRepository.findAllByOrderByCreatedAtDesc();

            assertThat(documents).hasSize(2);
            assertThat(documents.get(0).getName()).isEqualTo("Newer Document");
            assertThat(documents.get(1).getName()).isEqualTo("Older Document");
        }

        @Test
        @DisplayName("should return empty list when no documents exist")
        void shouldReturnEmptyListWhenNoDocuments() {
            List<Document> documents = documentRepository.findAllByOrderByCreatedAtDesc();

            assertThat(documents).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByEntityId")
    class FindByEntityIdTests {

        @Test
        @DisplayName("should find document containing entity")
        void shouldFindDocumentByEntityId() {
            // Create entity
            Entity entity = Entity.builder()
                    .name("Payment Term")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("30 days")
                    .build();

            // Create document with entity
            Document document = Document.builder()
                    .name("Contract with Entity")
                    .content("Payment term is 30 days")
                    .documentType(DocumentType.CONTRACT)
                    .entities(List.of(entity))
                    .build();

            Document saved = documentRepository.save(document);
            String entityId = saved.getEntities().get(0).getId();

            Optional<Document> found = documentRepository.findByEntityId(entityId);

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Contract with Entity");
        }

        @Test
        @DisplayName("should return empty when entity not found")
        void shouldReturnEmptyWhenEntityNotFound() {
            Optional<Document> found = documentRepository.findByEntityId("nonexistent-entity-id");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should find correct document when multiple documents exist")
        void shouldFindCorrectDocumentAmongMany() {
            Entity entity1 = Entity.builder()
                    .name("Entity 1")
                    .entityType(EntityType.CLAUSE)
                    .build();

            Entity entity2 = Entity.builder()
                    .name("Entity 2")
                    .entityType(EntityType.OBLIGATION)
                    .build();

            Document doc1 = Document.builder()
                    .name("Document 1")
                    .content("Content 1")
                    .documentType(DocumentType.CONTRACT)
                    .entities(List.of(entity1))
                    .build();

            Document doc2 = Document.builder()
                    .name("Document 2")
                    .content("Content 2")
                    .documentType(DocumentType.REGULATION)
                    .entities(List.of(entity2))
                    .build();

            documentRepository.save(doc1);
            Document savedDoc2 = documentRepository.save(doc2);
            String entity2Id = savedDoc2.getEntities().get(0).getId();

            Optional<Document> found = documentRepository.findByEntityId(entity2Id);

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Document 2");
        }
    }

    @Nested
    @DisplayName("Document with Entities Relationship")
    class DocumentEntityRelationshipTests {

        @Test
        @DisplayName("should persist document with multiple entities")
        void shouldPersistDocumentWithMultipleEntities() {
            Entity entity1 = Entity.builder()
                    .name("Payment Term")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("14 days")
                    .build();

            Entity entity2 = Entity.builder()
                    .name("Late Fee")
                    .entityType(EntityType.PENALTY)
                    .value("0.05% per day")
                    .build();

            Document document = Document.builder()
                    .name("Multi-Entity Contract")
                    .content("Payment in 14 days, late fee 0.05% per day")
                    .documentType(DocumentType.CONTRACT)
                    .entities(List.of(entity1, entity2))
                    .build();

            Document saved = documentRepository.save(document);

            Optional<Document> retrieved = documentRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getEntities()).hasSize(2);
        }
    }
}
