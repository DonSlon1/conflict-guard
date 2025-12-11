package com.conflictguard.graphql.exception;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.language.SourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Custom GraphQL exception for not found errors.
 * Provides proper GraphQL error format with extensions.
 */
public class GraphQLNotFoundException extends RuntimeException implements GraphQLError {

    private final String resourceType;
    private final String resourceId;

    public GraphQLNotFoundException(String resourceType, String resourceId) {
        super(resourceType + " with ID '" + resourceId + "' not found");
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    @Override
    public List<SourceLocation> getLocations() {
        return null;
    }

    @Override
    public ErrorClassification getErrorType() {
        return ErrorClassification.errorClassification("NOT_FOUND");
    }

    @Override
    public Map<String, Object> getExtensions() {
        return Map.of(
                "resourceType", resourceType,
                "resourceId", resourceId,
                "code", "NOT_FOUND"
        );
    }
}
