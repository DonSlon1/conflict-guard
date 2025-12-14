package com.conflictguard.graphql.exception;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.language.SourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Custom GraphQL exception for service unavailability (circuit breaker open).
 * Indicates the AI service is temporarily unavailable due to repeated failures.
 *
 * <p>Example GraphQL response:
 * <pre>
 * {
 *   "errors": [{
 *     "message": "AI service temporarily unavailable",
 *     "extensions": {
 *       "code": "SERVICE_UNAVAILABLE",
 *       "service": "openrouter",
 *       "retryAfterSeconds": 30
 *     }
 *   }]
 * }
 * </pre>
 */
public class GraphQLServiceUnavailableException extends RuntimeException implements GraphQLError {

    private final String service;
    private final int retryAfterSeconds;

    public GraphQLServiceUnavailableException(String message, String service, int retryAfterSeconds) {
        super(message);
        this.service = service;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public GraphQLServiceUnavailableException(String service) {
        this("Service temporarily unavailable. Please try again later.", service, 30);
    }

    @Override
    public List<SourceLocation> getLocations() {
        return null;
    }

    @Override
    public ErrorClassification getErrorType() {
        return ErrorClassification.errorClassification("SERVICE_UNAVAILABLE");
    }

    @Override
    public Map<String, Object> getExtensions() {
        return Map.of(
                "code", "SERVICE_UNAVAILABLE",
                "service", service,
                "retryAfterSeconds", retryAfterSeconds
        );
    }
}
