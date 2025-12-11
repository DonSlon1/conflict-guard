package com.conflictguard.graphql;

import com.conflictguard.domain.Conflict;
import com.conflictguard.domain.Document;
import com.conflictguard.domain.Entity;
import com.conflictguard.repository.ConflictRepository;
import com.conflictguard.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GraphQL resolvers for nested field queries.
 * These enable traversing the graph through GraphQL queries.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class EntityResolver {

    private final DocumentRepository documentRepository;
    private final ConflictRepository conflictRepository;

    /**
     * Resolves relatedEntities for Entity type.
     * Maps domain EntityRelationship to GraphQL EntityRelation.
     */
    @SchemaMapping(typeName = "Entity", field = "relatedEntities")
    public List<EntityRelationDto> relatedEntities(Entity entity) {
        if (entity.getRelatedEntities() == null) {
            return List.of();
        }
        return entity.getRelatedEntities().stream()
                .map(rel -> new EntityRelationDto(rel.getTargetEntity(), rel.getRelationshipType().name()))
                .collect(Collectors.toList());
    }

    /**
     * Resolves conflicts for Entity type.
     */
    @SchemaMapping(typeName = "Entity", field = "conflicts")
    public List<Conflict> entityConflicts(Entity entity) {
        return conflictRepository.findByEntityId(entity.getId());
    }

    /**
     * Resolves sourceDocument for Entity type.
     * Traverses the CONTAINS relationship backwards to find the parent document.
     */
    @SchemaMapping(typeName = "Entity", field = "sourceDocument")
    public Document sourceDocument(Entity entity) {
        if (entity.getId() == null) {
            return null;
        }
        return documentRepository.findByEntityId(entity.getId()).orElse(null);
    }

    /**
     * Resolves createdAt for Document type (converts LocalDateTime to String).
     */
    @SchemaMapping(typeName = "Document", field = "createdAt")
    public String documentCreatedAt(Document document) {
        return document.getCreatedAt() != null ? document.getCreatedAt().toString() : null;
    }

    /**
     * DTO for GraphQL EntityRelation type.
     */
    public record EntityRelationDto(
            Entity entity,
            String relationshipType) {
    }
}
