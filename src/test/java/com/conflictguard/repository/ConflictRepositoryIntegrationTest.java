package com.conflictguard.repository;

import com.conflictguard.domain.*;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataNeo4jTest
@Testcontainers
@DisplayName("ConflictRepository Integration Tests")
class ConflictRepositoryIntegrationTest {

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
    private ConflictRepository conflictRepository;

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @BeforeEach
    void setUp() {
        conflictRepository.deleteAll();
        documentRepository.deleteAll();
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class CrudOperationsTests {

        @Test
        @DisplayName("should save and retrieve conflict")
        void shouldSaveAndRetrieveConflict() {
            Entity entity = entityRepository.save(Entity.builder()
                    .name("Test Entity")
                    .entityType(EntityType.TIME_PERIOD)
                    .build());

            Conflict conflict = Conflict.builder()
                    .description("Test conflict description")
                    .severity(ConflictSeverity.HIGH)
                    .reasoning("Test reasoning")
                    .legalPrinciple("Lex Specialis")
                    .detectedAt(LocalDateTime.now())
                    .entities(List.of(entity))
                    .build();

            Conflict saved = conflictRepository.save(conflict);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getDescription()).isEqualTo("Test conflict description");
            assertThat(saved.getSeverity()).isEqualTo(ConflictSeverity.HIGH);
            assertThat(saved.getLegalPrinciple()).isEqualTo("Lex Specialis");
        }

        @Test
        @DisplayName("should delete conflict")
        void shouldDeleteConflict() {
            Conflict conflict = Conflict.builder()
                    .description("To Delete")
                    .severity(ConflictSeverity.LOW)
                    .build();
            Conflict saved = conflictRepository.save(conflict);

            conflictRepository.deleteById(saved.getId());

            assertThat(conflictRepository.findById(saved.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("findBySeverity")
    class FindBySeverityTests {

        @Test
        @DisplayName("should find conflicts by severity")
        void shouldFindBySeverity() {
            Conflict high = Conflict.builder()
                    .description("High severity")
                    .severity(ConflictSeverity.HIGH)
                    .build();

            Conflict low = Conflict.builder()
                    .description("Low severity")
                    .severity(ConflictSeverity.LOW)
                    .build();

            Conflict critical = Conflict.builder()
                    .description("Critical severity")
                    .severity(ConflictSeverity.CRITICAL)
                    .build();

            conflictRepository.saveAll(List.of(high, low, critical));

            List<Conflict> highConflicts = conflictRepository.findBySeverity(ConflictSeverity.HIGH);

            assertThat(highConflicts).hasSize(1);
            assertThat(highConflicts.get(0).getDescription()).isEqualTo("High severity");
        }

        @Test
        @DisplayName("should return empty when no conflicts match severity")
        void shouldReturnEmptyWhenNoMatch() {
            Conflict conflict = Conflict.builder()
                    .description("Low conflict")
                    .severity(ConflictSeverity.LOW)
                    .build();
            conflictRepository.save(conflict);

            List<Conflict> critical = conflictRepository.findBySeverity(ConflictSeverity.CRITICAL);

            assertThat(critical).isEmpty();
        }

        @Test
        @DisplayName("should return multiple conflicts of same severity")
        void shouldReturnMultipleOfSameSeverity() {
            Conflict h1 = Conflict.builder().description("High 1").severity(ConflictSeverity.HIGH).build();
            Conflict h2 = Conflict.builder().description("High 2").severity(ConflictSeverity.HIGH).build();
            conflictRepository.saveAll(List.of(h1, h2));

            List<Conflict> highConflicts = conflictRepository.findBySeverity(ConflictSeverity.HIGH);

            assertThat(highConflicts).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findByEntityId")
    class FindByEntityIdTests {

        @Test
        @DisplayName("should find conflicts involving entity")
        void shouldFindConflictsByEntityId() {
            Entity entity = entityRepository.save(Entity.builder()
                    .name("Involved Entity")
                    .entityType(EntityType.TIME_PERIOD)
                    .build());

            Conflict conflict = Conflict.builder()
                    .description("Conflict involving entity")
                    .severity(ConflictSeverity.MEDIUM)
                    .entities(List.of(entity))
                    .build();
            conflictRepository.save(conflict);

            List<Conflict> conflicts = conflictRepository.findByEntityId(entity.getId());

            assertThat(conflicts).hasSize(1);
            assertThat(conflicts.get(0).getDescription()).isEqualTo("Conflict involving entity");
        }

        @Test
        @DisplayName("should return empty when entity has no conflicts")
        void shouldReturnEmptyWhenNoConflicts() {
            Entity entity = entityRepository.save(Entity.builder()
                    .name("Peaceful Entity")
                    .entityType(EntityType.CLAUSE)
                    .build());

            List<Conflict> conflicts = conflictRepository.findByEntityId(entity.getId());

            assertThat(conflicts).isEmpty();
        }

        @Test
        @DisplayName("should find multiple conflicts for same entity")
        void shouldFindMultipleConflictsForEntity() {
            Entity entity = entityRepository.save(Entity.builder()
                    .name("Problematic Entity")
                    .entityType(EntityType.OBLIGATION)
                    .build());

            Conflict c1 = Conflict.builder()
                    .description("Conflict 1")
                    .severity(ConflictSeverity.HIGH)
                    .entities(List.of(entity))
                    .build();

            Conflict c2 = Conflict.builder()
                    .description("Conflict 2")
                    .severity(ConflictSeverity.MEDIUM)
                    .entities(List.of(entity))
                    .build();

            conflictRepository.saveAll(List.of(c1, c2));

            List<Conflict> conflicts = conflictRepository.findByEntityId(entity.getId());

            assertThat(conflicts).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findByDocumentIds")
    class FindByDocumentIdsTests {

        @Test
        @DisplayName("should find conflicts for documents via entities")
        void shouldFindConflictsByDocumentIds() {
            Entity entity = Entity.builder()
                    .name("Doc Entity")
                    .entityType(EntityType.TIME_PERIOD)
                    .build();

            Document document = Document.builder()
                    .name("Test Document")
                    .content("Content")
                    .documentType(DocumentType.CONTRACT)
                    .entities(List.of(entity))
                    .build();
            Document savedDoc = documentRepository.save(document);
            Entity savedEntity = savedDoc.getEntities().get(0);

            Conflict conflict = Conflict.builder()
                    .description("Document conflict")
                    .severity(ConflictSeverity.HIGH)
                    .entities(List.of(savedEntity))
                    .build();
            conflictRepository.save(conflict);

            List<Conflict> conflicts = conflictRepository.findByDocumentIds(List.of(savedDoc.getId()));

            assertThat(conflicts).hasSize(1);
            assertThat(conflicts.get(0).getDescription()).isEqualTo("Document conflict");
        }

        @Test
        @DisplayName("should return distinct conflicts when documents share conflicts")
        void shouldReturnDistinctConflicts() {
            Entity e1 = Entity.builder().name("Entity 1").entityType(EntityType.CLAUSE).build();
            Entity e2 = Entity.builder().name("Entity 2").entityType(EntityType.CLAUSE).build();

            Document doc1 = Document.builder()
                    .name("Doc 1")
                    .content("Content 1")
                    .documentType(DocumentType.CONTRACT)
                    .entities(List.of(e1))
                    .build();

            Document doc2 = Document.builder()
                    .name("Doc 2")
                    .content("Content 2")
                    .documentType(DocumentType.REGULATION)
                    .entities(List.of(e2))
                    .build();

            Document saved1 = documentRepository.save(doc1);
            Document saved2 = documentRepository.save(doc2);

            // Create conflict involving entities from both documents
            Conflict conflict = Conflict.builder()
                    .description("Shared conflict")
                    .severity(ConflictSeverity.CRITICAL)
                    .entities(List.of(saved1.getEntities().get(0), saved2.getEntities().get(0)))
                    .build();
            conflictRepository.save(conflict);

            List<Conflict> conflicts = conflictRepository.findByDocumentIds(
                    List.of(saved1.getId(), saved2.getId()));

            // Should return one conflict, not two (distinct)
            assertThat(conflicts).hasSize(1);
        }

        @Test
        @DisplayName("should return empty when documents have no conflicts")
        void shouldReturnEmptyWhenNoConflicts() {
            Document doc = Document.builder()
                    .name("Clean Doc")
                    .content("Content")
                    .documentType(DocumentType.OTHER)
                    .build();
            Document saved = documentRepository.save(doc);

            List<Conflict> conflicts = conflictRepository.findByDocumentIds(List.of(saved.getId()));

            assertThat(conflicts).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllOrderByDetectedAtDesc")
    class FindAllOrderedTests {

        @Test
        @DisplayName("should return conflicts ordered by detection date descending")
        void shouldReturnOrderedByDetectedAtDesc() {
            Conflict older = Conflict.builder()
                    .description("Older conflict")
                    .severity(ConflictSeverity.LOW)
                    .detectedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                    .build();

            Conflict newer = Conflict.builder()
                    .description("Newer conflict")
                    .severity(ConflictSeverity.HIGH)
                    .detectedAt(LocalDateTime.of(2024, 6, 15, 10, 0))
                    .build();

            conflictRepository.save(older);
            conflictRepository.save(newer);

            List<Conflict> conflicts = conflictRepository.findAllOrderByDetectedAtDesc();

            assertThat(conflicts).hasSize(2);
            assertThat(conflicts.get(0).getDescription()).isEqualTo("Newer conflict");
            assertThat(conflicts.get(1).getDescription()).isEqualTo("Older conflict");
        }

        @Test
        @DisplayName("should return empty list when no conflicts exist")
        void shouldReturnEmptyWhenNoConflicts() {
            List<Conflict> conflicts = conflictRepository.findAllOrderByDetectedAtDesc();

            assertThat(conflicts).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByInvolvedEntityIds")
    class FindByInvolvedEntityIdsTests {

        @Test
        @DisplayName("should find conflicts involving at least 2 of specified entities")
        void shouldFindConflictsWithMultipleEntities() {
            Entity e1 = entityRepository.save(Entity.builder()
                    .name("Entity 1")
                    .entityType(EntityType.TIME_PERIOD)
                    .build());

            Entity e2 = entityRepository.save(Entity.builder()
                    .name("Entity 2")
                    .entityType(EntityType.TIME_PERIOD)
                    .build());

            Conflict conflict = Conflict.builder()
                    .description("Multi-entity conflict")
                    .severity(ConflictSeverity.HIGH)
                    .entities(List.of(e1, e2))
                    .build();
            conflictRepository.save(conflict);

            List<Conflict> conflicts = conflictRepository.findByInvolvedEntityIds(
                    List.of(e1.getId(), e2.getId()));

            assertThat(conflicts).hasSize(1);
        }

        @Test
        @DisplayName("should not return conflicts with only one matching entity")
        void shouldNotReturnSingleEntityConflicts() {
            Entity e1 = entityRepository.save(Entity.builder()
                    .name("Entity 1")
                    .entityType(EntityType.CLAUSE)
                    .build());

            Entity e2 = entityRepository.save(Entity.builder()
                    .name("Entity 2")
                    .entityType(EntityType.CLAUSE)
                    .build());

            Entity e3 = entityRepository.save(Entity.builder()
                    .name("Entity 3")
                    .entityType(EntityType.CLAUSE)
                    .build());

            // Conflict involves e1 and e3
            Conflict conflict = Conflict.builder()
                    .description("Conflict")
                    .severity(ConflictSeverity.MEDIUM)
                    .entities(List.of(e1, e3))
                    .build();
            conflictRepository.save(conflict);

            // Query with e1 and e2 (only e1 matches)
            List<Conflict> conflicts = conflictRepository.findByInvolvedEntityIds(
                    List.of(e1.getId(), e2.getId()));

            assertThat(conflicts).isEmpty();
        }

        @Test
        @DisplayName("should return empty when no entities match")
        void shouldReturnEmptyWhenNoMatch() {
            List<Conflict> conflicts = conflictRepository.findByInvolvedEntityIds(
                    List.of("nonexistent1", "nonexistent2"));

            assertThat(conflicts).isEmpty();
        }
    }

    @Nested
    @DisplayName("Conflict with Entities Relationship")
    class ConflictEntityRelationshipTests {

        @Test
        @DisplayName("should persist conflict with multiple entities")
        void shouldPersistConflictWithMultipleEntities() {
            Entity e1 = entityRepository.save(Entity.builder()
                    .name("Payment Term Contract")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("14 days")
                    .build());

            Entity e2 = entityRepository.save(Entity.builder()
                    .name("Payment Term T&C")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("30 days")
                    .build());

            Conflict conflict = Conflict.builder()
                    .description("Payment term mismatch")
                    .severity(ConflictSeverity.HIGH)
                    .reasoning("14 days vs 30 days payment terms")
                    .legalPrinciple("Lex Specialis")
                    .entities(List.of(e1, e2))
                    .build();

            Conflict saved = conflictRepository.save(conflict);

            assertThat(saved.getEntities()).hasSize(2);
            assertThat(saved.getEntities()).extracting(Entity::getName)
                    .containsExactlyInAnyOrder("Payment Term Contract", "Payment Term T&C");
        }
    }
}
