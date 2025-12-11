package com.conflictguard.service;

import com.conflictguard.domain.Entity;
import com.conflictguard.domain.EntityType;
import com.conflictguard.repository.EntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntityService")
class EntityServiceTest {

    @Mock
    private EntityRepository entityRepository;

    private EntityService entityService;

    @BeforeEach
    void setUp() {
        entityService = new EntityService(entityRepository);
    }

    @Nested
    @DisplayName("getEntities")
    class GetEntitiesTests {

        @Test
        @DisplayName("should return all entities when type is null")
        void shouldReturnAllEntitiesWhenTypeIsNull() {
            Entity e1 = Entity.builder().id("1").name("Entity 1").entityType(EntityType.TIME_PERIOD).build();
            Entity e2 = Entity.builder().id("2").name("Entity 2").entityType(EntityType.MONETARY_VALUE).build();
            when(entityRepository.findAll()).thenReturn(List.of(e1, e2));

            List<Entity> result = entityService.getEntities(null);

            assertThat(result).hasSize(2);
            verify(entityRepository).findAll();
            verify(entityRepository, never()).findByEntityType(any());
        }

        @Test
        @DisplayName("should filter by type when type is provided")
        void shouldFilterByTypeWhenProvided() {
            Entity e1 = Entity.builder().id("1").name("Payment Term").entityType(EntityType.TIME_PERIOD).build();
            when(entityRepository.findByEntityType(EntityType.TIME_PERIOD)).thenReturn(List.of(e1));

            List<Entity> result = entityService.getEntities(EntityType.TIME_PERIOD);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEntityType()).isEqualTo(EntityType.TIME_PERIOD);
            verify(entityRepository).findByEntityType(EntityType.TIME_PERIOD);
            verify(entityRepository, never()).findAll();
        }

        @Test
        @DisplayName("should return empty list when no entities match type")
        void shouldReturnEmptyListWhenNoMatch() {
            when(entityRepository.findByEntityType(EntityType.PENALTY)).thenReturn(List.of());

            List<Entity> result = entityService.getEntities(EntityType.PENALTY);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getEntityById")
    class GetEntityByIdTests {

        @Test
        @DisplayName("should return entity when found")
        void shouldReturnEntityWhenFound() {
            Entity entity = Entity.builder()
                    .id("123")
                    .name("Test Entity")
                    .entityType(EntityType.OBLIGATION)
                    .value("Must pay on time")
                    .build();
            when(entityRepository.findById("123")).thenReturn(Optional.of(entity));

            Optional<Entity> result = entityService.getEntityById("123");

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Test Entity");
            assertThat(result.get().getValue()).isEqualTo("Must pay on time");
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            when(entityRepository.findById("nonexistent")).thenReturn(Optional.empty());

            Optional<Entity> result = entityService.getEntityById("nonexistent");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getEntitiesForDocument")
    class GetEntitiesForDocumentTests {

        @Test
        @DisplayName("should return entities for document")
        void shouldReturnEntitiesForDocument() {
            Entity e1 = Entity.builder().id("1").name("Entity 1").build();
            Entity e2 = Entity.builder().id("2").name("Entity 2").build();
            when(entityRepository.findByDocumentId("doc-123")).thenReturn(List.of(e1, e2));

            List<Entity> result = entityService.getEntitiesForDocument("doc-123");

            assertThat(result).hasSize(2);
            verify(entityRepository).findByDocumentId("doc-123");
        }

        @Test
        @DisplayName("should return empty list when document has no entities")
        void shouldReturnEmptyWhenNoEntities() {
            when(entityRepository.findByDocumentId("empty-doc")).thenReturn(List.of());

            List<Entity> result = entityService.getEntitiesForDocument("empty-doc");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getEntitiesWithConflicts")
    class GetEntitiesWithConflictsTests {

        @Test
        @DisplayName("should return entities that have conflicts")
        void shouldReturnEntitiesWithConflicts() {
            Entity e1 = Entity.builder().id("1").name("Conflicting Entity").build();
            when(entityRepository.findEntitiesWithConflicts()).thenReturn(List.of(e1));

            List<Entity> result = entityService.getEntitiesWithConflicts();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Conflicting Entity");
        }

        @Test
        @DisplayName("should return empty when no conflicts")
        void shouldReturnEmptyWhenNoConflicts() {
            when(entityRepository.findEntitiesWithConflicts()).thenReturn(List.of());

            List<Entity> result = entityService.getEntitiesWithConflicts();

            assertThat(result).isEmpty();
        }
    }
}
