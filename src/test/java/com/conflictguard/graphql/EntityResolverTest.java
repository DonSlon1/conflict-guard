package com.conflictguard.graphql;

import com.conflictguard.domain.*;
import com.conflictguard.repository.ConflictRepository;
import com.conflictguard.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntityResolver")
class EntityResolverTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ConflictRepository conflictRepository;

    private EntityResolver entityResolver;

    @BeforeEach
    void setUp() {
        entityResolver = new EntityResolver(documentRepository, conflictRepository);
    }

    @Nested
    @DisplayName("relatedEntities resolver")
    class RelatedEntitiesTests {

        @Test
        @DisplayName("should return empty list when entity has no relationships")
        void shouldReturnEmptyListWhenNoRelationships() {
            Entity entity = Entity.builder()
                    .id("e1")
                    .name("Test Entity")
                    .relatedEntities(null)
                    .build();

            List<EntityResolver.EntityRelationDto> result = entityResolver.relatedEntities(entity);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when relationships list is empty")
        void shouldReturnEmptyListWhenEmpty() {
            Entity entity = Entity.builder()
                    .id("e1")
                    .name("Test Entity")
                    .relatedEntities(new ArrayList<>())
                    .build();

            List<EntityResolver.EntityRelationDto> result = entityResolver.relatedEntities(entity);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should map entity relationships to DTOs")
        void shouldMapRelationshipsToDtos() {
            Entity targetEntity = Entity.builder()
                    .id("e2")
                    .name("Target Entity")
                    .entityType(EntityType.CLAUSE)
                    .build();

            EntityRelationship relationship = EntityRelationship.builder()
                    .targetEntity(targetEntity)
                    .relationshipType(RelationshipType.REFERENCES)
                    .build();

            Entity entity = Entity.builder()
                    .id("e1")
                    .name("Source Entity")
                    .relatedEntities(List.of(relationship))
                    .build();

            List<EntityResolver.EntityRelationDto> result = entityResolver.relatedEntities(entity);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).entity().getName()).isEqualTo("Target Entity");
            assertThat(result.get(0).relationshipType()).isEqualTo("REFERENCES");
        }

        @Test
        @DisplayName("should handle multiple relationships")
        void shouldHandleMultipleRelationships() {
            Entity target1 = Entity.builder().id("t1").name("Target 1").build();
            Entity target2 = Entity.builder().id("t2").name("Target 2").build();

            EntityRelationship rel1 = EntityRelationship.builder()
                    .targetEntity(target1)
                    .relationshipType(RelationshipType.DEFINES)
                    .build();
            EntityRelationship rel2 = EntityRelationship.builder()
                    .targetEntity(target2)
                    .relationshipType(RelationshipType.OVERRIDES)
                    .build();

            Entity entity = Entity.builder()
                    .id("e1")
                    .relatedEntities(List.of(rel1, rel2))
                    .build();

            List<EntityResolver.EntityRelationDto> result = entityResolver.relatedEntities(entity);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("entityConflicts resolver")
    class EntityConflictsTests {

        @Test
        @DisplayName("should return conflicts for entity")
        void shouldReturnConflictsForEntity() {
            Entity entity = Entity.builder().id("e1").name("Payment Term").build();
            Conflict conflict = Conflict.builder()
                    .id("c1")
                    .description("Payment conflict")
                    .severity(ConflictSeverity.HIGH)
                    .build();

            when(conflictRepository.findByEntityId("e1")).thenReturn(List.of(conflict));

            List<Conflict> result = entityResolver.entityConflicts(entity);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDescription()).isEqualTo("Payment conflict");
            verify(conflictRepository).findByEntityId("e1");
        }

        @Test
        @DisplayName("should return empty list when no conflicts")
        void shouldReturnEmptyWhenNoConflicts() {
            Entity entity = Entity.builder().id("e1").build();
            when(conflictRepository.findByEntityId("e1")).thenReturn(List.of());

            List<Conflict> result = entityResolver.entityConflicts(entity);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("sourceDocument resolver")
    class SourceDocumentTests {

        @Test
        @DisplayName("should return source document for entity")
        void shouldReturnSourceDocument() {
            Entity entity = Entity.builder().id("e1").name("Test Entity").build();
            Document document = Document.builder()
                    .id("doc1")
                    .name("Test Document")
                    .documentType(DocumentType.CONTRACT)
                    .build();

            when(documentRepository.findByEntityId("e1")).thenReturn(Optional.of(document));

            Document result = entityResolver.sourceDocument(entity);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Test Document");
            verify(documentRepository).findByEntityId("e1");
        }

        @Test
        @DisplayName("should return null when entity has null ID")
        void shouldReturnNullWhenEntityIdIsNull() {
            Entity entity = Entity.builder().id(null).name("Test").build();

            Document result = entityResolver.sourceDocument(entity);

            assertThat(result).isNull();
            verify(documentRepository, never()).findByEntityId(any());
        }

        @Test
        @DisplayName("should return null when document not found")
        void shouldReturnNullWhenDocumentNotFound() {
            Entity entity = Entity.builder().id("orphan").build();
            when(documentRepository.findByEntityId("orphan")).thenReturn(Optional.empty());

            Document result = entityResolver.sourceDocument(entity);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("documentCreatedAt resolver")
    class DocumentCreatedAtTests {

        @Test
        @DisplayName("should format LocalDateTime as string")
        void shouldFormatDateTime() {
            Document document = Document.builder()
                    .id("doc1")
                    .createdAt(LocalDateTime.of(2024, 6, 15, 14, 30, 0))
                    .build();

            String result = entityResolver.documentCreatedAt(document);

            assertThat(result).isEqualTo("2024-06-15T14:30");
        }

        @Test
        @DisplayName("should return null when createdAt is null")
        void shouldReturnNullWhenDateIsNull() {
            Document document = Document.builder()
                    .id("doc1")
                    .createdAt(null)
                    .build();

            String result = entityResolver.documentCreatedAt(document);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("EntityRelationDto record")
    class EntityRelationDtoTests {

        @Test
        @DisplayName("should create DTO with entity and relationship type")
        void shouldCreateDto() {
            Entity entity = Entity.builder().id("e1").name("Test").build();
            EntityResolver.EntityRelationDto dto = new EntityResolver.EntityRelationDto(entity, "DEFINES");

            assertThat(dto.entity()).isEqualTo(entity);
            assertThat(dto.relationshipType()).isEqualTo("DEFINES");
        }
    }
}
