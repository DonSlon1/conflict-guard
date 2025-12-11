package com.conflictguard.graphql;

import com.conflictguard.domain.DocumentType;
import com.conflictguard.domain.EntityType;
import com.conflictguard.domain.ConflictSeverity;
import com.conflictguard.domain.RelationshipType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for domain enums used in GraphQL schema.
 * Ensures enum values match GraphQL schema definitions.
 */
class GraphQLSchemaTest {

    @Test
    @DisplayName("DocumentType enum should have all expected values")
    void documentTypeShouldHaveExpectedValues() {
        DocumentType[] values = DocumentType.values();

        assertEquals(5, values.length);
        assertNotNull(DocumentType.valueOf("CONTRACT"));
        assertNotNull(DocumentType.valueOf("TERMS_AND_CONDITIONS"));
        assertNotNull(DocumentType.valueOf("INTERNAL_DIRECTIVE"));
        assertNotNull(DocumentType.valueOf("REGULATION"));
        assertNotNull(DocumentType.valueOf("OTHER"));
    }

    @Test
    @DisplayName("EntityType enum should have all expected values")
    void entityTypeShouldHaveExpectedValues() {
        EntityType[] values = EntityType.values();

        assertEquals(8, values.length);
        assertNotNull(EntityType.valueOf("TIME_PERIOD"));
        assertNotNull(EntityType.valueOf("MONETARY_VALUE"));
        assertNotNull(EntityType.valueOf("PARTY"));
        assertNotNull(EntityType.valueOf("OBLIGATION"));
        assertNotNull(EntityType.valueOf("RIGHT"));
        assertNotNull(EntityType.valueOf("CONDITION"));
        assertNotNull(EntityType.valueOf("PENALTY"));
        assertNotNull(EntityType.valueOf("CLAUSE"));
    }

    @Test
    @DisplayName("ConflictSeverity enum should have all expected values")
    void conflictSeverityShouldHaveExpectedValues() {
        ConflictSeverity[] values = ConflictSeverity.values();

        assertEquals(4, values.length);
        assertNotNull(ConflictSeverity.valueOf("LOW"));
        assertNotNull(ConflictSeverity.valueOf("MEDIUM"));
        assertNotNull(ConflictSeverity.valueOf("HIGH"));
        assertNotNull(ConflictSeverity.valueOf("CRITICAL"));
    }

    @Test
    @DisplayName("RelationshipType enum should have all expected values")
    void relationshipTypeShouldHaveExpectedValues() {
        RelationshipType[] values = RelationshipType.values();

        assertEquals(5, values.length);
        assertNotNull(RelationshipType.valueOf("DEFINES"));
        assertNotNull(RelationshipType.valueOf("REFERENCES"));
        assertNotNull(RelationshipType.valueOf("OVERRIDES"));
        assertNotNull(RelationshipType.valueOf("CONFLICTS_WITH"));
        assertNotNull(RelationshipType.valueOf("DEPENDS_ON"));
    }

    @Test
    @DisplayName("Severity levels should be ordered correctly")
    void severityLevelsShouldBeOrdered() {
        // Ordinal values should represent increasing severity
        assertTrue(ConflictSeverity.LOW.ordinal() < ConflictSeverity.MEDIUM.ordinal());
        assertTrue(ConflictSeverity.MEDIUM.ordinal() < ConflictSeverity.HIGH.ordinal());
        assertTrue(ConflictSeverity.HIGH.ordinal() < ConflictSeverity.CRITICAL.ordinal());
    }
}
