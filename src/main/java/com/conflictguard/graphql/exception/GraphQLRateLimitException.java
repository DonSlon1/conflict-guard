package com.conflictguard.graphql.exception;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.language.SourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Custom GraphQL exception for rate limit violations.
 * Returns HTTP 429-equivalent error with retry-after hint.
 *
 * <p>Example GraphQL response:
 * <pre>
 * {
 *   "errors": [{
 *     "message": "Rate limit exceeded for conflict analysis",
 *     "extensions": {
 *       "code": "RATE_LIMITED",
 *       "operation": "analyzeConflicts",
 *       "retryAfterSeconds": 60
 *     }
 *   }]
 * }
 * </pre>
 */
public class GraphQLRateLimitException extends RuntimeException implements GraphQLError {

    private final String operation;
    private final int retryAfterSeconds;

    public GraphQLRateLimitException(String message, String operation, int retryAfterSeconds) {
        super(message);
        this.operation = operation;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public GraphQLRateLimitException(String operation, int retryAfterSeconds) {
        this("Rate limit exceeded. Please try again later.", operation, retryAfterSeconds);
    }

    @Override
    public List<SourceLocation> getLocations() {
        return null;
    }

    @Override
    public ErrorClassification getErrorType() {
        return ErrorClassification.errorClassification("RATE_LIMITED");
    }

    @Override
    public Map<String, Object> getExtensions() {
        return Map.of(
                "code", "RATE_LIMITED",
                "operation", operation,
                "retryAfterSeconds", retryAfterSeconds
        );
    }
}
