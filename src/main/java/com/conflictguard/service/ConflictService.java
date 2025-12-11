package com.conflictguard.service;

import com.conflictguard.ai.ConflictReasoningService;
import com.conflictguard.domain.Conflict;
import com.conflictguard.domain.ConflictSeverity;
import com.conflictguard.domain.Document;
import com.conflictguard.domain.Entity;
import com.conflictguard.dto.ConflictAnalysisDto;
import com.conflictguard.dto.DetectedConflict;
import com.conflictguard.repository.ConflictRepository;
import com.conflictguard.repository.DocumentRepository;
import com.conflictguard.repository.EntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for conflict detection and analysis.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConflictService {

    private final ConflictRepository conflictRepository;
    private final DocumentRepository documentRepository;
    private final EntityRepository entityRepository;
    private final ConflictReasoningService reasoningService;

    /**
     * Analyzes documents for conflicts using AI reasoning.
     */
    @Transactional
    public ConflictAnalysisResult analyzeConflicts(List<String> documentIds) {
        log.info("Analyzing conflicts for documents: {}", documentIds);

        // Fetch documents with their entities
        List<Document> documents = documentRepository.findAllById(documentIds);

        if (documents.isEmpty()) {
            log.warn("No documents found for IDs: {}", documentIds);
            return new ConflictAnalysisResult(
                    List.of(),
                    "No documents found for analysis",
                    LocalDateTime.now());
        }
        // Collect all entities from the documents
        List<Entity> allEntities = new ArrayList<>();
        for (Document doc : documents) {
            if (doc.getEntities() != null) {
                allEntities.addAll(doc.getEntities());
            }
        }

        if (allEntities.isEmpty()) {
            log.warn("No entities found in documents for analysis");
            return new ConflictAnalysisResult(
                    List.of(),
                    "No entities found for conflict analysis",
                    LocalDateTime.now());
        }

        // Use AI to analyze conflicts
        ConflictAnalysisDto analysisResult = reasoningService.analyzeConflicts(allEntities);

        // Convert detected conflicts to domain objects and save
        List<Conflict> savedConflicts = new ArrayList<>();

        for (DetectedConflict detected : analysisResult.getConflicts()) {
            // Find involved entities using fuzzy matching
            List<Entity> involvedEntities = findInvolvedEntities(
                    detected.getInvolvedEntityNames(), allEntities);

            // Skip if no entities matched
            if (involvedEntities.isEmpty()) {
                log.warn("Skipping conflict with no matched entities: {}", detected.getDescription());
                continue;
            }

            // Check for duplicate conflicts (same entities, same description pattern)
            if (isDuplicateConflict(involvedEntities, detected.getDescription())) {
                log.info("Skipping duplicate conflict: {}", detected.getDescription());
                continue;
            }

            Conflict conflict = Conflict.builder()
                    .description(detected.getDescription())
                    .severity(detected.getSeverity())
                    .reasoning(detected.getReasoning())
                    .legalPrinciple(detected.getLegalPrinciple())
                    .detectedAt(LocalDateTime.now())
                    .entities(involvedEntities)
                    .build();

            savedConflicts.add(conflictRepository.save(conflict));
        }

        log.info("Detected and saved {} conflicts", savedConflicts.size());

        return new ConflictAnalysisResult(
                savedConflicts,
                analysisResult.getOverallSummary(),
                LocalDateTime.now());
    }

    /**
     * Checks if a conflict with similar entities and description already exists.
     * Prevents duplicate conflicts from being created on repeated analysis runs.
     */
    private boolean isDuplicateConflict(List<Entity> involvedEntities, String description) {
        if (involvedEntities.size() < 2) {
            return false;
        }

        List<String> entityIds = involvedEntities.stream()
                .map(Entity::getId)
                .toList();

        List<Conflict> existingConflicts = conflictRepository.findByInvolvedEntityIds(entityIds);

        for (Conflict existing : existingConflicts) {
            // Check if the conflict involves the same set of entities
            List<String> existingEntityIds = existing.getEntities().stream()
                    .map(Entity::getId)
                    .sorted()
                    .toList();

            List<String> newEntityIds = entityIds.stream()
                    .sorted()
                    .toList();

            if (existingEntityIds.equals(newEntityIds)) {
                // Same entities involved - consider it a duplicate
                log.debug("Found existing conflict involving same entities: {}", existing.getId());
                return true;
            }

            // Also check if descriptions are similar (simple substring check)
            if (existing.getDescription() != null && description != null) {
                String existingNorm = existing.getDescription().toLowerCase();
                String newNorm = description.toLowerCase();
                if (existingNorm.contains(newNorm) || newNorm.contains(existingNorm)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Finds entities matching the AI-provided names using fuzzy matching.
     * This handles cases where the AI returns slightly different names than stored.
     */
    private List<Entity> findInvolvedEntities(List<String> aiEntityNames, List<Entity> allEntities) {
        if (aiEntityNames == null || aiEntityNames.isEmpty()) {
            return new ArrayList<>();
        }

        List<Entity> matched = new ArrayList<>();
        for (String aiName : aiEntityNames) {
            Entity bestMatch = findBestMatchingEntity(aiName, allEntities);
            if (bestMatch != null && !matched.contains(bestMatch)) {
                matched.add(bestMatch);
            }
        }
        return matched;
    }

    /**
     * Finds the best matching entity for a given AI-provided name.
     * Uses case-insensitive contains matching as a simple fuzzy match.
     */
    private Entity findBestMatchingEntity(String aiName, List<Entity> entities) {
        String normalizedAiName = aiName.toLowerCase().trim();

        // First try exact match (case-insensitive)
        for (Entity entity : entities) {
            if (entity.getName().equalsIgnoreCase(aiName)) {
                return entity;
            }
        }

        // Then try contains match
        for (Entity entity : entities) {
            String normalizedEntityName = entity.getName().toLowerCase().trim();
            if (normalizedEntityName.contains(normalizedAiName) ||
                    normalizedAiName.contains(normalizedEntityName)) {
                return entity;
            }
        }

        // Try matching by value as well (e.g., "14 days" might be referenced)
        for (Entity entity : entities) {
            if (entity.getValue() != null &&
                    entity.getValue().toLowerCase().contains(normalizedAiName)) {
                return entity;
            }
        }

        log.warn("Could not find entity matching AI name: {}", aiName);
        return null;
    }

    /**
     * Gets all conflicts, optionally filtered by severity.
     */
    public List<Conflict> getConflicts(ConflictSeverity severity) {
        if (severity != null) {
            return conflictRepository.findBySeverity(severity);
        }
        return conflictRepository.findAllOrderByDetectedAtDesc();
    }

    /**
     * Gets conflicts for specific documents.
     */
    public List<Conflict> getConflictsForDocuments(List<String> documentIds) {
        return conflictRepository.findByDocumentIds(documentIds);
    }

    /**
     * Result of conflict analysis.
     */
    public record ConflictAnalysisResult(
            List<Conflict> conflicts,
            String summary,
            LocalDateTime analyzedAt) {
    }
}
