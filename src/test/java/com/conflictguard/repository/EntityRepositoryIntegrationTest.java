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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataNeo4jTest
@Testcontainers
@DisplayName("EntityRepository Integration Tests")
class EntityRepositoryIntegrationTest {

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
    private EntityRepository entityRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        entityRepository.deleteAll();
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class CrudOperationsTests {

        @Test
        @DisplayName("should save and retrieve entity")
        void shouldSaveAndRetrieveEntity() {
            Entity entity = Entity.builder()
                    .name("Payment Term")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("30 days")
                    .sourceContext("Payment must be made within 30 days")
                    .build();

            Entity saved = entityRepository.save(entity);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo("Payment Term");
            assertThat(saved.getEntityType()).isEqualTo(EntityType.TIME_PERIOD);
            assertThat(saved.getValue()).isEqualTo("30 days");
        }

        @Test
        @DisplayName("should delete entity")
        void shouldDeleteEntity() {
            Entity entity = Entity.builder()
                    .name("To Delete")
                    .entityType(EntityType.CLAUSE)
                    .build();
            Entity saved = entityRepository.save(entity);

            entityRepository.deleteById(saved.getId());

            assertThat(entityRepository.findById(saved.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByEntityType")
    class FindByEntityTypeTests {

        @Test
        @DisplayName("should find entities by type")
        void shouldFindByEntityType() {
            Entity timePeriod = Entity.builder()
                    .name("Payment Term")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("14 days")
                    .build();

            Entity monetary = Entity.builder()
                    .name("Contract Value")
                    .entityType(EntityType.MONETARY_VALUE)
                    .value("$10,000")
                    .build();

            entityRepository.saveAll(List.of(timePeriod, monetary));

            List<Entity> timePeriods = entityRepository.findByEntityType(EntityType.TIME_PERIOD);

            assertThat(timePeriods).hasSize(1);
            assertThat(timePeriods.get(0).getName()).isEqualTo("Payment Term");
        }

        @Test
        @DisplayName("should return empty list when no entities of type exist")
        void shouldReturnEmptyWhenNoMatchingType() {
            Entity entity = Entity.builder()
                    .name("Clause")
                    .entityType(EntityType.CLAUSE)
                    .build();
            entityRepository.save(entity);

            List<Entity> penalties = entityRepository.findByEntityType(EntityType.PENALTY);

            assertThat(penalties).isEmpty();
        }

        @Test
        @DisplayName("should return multiple entities of same type")
        void shouldReturnMultipleOfSameType() {
            Entity term1 = Entity.builder()
                    .name("Payment Term 1")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("14 days")
                    .build();

            Entity term2 = Entity.builder()
                    .name("Payment Term 2")
                    .entityType(EntityType.TIME_PERIOD)
                    .value("30 days")
                    .build();

            entityRepository.saveAll(List.of(term1, term2));

            List<Entity> timePeriods = entityRepository.findByEntityType(EntityType.TIME_PERIOD);

            assertThat(timePeriods).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findByName")
    class FindByNameTests {

        @Test
        @DisplayName("should find entity by exact name")
        void shouldFindByName() {
            Entity entity = Entity.builder()
                    .name("Unique Entity Name")
                    .entityType(EntityType.OBLIGATION)
                    .build();
            entityRepository.save(entity);

            Optional<Entity> found = entityRepository.findByName("Unique Entity Name");

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Unique Entity Name");
        }

        @Test
        @DisplayName("should return empty when name not found")
        void shouldReturnEmptyWhenNotFound() {
            Optional<Entity> found = entityRepository.findByName("Nonexistent");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByNameIn")
    class FindByNameInTests {

        @Test
        @DisplayName("should find entities by list of names")
        void shouldFindByNameIn() {
            Entity e1 = Entity.builder().name("Entity A").entityType(EntityType.CLAUSE).build();
            Entity e2 = Entity.builder().name("Entity B").entityType(EntityType.CLAUSE).build();
            Entity e3 = Entity.builder().name("Entity C").entityType(EntityType.CLAUSE).build();
            entityRepository.saveAll(List.of(e1, e2, e3));

            List<Entity> found = entityRepository.findByNameIn(List.of("Entity A", "Entity C"));

            assertThat(found).hasSize(2);
            assertThat(found).extracting(Entity::getName)
                    .containsExactlyInAnyOrder("Entity A", "Entity C");
        }

        @Test
        @DisplayName("should return only matching entities when some names don't exist")
        void shouldReturnOnlyMatchingEntities() {
            Entity e1 = Entity.builder().name("Exists").entityType(EntityType.CLAUSE).build();
            entityRepository.save(e1);

            List<Entity> found = entityRepository.findByNameIn(List.of("Exists", "DoesNotExist"));

            assertThat(found).hasSize(1);
            assertThat(found.get(0).getName()).isEqualTo("Exists");
        }

        @Test
        @DisplayName("should return empty list when no names match")
        void shouldReturnEmptyWhenNoMatch() {
            List<Entity> found = entityRepository.findByNameIn(List.of("NonexistentA", "NonexistentB"));

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByDocumentIds")
    class FindByDocumentIdsTests {

        @Test
        @DisplayName("should find entities from multiple documents")
        void shouldFindEntitiesFromMultipleDocuments() {
            Entity e1 = Entity.builder().name("Entity 1").entityType(EntityType.TIME_PERIOD).build();
            Entity e2 = Entity.builder().name("Entity 2").entityType(EntityType.MONETARY_VALUE).build();

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

            List<Entity> entities = entityRepository.findByDocumentIds(
                    List.of(saved1.getId(), saved2.getId()));

            assertThat(entities).hasSize(2);
            assertThat(entities).extracting(Entity::getName)
                    .containsExactlyInAnyOrder("Entity 1", "Entity 2");
        }

        @Test
        @DisplayName("should return distinct entities when document IDs overlap")
        void shouldReturnDistinctEntities() {
            Entity entity = Entity.builder()
                    .name("Shared Entity")
                    .entityType(EntityType.CLAUSE)
                    .build();

            Document doc = Document.builder()
                    .name("Single Doc")
                    .content("Content")
                    .documentType(DocumentType.CONTRACT)
                    .entities(List.of(entity))
                    .build();

            Document saved = documentRepository.save(doc);

            // Query with same ID twice
            List<Entity> entities = entityRepository.findByDocumentIds(
                    List.of(saved.getId(), saved.getId()));

            assertThat(entities).hasSize(1);
        }

        @Test
        @DisplayName("should return empty when document has no entities")
        void shouldReturnEmptyWhenNoEntities() {
            Document doc = Document.builder()
                    .name("Empty Doc")
                    .content("Content")
                    .documentType(DocumentType.OTHER)
                    .build();
            Document saved = documentRepository.save(doc);

            List<Entity> entities = entityRepository.findByDocumentIds(List.of(saved.getId()));

            assertThat(entities).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByDocumentId")
    class FindByDocumentIdTests {

        @Test
        @DisplayName("should find entities for single document")
        void shouldFindEntitiesForDocument() {
            Entity e1 = Entity.builder().name("Entity 1").entityType(EntityType.CLAUSE).build();
            Entity e2 = Entity.builder().name("Entity 2").entityType(EntityType.OBLIGATION).build();

            Document doc = Document.builder()
                    .name("Document")
                    .content("Content")
                    .documentType(DocumentType.CONTRACT)
                    .entities(List.of(e1, e2))
                    .build();

            Document saved = documentRepository.save(doc);

            List<Entity> entities = entityRepository.findByDocumentId(saved.getId());

            assertThat(entities).hasSize(2);
        }

        @Test
        @DisplayName("should return empty for nonexistent document")
        void shouldReturnEmptyForNonexistent() {
            List<Entity> entities = entityRepository.findByDocumentId("nonexistent");

            assertThat(entities).isEmpty();
        }
    }

    @Nested
    @DisplayName("findEntitiesWithConflicts")
    class FindEntitiesWithConflictsTests {

        @Test
        @DisplayName("should return empty when no conflict relationships exist")
        void shouldReturnEmptyWhenNoConflicts() {
            Entity entity = Entity.builder()
                    .name("Peaceful Entity")
                    .entityType(EntityType.CLAUSE)
                    .build();
            entityRepository.save(entity);

            List<Entity> conflicting = entityRepository.findEntitiesWithConflicts();

            assertThat(conflicting).isEmpty();
        }
    }
}
