package com.conflictguard.integration;

import com.conflictguard.domain.*;
import com.conflictguard.dto.ExtractionResult;
import com.conflictguard.dto.ExtractedEntity;
import com.conflictguard.dto.ConflictAnalysisDto;
import com.conflictguard.dto.DetectedConflict;
import com.conflictguard.ai.EntityExtractionService;
import com.conflictguard.ai.ConflictReasoningService;
import com.conflictguard.repository.DocumentRepository;
import com.conflictguard.repository.EntityRepository;
import com.conflictguard.repository.ConflictRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
@Testcontainers
@DisplayName("GraphQL Integration Tests")
class GraphQLIntegrationTest {

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
    private HttpGraphQlTester graphQlTester;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private ConflictRepository conflictRepository;

    @MockBean
    private EntityExtractionService extractionService;

    @MockBean
    private ConflictReasoningService reasoningService;

    @BeforeEach
    void setUp() {
        // Clear database before each test
        conflictRepository.deleteAll();
        documentRepository.deleteAll();
    }

    @Nested
    @DisplayName("Document Operations")
    class DocumentOperationsTests {

        @Test
        @DisplayName("should ingest document and extract entities via GraphQL")
        void shouldIngestDocument() {
            // Given - mock AI response
            ExtractedEntity entity = ExtractedEntity.builder()
                    .name("Payment Term")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("14 days")
                    .sourceContext("Splatnost je 14 dní")
                    .build();

            ExtractionResult extractionResult = ExtractionResult.builder()
                    .entities(List.of(entity))
                    .documentSummary("Contract document")
                    .build();

            when(extractionService.extractEntities(any(), any(), any())).thenReturn(extractionResult);

            // When - execute GraphQL mutation
            String mutation = """
                mutation {
                    ingestDocument(input: {
                        name: "Test Contract"
                        content: "Splatnost je 14 dní"
                        documentType: CONTRACT
                    }) {
                        id
                        name
                        documentType
                        entities {
                            id
                            name
                            entityType
                            value
                        }
                    }
                }
                """;

            // Then
            graphQlTester.document(mutation)
                    .execute()
                    .path("ingestDocument.name").entity(String.class).isEqualTo("Test Contract")
                    .path("ingestDocument.documentType").entity(String.class).isEqualTo("CONTRACT")
                    .path("ingestDocument.entities[0].name").entity(String.class).isEqualTo("Payment Term")
                    .path("ingestDocument.entities[0].entityType").entity(String.class).isEqualTo("TIME_PERIOD");
        }

        @Test
        @DisplayName("should query all documents")
        void shouldQueryAllDocuments() {
            // Given - create test documents directly
            Document doc = Document.builder()
                    .name("Query Test Doc")
                    .content("Test content")
                    .documentType(DocumentType.REGULATION)
                    .build();
            documentRepository.save(doc);

            // When & Then
            String query = """
                query {
                    documents {
                        name
                        documentType
                    }
                }
                """;

            graphQlTester.document(query)
                    .execute()
                    .path("documents").entityList(Object.class).hasSize(1)
                    .path("documents[0].name").entity(String.class).isEqualTo("Query Test Doc");
        }

        @Test
        @DisplayName("should query document by ID")
        void shouldQueryDocumentById() {
            // Given
            Document doc = Document.builder()
                    .name("Specific Doc")
                    .content("Content")
                    .documentType(DocumentType.OTHER)
                    .build();
            Document saved = documentRepository.save(doc);

            // When & Then
            String query = """
                query($id: ID!) {
                    document(id: $id) {
                        name
                        documentType
                    }
                }
                """;

            graphQlTester.document(query)
                    .variable("id", saved.getId())
                    .execute()
                    .path("document.name").entity(String.class).isEqualTo("Specific Doc");
        }

        @Test
        @DisplayName("should delete document")
        void shouldDeleteDocument() {
            // Given
            Document doc = Document.builder()
                    .name("To Delete")
                    .content("Content")
                    .documentType(DocumentType.CONTRACT)
                    .build();
            Document saved = documentRepository.save(doc);

            // When
            String mutation = """
                mutation($id: ID!) {
                    deleteDocument(id: $id)
                }
                """;

            graphQlTester.document(mutation)
                    .variable("id", saved.getId())
                    .execute()
                    .path("deleteDocument").entity(Boolean.class).isEqualTo(true);

            // Then
            assertThat(documentRepository.findById(saved.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Entity Operations")
    class EntityOperationsTests {

        @Test
        @DisplayName("should query entities by type")
        void shouldQueryEntitiesByType() {
            // Given - create document with entity
            ExtractedEntity entity = ExtractedEntity.builder()
                    .name("Due Date")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("30 days")
                    .build();

            ExtractionResult result = ExtractionResult.builder()
                    .entities(List.of(entity))
                    .build();

            when(extractionService.extractEntities(any(), any(), any())).thenReturn(result);

            // Ingest via GraphQL
            String ingestMutation = """
                mutation {
                    ingestDocument(input: {
                        name: "Entity Test"
                        content: "Content"
                        documentType: CONTRACT
                    }) { id }
                }
                """;
            graphQlTester.document(ingestMutation).execute();

            // When & Then - query by type
            String query = """
                query {
                    entities(type: TIME_PERIOD) {
                        name
                        entityType
                        value
                    }
                }
                """;

            graphQlTester.document(query)
                    .execute()
                    .path("entities").entityList(Object.class).hasSize(1)
                    .path("entities[0].entityType").entity(String.class).isEqualTo("TIME_PERIOD");
        }
    }

    @Nested
    @DisplayName("Conflict Analysis")
    class ConflictAnalysisTests {

        @Test
        @DisplayName("should analyze conflicts between documents")
        void shouldAnalyzeConflicts() {
            // Given - create two documents
            ExtractedEntity entity1 = ExtractedEntity.builder()
                    .name("Contract Payment Term")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("14 days")
                    .build();

            ExtractedEntity entity2 = ExtractedEntity.builder()
                    .name("Standard Payment Term")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("30 days")
                    .build();

            when(extractionService.extractEntities(any(), any(), any()))
                    .thenReturn(ExtractionResult.builder().entities(List.of(entity1)).build())
                    .thenReturn(ExtractionResult.builder().entities(List.of(entity2)).build());

            // Mock conflict analysis
            DetectedConflict conflict = DetectedConflict.builder()
                    .description("Payment term conflict: 14 days vs 30 days")
                    .severity(ConflictSeverity.HIGH)
                    .reasoning("Different payment terms in contract and T&C")
                    .legalPrinciple("Lex Specialis")
                    .involvedEntityNames(List.of("Contract Payment Term", "Standard Payment Term"))
                    .build();

            when(reasoningService.analyzeConflicts(any())).thenReturn(
                    ConflictAnalysisDto.builder()
                            .conflicts(List.of(conflict))
                            .overallSummary("One conflict detected")
                            .build()
            );

            // Ingest documents
            String ingestMutation = """
                mutation($name: String!, $type: DocumentType!) {
                    ingestDocument(input: {
                        name: $name
                        content: "Content"
                        documentType: $type
                    }) { id }
                }
                """;

            String doc1Id = graphQlTester.document(ingestMutation)
                    .variable("name", "Contract")
                    .variable("type", "CONTRACT")
                    .execute()
                    .path("ingestDocument.id").entity(String.class).get();

            String doc2Id = graphQlTester.document(ingestMutation)
                    .variable("name", "T&C")
                    .variable("type", "TERMS_AND_CONDITIONS")
                    .execute()
                    .path("ingestDocument.id").entity(String.class).get();

            // When - analyze conflicts
            String analyzeMutation = """
                mutation($ids: [ID!]!) {
                    analyzeConflicts(documentIds: $ids) {
                        conflicts {
                            description
                            severity
                            reasoning
                            legalPrinciple
                        }
                        summary
                    }
                }
                """;

            // Then
            graphQlTester.document(analyzeMutation)
                    .variable("ids", List.of(doc1Id, doc2Id))
                    .execute()
                    .path("analyzeConflicts.summary").entity(String.class).isEqualTo("One conflict detected")
                    .path("analyzeConflicts.conflicts[0].severity").entity(String.class).isEqualTo("HIGH")
                    .path("analyzeConflicts.conflicts[0].legalPrinciple").entity(String.class).isEqualTo("Lex Specialis");
        }

        @Test
        @DisplayName("should query existing conflicts by severity")
        void shouldQueryConflictsBySeverity() {
            // Given - create a conflict directly
            Entity entity = Entity.builder()
                    .name("Test Entity")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("Test value")
                    .build();
            entity = entityRepository.save(entity);

            Conflict conflict = Conflict.builder()
                    .description("Test conflict")
                    .severity(ConflictSeverity.CRITICAL)
                    .reasoning("Test reasoning")
                    .entities(List.of(entity))
                    .build();
            conflictRepository.save(conflict);

            // When & Then
            String query = """
                query {
                    conflicts(severity: CRITICAL) {
                        description
                        severity
                    }
                }
                """;

            graphQlTester.document(query)
                    .execute()
                    .path("conflicts").entityList(Object.class).hasSize(1)
                    .path("conflicts[0].severity").entity(String.class).isEqualTo("CRITICAL");
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("should reject empty document name")
        void shouldRejectEmptyName() {
            String mutation = """
                mutation {
                    ingestDocument(input: {
                        name: ""
                        content: "Valid content"
                        documentType: CONTRACT
                    }) { id }
                }
                """;

            graphQlTester.document(mutation)
                    .execute()
                    .errors()
                    .satisfy(errors -> {
                        assertThat(errors).isNotEmpty();
                        assertThat(errors.get(0).getMessage()).contains("name cannot be empty");
                    });
        }

        @Test
        @DisplayName("should reject empty document content")
        void shouldRejectEmptyContent() {
            String mutation = """
                mutation {
                    ingestDocument(input: {
                        name: "Valid name"
                        content: "   "
                        documentType: CONTRACT
                    }) { id }
                }
                """;

            graphQlTester.document(mutation)
                    .execute()
                    .errors()
                    .satisfy(errors -> {
                        assertThat(errors).isNotEmpty();
                        assertThat(errors.get(0).getMessage()).contains("content cannot be empty");
                    });
        }

        @Test
        @DisplayName("should reject empty document IDs for analysis")
        void shouldRejectEmptyDocumentIds() {
            String mutation = """
                mutation {
                    analyzeConflicts(documentIds: []) {
                        summary
                    }
                }
                """;

            graphQlTester.document(mutation)
                    .execute()
                    .errors()
                    .satisfy(errors -> {
                        assertThat(errors).isNotEmpty();
                        assertThat(errors.get(0).getMessage()).contains("At least one document ID");
                    });
        }
    }
}
