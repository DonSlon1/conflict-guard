package com.conflictguard.graphql;

import com.conflictguard.domain.Conflict;
import com.conflictguard.domain.ConflictSeverity;
import com.conflictguard.domain.Document;
import com.conflictguard.domain.Entity;
import com.conflictguard.domain.EntityType;
import com.conflictguard.service.ConflictService;
import com.conflictguard.service.DocumentService;
import com.conflictguard.service.EntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * GraphQL Query controller for read operations.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class QueryController {

    private final DocumentService documentService;
    private final EntityService entityService;
    private final ConflictService conflictService;

    @QueryMapping
    public List<Document> documents() {
        log.debug("Query: documents");
        return documentService.getAllDocuments();
    }

    @QueryMapping
    public Document document(@Argument String id) {
        log.debug("Query: document(id={})", id);
        return documentService.getDocumentById(id).orElse(null);
    }

    @QueryMapping
    public List<Entity> entities(@Argument EntityType type) {
        log.debug("Query: entities(type={})", type);
        return entityService.getEntities(type);
    }

    @QueryMapping
    public Entity entity(@Argument String id) {
        log.debug("Query: entity(id={})", id);
        return entityService.getEntityById(id).orElse(null);
    }

    @QueryMapping
    public List<Conflict> conflicts(@Argument ConflictSeverity severity) {
        log.debug("Query: conflicts(severity={})", severity);
        return conflictService.getConflicts(severity);
    }

    @QueryMapping
    public List<Conflict> conflictsForDocuments(@Argument List<String> documentIds) {
        log.debug("Query: conflictsForDocuments(documentIds={})", documentIds);
        return conflictService.getConflictsForDocuments(documentIds);
    }

    /**
     * DTO for GraphQL response.
     */
    public record ConflictAnalysisResultDto(
            List<Conflict> conflicts,
            String summary,
            String analyzedAt) {
    }
}
