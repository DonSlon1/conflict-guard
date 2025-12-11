package com.conflictguard.service;

import com.conflictguard.domain.Entity;
import com.conflictguard.domain.EntityType;
import com.conflictguard.repository.EntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for entity operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EntityService {

    private final EntityRepository entityRepository;

    /**
     * Gets all entities, optionally filtered by type.
     */
    public List<Entity> getEntities(EntityType type) {
        if (type != null) {
            return entityRepository.findByEntityType(type);
        }
        return entityRepository.findAll();
    }

    /**
     * Gets an entity by ID.
     */
    public Optional<Entity> getEntityById(String id) {
        return entityRepository.findById(id);
    }

    /**
     * Gets entities for a specific document.
     */
    public List<Entity> getEntitiesForDocument(String documentId) {
        return entityRepository.findByDocumentId(documentId);
    }

    /**
     * Gets entities involved in conflicts.
     */
    public List<Entity> getEntitiesWithConflicts() {
        return entityRepository.findEntitiesWithConflicts();
    }
}
