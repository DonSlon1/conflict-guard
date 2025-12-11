package com.conflictguard.dto;

import com.conflictguard.domain.EntityType;
import com.conflictguard.domain.RelationshipType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for AI-extracted entity information.
 * This structure is used to parse the JSON output from the LLM.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedEntity {
    private String name;
    private EntityType entityType;
    private String value;
    private String sourceContext;
    private List<ExtractedRelation> relationships;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtractedRelation {
        private String targetEntityName;
        private RelationshipType relationshipType;
    }
}
