package com.conflictguard.graphql;

import com.conflictguard.domain.Document;
import com.conflictguard.domain.DocumentType;
import com.conflictguard.graphql.exception.GraphQLValidationException;
import com.conflictguard.service.ConflictService;
import com.conflictguard.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * GraphQL Mutation controller for write operations.
 *
 * <p><b>PRODUCTION TODO - Rate Limiting:</b>
 * <pre>
 * In production, implement rate limiting to prevent:
 * 1. AI API cost explosions from runaway requests
 * 2. DoS attacks on expensive operations
 *
 * Recommended implementation options:
 *
 * Option A: Bucket4j with Spring Boot Starter
 * {@code
 * @RateLimiter(name = "aiOperations", fallbackMethod = "rateLimitFallback")
 * public Document ingestDocument(...) { ... }
 * }
 *
 * Option B: Custom interceptor with Redis (for distributed systems)
 * {@code
 * @Bean
 * public WebGraphQlInterceptor rateLimitInterceptor(RedisTemplate<String, String> redis) {
 *     return (request, chain) -> {
 *         String clientId = extractClientId(request);
 *         if (!rateLimiter.tryAcquire(clientId)) {
 *             throw new RateLimitExceededException("Too many requests");
 *         }
 *         return chain.next(request);
 *     };
 * }
 * }
 *
 * Suggested limits:
 * - ingestDocument: 10 requests/minute per client (AI extraction is expensive)
 * - analyzeConflicts: 5 requests/minute per client (reasoning is very expensive)
 * - deleteDocument: 30 requests/minute per client (cheap operation)
 * </pre>
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class MutationController {

    private static final int MAX_DOCUMENT_NAME_LENGTH = 255;
    private static final int MAX_DOCUMENT_CONTENT_LENGTH = 100_000; // ~100KB of text
    private static final int MAX_DOCUMENTS_FOR_ANALYSIS = 10;

    private final DocumentService documentService;
    private final ConflictService conflictService;

    // TODO: [PRODUCTION] Add @RateLimiter annotation here - see class Javadoc for implementation
    @MutationMapping
    public Document ingestDocument(@Argument DocumentInput input) {
        validateDocumentInput(input);
        log.info("Mutation: ingestDocument(name={}, type={})", input.name(), input.documentType());
        return documentService.ingestDocument(
                input.name(),
                input.content(),
                input.documentType());
    }

    private void validateDocumentInput(DocumentInput input) {
        if (input.name() == null || input.name().isBlank()) {
            throw new GraphQLValidationException("Document name cannot be empty", "input.name");
        }
        if (input.name().length() > MAX_DOCUMENT_NAME_LENGTH) {
            throw new GraphQLValidationException(
                    "Document name exceeds maximum length of " + MAX_DOCUMENT_NAME_LENGTH + " characters",
                    "input.name");
        }
        if (input.content() == null || input.content().isBlank()) {
            throw new GraphQLValidationException("Document content cannot be empty", "input.content");
        }
        if (input.content().length() > MAX_DOCUMENT_CONTENT_LENGTH) {
            throw new GraphQLValidationException(
                    "Document content exceeds maximum length of " + MAX_DOCUMENT_CONTENT_LENGTH + " characters",
                    "input.content");
        }
        if (input.documentType() == null) {
            throw new GraphQLValidationException("Document type is required", "input.documentType");
        }
    }

    @MutationMapping
    public boolean deleteDocument(@Argument String id) {
        log.info("Mutation: deleteDocument(id={})", id);
        return documentService.deleteDocument(id);
    }

    // TODO: [PRODUCTION] Add @RateLimiter annotation here - most expensive operation
    @MutationMapping
    public QueryController.ConflictAnalysisResultDto analyzeConflicts(@Argument List<String> documentIds) {
        validateDocumentIds(documentIds);
        log.info("Mutation: analyzeConflicts(documentIds={})", documentIds);
        ConflictService.ConflictAnalysisResult result = conflictService.analyzeConflicts(documentIds);
        return new QueryController.ConflictAnalysisResultDto(
                result.conflicts(),
                result.summary(),
                result.analyzedAt().toString());
    }

    private void validateDocumentIds(List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            throw new GraphQLValidationException("At least one document ID is required", "documentIds");
        }
        if (documentIds.size() > MAX_DOCUMENTS_FOR_ANALYSIS) {
            throw new GraphQLValidationException(
                    "Cannot analyze more than " + MAX_DOCUMENTS_FOR_ANALYSIS + " documents at once",
                    "documentIds");
        }
    }

    /**
     * Input record for document ingestion.
     */
    public record DocumentInput(
            String name,
            String content,
            DocumentType documentType) {
    }
}
