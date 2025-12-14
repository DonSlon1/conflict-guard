package com.conflictguard.graphql;

import com.conflictguard.ai.OpenRouterClient;
import com.conflictguard.domain.Document;
import com.conflictguard.domain.DocumentType;
import com.conflictguard.graphql.exception.GraphQLRateLimitException;
import com.conflictguard.graphql.exception.GraphQLServiceUnavailableException;
import com.conflictguard.graphql.exception.GraphQLValidationException;
import com.conflictguard.service.ConflictService;
import com.conflictguard.service.DocumentService;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * GraphQL Mutation controller for write operations.
 *
 * <p><b>Rate Limiting:</b> All mutations are protected by Resilience4j rate limiters:
 * <ul>
 *   <li>{@code ingestDocument}: 10 requests/minute (AI extraction is expensive)</li>
 *   <li>{@code analyzeConflicts}: 5 requests/minute (reasoning is very expensive)</li>
 *   <li>{@code deleteDocument}: 30 requests/minute (cheap operation)</li>
 * </ul>
 *
 * <p><b>Circuit Breaker:</b> AI operations are protected by circuit breaker.
 * When OpenRouter API fails repeatedly, the circuit opens and returns a
 * SERVICE_UNAVAILABLE error instead of overwhelming the failing service.
 *
 * @see com.conflictguard.ai.OpenRouterClient
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class MutationController {

    private static final int MAX_DOCUMENT_NAME_LENGTH = 255;
    private static final int MAX_DOCUMENT_CONTENT_LENGTH = 100_000; // ~100KB of text
    private static final int MAX_DOCUMENTS_FOR_ANALYSIS = 10;
    private static final int RATE_LIMIT_RETRY_SECONDS = 60;

    private final DocumentService documentService;
    private final ConflictService conflictService;

    /**
     * Ingests a document, extracts entities using AI, and stores in graph database.
     * Rate limited to 10 requests/minute to prevent AI API cost explosions.
     */
    @MutationMapping
    @RateLimiter(name = "documentIngestion", fallbackMethod = "ingestDocumentFallback")
    public Document ingestDocument(@Argument DocumentInput input) {
        validateDocumentInput(input);
        log.info("Mutation: ingestDocument(name={}, type={})", input.name(), input.documentType());
        try {
            return documentService.ingestDocument(
                    input.name(),
                    input.content(),
                    input.documentType());
        } catch (OpenRouterClient.CircuitBreakerOpenException e) {
            throw new GraphQLServiceUnavailableException(
                    "AI extraction service temporarily unavailable. Please try again later.",
                    "openrouter", 30);
        }
    }

    @SuppressWarnings("unused") // Called by Resilience4j via reflection
    private Document ingestDocumentFallback(DocumentInput input, RequestNotPermitted e) {
        log.warn("Rate limit exceeded for ingestDocument: {}", input.name());
        throw new GraphQLRateLimitException(
                "Rate limit exceeded for document ingestion. Maximum 10 documents per minute.",
                "ingestDocument", RATE_LIMIT_RETRY_SECONDS);
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

    /**
     * Deletes a document and its associated entities.
     * Rate limited to 30 requests/minute (cheap operation).
     */
    @MutationMapping
    @RateLimiter(name = "documentDeletion", fallbackMethod = "deleteDocumentFallback")
    public boolean deleteDocument(@Argument String id) {
        log.info("Mutation: deleteDocument(id={})", id);
        return documentService.deleteDocument(id);
    }


    @SuppressWarnings("unused")
    private boolean deleteDocumentFallback(String id, RequestNotPermitted e) {
        log.warn("Rate limit exceeded for deleteDocument: {}", id);
        throw new GraphQLRateLimitException(
                "Rate limit exceeded for document deletion. Maximum 30 deletions per minute.",
                "deleteDocument", RATE_LIMIT_RETRY_SECONDS);
    }

    @MutationMapping
    @RateLimiter(name = "conflictDeletion", fallbackMethod = "deleteConflictFallback")
    public boolean deleteConflict(@Argument String id) {
        log.info("Mutation: deleteConflict(id={})", id);
        return conflictService.deleteConflict(id);
    }
    @SuppressWarnings("unused")
    private boolean deleteConflictFallback(String id, RequestNotPermitted e) {
        log.warn("Rate limit exceeded for deleteConflict: {}", id);
        throw new GraphQLRateLimitException(
                "Rate limit exceeded for conflict deletion. Maximum 30 deletions per minute.",
                "deleteConflict", RATE_LIMIT_RETRY_SECONDS);
    }
    /**
     * Analyzes documents for conflicts using AI reasoning.
     * Rate limited to 5 requests/minute (most expensive operation).
     */
    @MutationMapping
    @RateLimiter(name = "conflictAnalysis", fallbackMethod = "analyzeConflictsFallback")
    public QueryController.ConflictAnalysisResultDto analyzeConflicts(@Argument List<String> documentIds) {
        validateDocumentIds(documentIds);
        log.info("Mutation: analyzeConflicts(documentIds={})", documentIds);
        try {
            ConflictService.ConflictAnalysisResult result = conflictService.analyzeConflicts(documentIds);
            return new QueryController.ConflictAnalysisResultDto(
                    result.conflicts(),
                    result.summary(),
                    result.analyzedAt().toString());
        } catch (OpenRouterClient.CircuitBreakerOpenException e) {
            throw new GraphQLServiceUnavailableException(
                    "AI reasoning service temporarily unavailable. Please try again later.",
                    "openrouter", 30);
        }
    }

    @SuppressWarnings("unused")
    private QueryController.ConflictAnalysisResultDto analyzeConflictsFallback(
            List<String> documentIds, RequestNotPermitted e) {
        log.warn("Rate limit exceeded for analyzeConflicts: {} documents", documentIds.size());
        throw new GraphQLRateLimitException(
                "Rate limit exceeded for conflict analysis. Maximum 5 analyses per minute.",
                "analyzeConflicts", RATE_LIMIT_RETRY_SECONDS);
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
