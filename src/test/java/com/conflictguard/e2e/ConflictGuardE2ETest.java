package com.conflictguard.e2e;

import com.conflictguard.domain.*;
import com.conflictguard.dto.ConflictAnalysisDto;
import com.conflictguard.dto.DetectedConflict;
import com.conflictguard.dto.ExtractionResult;
import com.conflictguard.dto.ExtractedEntity;
import com.conflictguard.ai.EntityExtractionService;
import com.conflictguard.ai.ConflictReasoningService;
import com.conflictguard.repository.ConflictRepository;
import com.conflictguard.repository.DocumentRepository;
import com.conflictguard.repository.EntityRepository;
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

/**
 * End-to-End tests that verify the complete ConflictGuard workflow:
 * 1. Document ingestion with AI entity extraction
 * 2. Building knowledge graph in Neo4j
 * 3. AI-powered conflict analysis
 * 4. Querying conflicts via GraphQL
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
@Testcontainers
@DisplayName("ConflictGuard End-to-End Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConflictGuardE2ETest {

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
        conflictRepository.deleteAll();
        documentRepository.deleteAll();
    }

    @Nested
    @DisplayName("Complete Conflict Detection Workflow")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CompleteWorkflowTests {

        @Test
        @Order(1)
        @DisplayName("E2E: Should detect payment term conflict between contract and terms")
        void shouldDetectPaymentTermConflict() {
            // === SETUP: Mock AI responses ===

            // Contract extraction - 14 day payment term
            ExtractedEntity contractPaymentTerm = ExtractedEntity.builder()
                    .name("Contract Payment Term")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("14 days")
                    .sourceContext("Buyer shall pay within 14 days of invoice date")
                    .build();

            ExtractionResult contractExtraction = ExtractionResult.builder()
                    .entities(List.of(contractPaymentTerm))
                    .documentSummary("Sales contract with 14-day payment term")
                    .build();

            // Terms extraction - 30 day payment term
            ExtractedEntity termsPaymentTerm = ExtractedEntity.builder()
                    .name("Standard Payment Term")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("30 days")
                    .sourceContext("Standard payment terms are 30 days net")
                    .build();

            ExtractionResult termsExtraction = ExtractionResult.builder()
                    .entities(List.of(termsPaymentTerm))
                    .documentSummary("Standard terms and conditions with 30-day payment")
                    .build();

            when(extractionService.extractEntities(any(), any(), any()))
                    .thenReturn(contractExtraction)
                    .thenReturn(termsExtraction);

            // Conflict analysis result
            DetectedConflict paymentConflict = DetectedConflict.builder()
                    .description("Payment term conflict: Contract specifies 14 days while Terms specify 30 days")
                    .severity(ConflictSeverity.HIGH)
                    .reasoning("The contract's 14-day payment term is more specific and favorable to seller than the standard 30-day term in T&C")
                    .legalPrinciple("Lex Specialis")
                    .involvedEntityNames(List.of("Contract Payment Term", "Standard Payment Term"))
                    .build();

            when(reasoningService.analyzeConflicts(any())).thenReturn(
                    ConflictAnalysisDto.builder()
                            .conflicts(List.of(paymentConflict))
                            .overallSummary("One HIGH severity conflict detected: Payment terms differ between contract and terms")
                            .build()
            );

            // === STEP 1: Ingest Contract ===
            String ingestContractMutation = """
                mutation {
                    ingestDocument(input: {
                        name: "Sales Contract 2024"
                        content: "This Sales Contract stipulates that the Buyer shall pay within 14 days of invoice date. Late payments incur 0.05% daily penalty."
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
                            sourceContext
                        }
                    }
                }
                """;

            String contractId = graphQlTester.document(ingestContractMutation)
                    .execute()
                    .path("ingestDocument.id").entity(String.class).get();

            // Verify contract was saved
            graphQlTester.document(ingestContractMutation)
                    .execute()
                    .path("ingestDocument.name").entity(String.class).isEqualTo("Sales Contract 2024")
                    .path("ingestDocument.documentType").entity(String.class).isEqualTo("CONTRACT")
                    .path("ingestDocument.entities[0].name").entity(String.class).isEqualTo("Contract Payment Term")
                    .path("ingestDocument.entities[0].entityType").entity(String.class).isEqualTo("TIME_PERIOD")
                    .path("ingestDocument.entities[0].value").entity(String.class).isEqualTo("14 days");

            // === STEP 2: Ingest Terms & Conditions ===
            String ingestTermsMutation = """
                mutation {
                    ingestDocument(input: {
                        name: "General Terms and Conditions"
                        content: "Section 5: Payment. Standard payment terms are 30 days net from invoice date."
                        documentType: TERMS_AND_CONDITIONS
                    }) {
                        id
                        name
                        entities {
                            name
                            value
                        }
                    }
                }
                """;

            String termsId = graphQlTester.document(ingestTermsMutation)
                    .execute()
                    .path("ingestDocument.id").entity(String.class).get();

            // === STEP 3: Analyze Conflicts ===
            String analyzeMutation = """
                mutation($docIds: [ID!]!) {
                    analyzeConflicts(documentIds: $docIds) {
                        conflicts {
                            description
                            severity
                            reasoning
                            legalPrinciple
                            involvedEntities {
                                name
                                value
                            }
                        }
                        summary
                    }
                }
                """;

            graphQlTester.document(analyzeMutation)
                    .variable("docIds", List.of(contractId, termsId))
                    .execute()
                    .path("analyzeConflicts.summary").entity(String.class)
                        .satisfies(summary -> assertThat(summary).contains("HIGH"))
                    .path("analyzeConflicts.conflicts").entityList(Object.class).hasSize(1)
                    .path("analyzeConflicts.conflicts[0].severity").entity(String.class).isEqualTo("HIGH")
                    .path("analyzeConflicts.conflicts[0].legalPrinciple").entity(String.class).isEqualTo("Lex Specialis");

            // === STEP 4: Query persisted conflicts ===
            String queryConflicts = """
                query {
                    conflicts(severity: HIGH) {
                        description
                        severity
                        reasoning
                    }
                }
                """;

            graphQlTester.document(queryConflicts)
                    .execute()
                    .path("conflicts").entityList(Object.class).hasSize(1)
                    .path("conflicts[0].severity").entity(String.class).isEqualTo("HIGH");
        }

        @Test
        @Order(2)
        @DisplayName("E2E: Should detect multiple conflicts in complex document set")
        void shouldDetectMultipleConflicts() {
            // Setup multiple entities with conflicts
            ExtractedEntity penaltyContract = ExtractedEntity.builder()
                    .name("Contract Penalty")
                    .entityType(EntityType.PENALTY)
                    .value("5% per month")
                    .sourceContext("Late payment penalty: 5% per month")
                    .build();

            ExtractedEntity penaltyRegulation = ExtractedEntity.builder()
                    .name("Legal Max Penalty")
                    .entityType(EntityType.PENALTY)
                    .value("0.5% per month")
                    .sourceContext("Maximum permissible penalty: 0.5% per month")
                    .build();

            ExtractedEntity terminationContract = ExtractedEntity.builder()
                    .name("Contract Termination")
                    .entityType(EntityType.CLAUSE)
                    .value("30 days notice")
                    .sourceContext("Either party may terminate with 30 days notice")
                    .build();

            ExtractedEntity terminationRegulation = ExtractedEntity.builder()
                    .name("Legal Min Notice")
                    .entityType(EntityType.CLAUSE)
                    .value("90 days notice")
                    .sourceContext("Minimum termination notice: 90 days")
                    .build();

            when(extractionService.extractEntities(any(), any(), any()))
                    .thenReturn(ExtractionResult.builder()
                            .entities(List.of(penaltyContract, terminationContract))
                            .build())
                    .thenReturn(ExtractionResult.builder()
                            .entities(List.of(penaltyRegulation, terminationRegulation))
                            .build());

            // Two conflicts detected
            DetectedConflict penaltyConflict = DetectedConflict.builder()
                    .description("Penalty rate exceeds legal maximum")
                    .severity(ConflictSeverity.CRITICAL)
                    .reasoning("Contract penalty (5%) exceeds regulatory maximum (0.5%)")
                    .legalPrinciple("Regulatory Compliance")
                    .involvedEntityNames(List.of("Contract Penalty", "Legal Max Penalty"))
                    .build();

            DetectedConflict terminationConflict = DetectedConflict.builder()
                    .description("Termination notice period below legal minimum")
                    .severity(ConflictSeverity.HIGH)
                    .reasoning("30-day notice is less than 90-day legal requirement")
                    .legalPrinciple("Consumer Protection")
                    .involvedEntityNames(List.of("Contract Termination", "Legal Min Notice"))
                    .build();

            when(reasoningService.analyzeConflicts(any())).thenReturn(
                    ConflictAnalysisDto.builder()
                            .conflicts(List.of(penaltyConflict, terminationConflict))
                            .overallSummary("Two conflicts: 1 CRITICAL (penalty), 1 HIGH (termination)")
                            .build()
            );

            // Ingest documents
            String contractId = ingestDocument("Problematic Contract", "CONTRACT",
                    "Penalty 5% per month. Termination with 30 days notice.");
            String regulationId = ingestDocument("Industry Regulation", "REGULATION",
                    "Max penalty 0.5%. Min notice 90 days.");

            // Analyze
            String analyzeMutation = """
                mutation($docIds: [ID!]!) {
                    analyzeConflicts(documentIds: $docIds) {
                        conflicts {
                            description
                            severity
                        }
                        summary
                    }
                }
                """;

            graphQlTester.document(analyzeMutation)
                    .variable("docIds", List.of(contractId, regulationId))
                    .execute()
                    .path("analyzeConflicts.conflicts").entityList(Object.class).hasSize(2)
                    .path("analyzeConflicts.summary").entity(String.class)
                        .satisfies(s -> {
                            assertThat(s).contains("CRITICAL");
                            assertThat(s).contains("HIGH");
                        });

            // Verify CRITICAL conflicts can be queried
            graphQlTester.document("query { conflicts(severity: CRITICAL) { description } }")
                    .execute()
                    .path("conflicts").entityList(Object.class).hasSize(1);
        }

        @Test
        @Order(3)
        @DisplayName("E2E: Should handle documents with no conflicts")
        void shouldHandleNoConflicts() {
            ExtractedEntity entity1 = ExtractedEntity.builder()
                    .name("Compatible Term 1")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("30 days")
                    .build();

            ExtractedEntity entity2 = ExtractedEntity.builder()
                    .name("Compatible Term 2")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("30 days")
                    .build();

            when(extractionService.extractEntities(any(), any(), any()))
                    .thenReturn(ExtractionResult.builder().entities(List.of(entity1)).build())
                    .thenReturn(ExtractionResult.builder().entities(List.of(entity2)).build());

            when(reasoningService.analyzeConflicts(any())).thenReturn(
                    ConflictAnalysisDto.builder()
                            .conflicts(List.of())
                            .overallSummary("No conflicts detected - documents are compatible")
                            .build()
            );

            String doc1Id = ingestDocument("Doc A", "CONTRACT", "Payment in 30 days");
            String doc2Id = ingestDocument("Doc B", "TERMS_AND_CONDITIONS", "Standard 30 day payment");

            String analyzeMutation = """
                mutation($docIds: [ID!]!) {
                    analyzeConflicts(documentIds: $docIds) {
                        conflicts {
                            description
                        }
                        summary
                    }
                }
                """;

            graphQlTester.document(analyzeMutation)
                    .variable("docIds", List.of(doc1Id, doc2Id))
                    .execute()
                    .path("analyzeConflicts.conflicts").entityList(Object.class).hasSize(0)
                    .path("analyzeConflicts.summary").entity(String.class)
                        .satisfies(s -> assertThat(s).contains("No conflicts"));
        }
    }

    @Nested
    @DisplayName("Graph Navigation Queries")
    class GraphNavigationTests {

        @Test
        @DisplayName("E2E: Should navigate from entity to source document")
        void shouldNavigateEntityToDocument() {
            ExtractedEntity entity = ExtractedEntity.builder()
                    .name("Test Entity")
                    .entityType(EntityType.OBLIGATION)
                    .value("Must deliver within 5 business days")
                    .build();

            when(extractionService.extractEntities(any(), any(), any()))
                    .thenReturn(ExtractionResult.builder().entities(List.of(entity)).build());

            String docId = ingestDocument("Delivery Contract", "CONTRACT", "Must deliver within 5 business days");

            // Query entity with source document
            String query = """
                query {
                    entities {
                        name
                        sourceDocument {
                            name
                            documentType
                        }
                    }
                }
                """;

            graphQlTester.document(query)
                    .execute()
                    .path("entities[0].name").entity(String.class).isEqualTo("Test Entity")
                    .path("entities[0].sourceDocument.name").entity(String.class).isEqualTo("Delivery Contract");
        }

        @Test
        @DisplayName("E2E: Should query entities filtered by type")
        void shouldQueryEntitiesByType() {
            ExtractedEntity timePeriod = ExtractedEntity.builder()
                    .name("Payment Period")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("14 days")
                    .build();

            ExtractedEntity monetary = ExtractedEntity.builder()
                    .name("Contract Value")
                    .entityType(EntityType.MONETARY_VALUE)
                    .value("$50,000")
                    .build();

            when(extractionService.extractEntities(any(), any(), any()))
                    .thenReturn(ExtractionResult.builder().entities(List.of(timePeriod, monetary)).build());

            ingestDocument("Mixed Entity Doc", "CONTRACT", "Payment in 14 days, value $50,000");

            // Query only TIME_PERIOD entities
            String query = """
                query {
                    entities(type: TIME_PERIOD) {
                        name
                        entityType
                    }
                }
                """;

            graphQlTester.document(query)
                    .execute()
                    .path("entities").entityList(Object.class).hasSize(1)
                    .path("entities[0].entityType").entity(String.class).isEqualTo("TIME_PERIOD");
        }

        @Test
        @DisplayName("E2E: Should query conflicts for specific documents")
        void shouldQueryConflictsForDocuments() {
            // Setup
            ExtractedEntity e1 = ExtractedEntity.builder()
                    .name("Entity A").entityType(EntityType.CLAUSE).value("A").build();
            ExtractedEntity e2 = ExtractedEntity.builder()
                    .name("Entity B").entityType(EntityType.CLAUSE).value("B").build();

            when(extractionService.extractEntities(any(), any(), any()))
                    .thenReturn(ExtractionResult.builder().entities(List.of(e1)).build())
                    .thenReturn(ExtractionResult.builder().entities(List.of(e2)).build());

            DetectedConflict conflict = DetectedConflict.builder()
                    .description("Test conflict")
                    .severity(ConflictSeverity.MEDIUM)
                    .involvedEntityNames(List.of("Entity A", "Entity B"))
                    .build();

            when(reasoningService.analyzeConflicts(any())).thenReturn(
                    ConflictAnalysisDto.builder()
                            .conflicts(List.of(conflict))
                            .overallSummary("One conflict")
                            .build()
            );

            String doc1 = ingestDocument("Doc 1", "CONTRACT", "Content A");
            String doc2 = ingestDocument("Doc 2", "REGULATION", "Content B");

            // Create conflict via analysis
            graphQlTester.document("mutation($ids: [ID!]!) { analyzeConflicts(documentIds: $ids) { summary } }")
                    .variable("ids", List.of(doc1, doc2))
                    .execute();

            // Query conflicts for specific documents
            String query = """
                query($docIds: [ID!]!) {
                    conflictsForDocuments(documentIds: $docIds) {
                        description
                        severity
                    }
                }
                """;

            graphQlTester.document(query)
                    .variable("docIds", List.of(doc1, doc2))
                    .execute()
                    .path("conflictsForDocuments").entityList(Object.class).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("E2E: Should reject empty document name")
        void shouldRejectEmptyDocumentName() {
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
        @DisplayName("E2E: Should reject empty document content")
        void shouldRejectEmptyDocumentContent() {
            String mutation = """
                mutation {
                    ingestDocument(input: {
                        name: "Valid Name"
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
        @DisplayName("E2E: Should reject empty document IDs for conflict analysis")
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

        @Test
        @DisplayName("E2E: Should handle nonexistent document ID gracefully")
        void shouldHandleNonexistentDocument() {
            String query = """
                query {
                    document(id: "nonexistent-id-12345") {
                        name
                    }
                }
                """;

            graphQlTester.document(query)
                    .execute()
                    .path("document").valueIsNull();
        }
    }

    // Helper method
    private String ingestDocument(String name, String type, String content) {
        String mutation = """
            mutation($name: String!, $content: String!, $type: DocumentType!) {
                ingestDocument(input: {
                    name: $name
                    content: $content
                    documentType: $type
                }) { id }
            }
            """;

        return graphQlTester.document(mutation)
                .variable("name", name)
                .variable("content", content)
                .variable("type", type)
                .execute()
                .path("ingestDocument.id").entity(String.class).get();
    }
}
