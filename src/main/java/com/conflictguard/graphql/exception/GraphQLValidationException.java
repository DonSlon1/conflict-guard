package com.conflictguard.graphql.exception;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.language.SourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Custom GraphQL exception for validation errors.
 * Provides proper GraphQL error format with extensions.
 */
public class GraphQLValidationException extends RuntimeException implements GraphQLError {

    private final String field;

    public GraphQLValidationException(String message, String field) {
        super(message);
        this.field = field;
    }

    @Override
    public List<SourceLocation> getLocations() {
        return null;
    }

    @Override
    public ErrorClassification getErrorType() {
        return ErrorClassification.errorClassification("VALIDATION_ERROR");
    }

    @Override
    public Map<String, Object> getExtensions() {
        return Map.of(
                "field", field,
                "code", "VALIDATION_ERROR"
        );
    }
}
