package com.conflictguard.service;

import com.conflictguard.ai.EntityExtractionService;
import com.conflictguard.domain.Document;
import com.conflictguard.domain.DocumentType;
import com.conflictguard.domain.Entity;
import com.conflictguard.dto.ExtractionResult;
import com.conflictguard.dto.ExtractedEntity;
import com.conflictguard.repository.DocumentRepository;
import com.conflictguard.repository.EntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for document ingestion and entity extraction.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final EntityRepository entityRepository;
    private final EntityExtractionService extractionService;

    /**
     * Ingests a document, extracts entities using AI, and stores in graph database.
     */
    @Transactional
    public Document ingestDocument(String name, String content, DocumentType documentType) {
        log.info("Ingesting document: {} (type: {})", name, documentType);

        // Create document
        Document document = Document.builder()
                .name(name)
                .content(content)
                .documentType(documentType)
                .createdAt(LocalDateTime.now())
                .entities(new ArrayList<>())
                .build();

        // Extract entities using AI
        ExtractionResult extractionResult = extractionService.extractEntities(name, documentType, content);
        log.info("Extracted {} entities from document", extractionResult.getEntities().size());

        // Convert extracted entities to domain entities
        Map<String, Entity> entityMap = new HashMap<>();

        for (ExtractedEntity extracted : extractionResult.getEntities()) {
            Entity entity = Entity.builder()
                    .name(extracted.getName())
                    .entityType(extracted.getEntityType())
                    .value(extracted.getValue())
                    .sourceContext(extracted.getSourceContext())
                    .relatedEntities(new ArrayList<>())
                    .build();

            entityMap.put(extracted.getName(), entity);
            document.addEntity(entity);
        }

        // Set up relationships between entities
        for (ExtractedEntity extracted : extractionResult.getEntities()) {
            if (extracted.getRelationships() != null) {
                Entity sourceEntity = entityMap.get(extracted.getName());
                for (ExtractedEntity.ExtractedRelation relation : extracted.getRelationships()) {
                    Entity targetEntity = entityMap.get(relation.getTargetEntityName());
                    if (targetEntity != null && sourceEntity != null) {
                        sourceEntity.addRelationship(targetEntity, relation.getRelationshipType());
                    }
                }
            }
        }

        // Save document with all entities
        Document savedDocument = documentRepository.save(document);
        log.info("Saved document with ID: {}", savedDocument.getId());

        return savedDocument;
    }

    /**
     * Retrieves all documents with their entities.
     */
    public List<Document> getAllDocuments() {
        return documentRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Retrieves a document by ID.
     */
    public Optional<Document> getDocumentById(String id) {
        return documentRepository.findById(id);
    }

    /**
     * Retrieves documents by their IDs.
     */
    public List<Document> getDocumentsByIds(List<String> ids) {
        return documentRepository.findAllById(ids);
    }

    /**
     * Deletes a document and its associated entities.
     */
    @Transactional
    public boolean deleteDocument(String id) {
        if (documentRepository.existsById(id)) {
            documentRepository.deleteById(id);
            log.info("Deleted document: {}", id);
            return true;
        }
        return false;
    }
}
