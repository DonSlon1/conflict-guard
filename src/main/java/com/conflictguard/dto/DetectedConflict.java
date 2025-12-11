package com.conflictguard.dto;

import com.conflictguard.domain.ConflictSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for AI-detected conflicts.
 * This structure is used to parse the reasoning output from the LLM.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectedConflict {
    private String description;
    private ConflictSeverity severity;
    private String reasoning;
    private String legalPrinciple;
    private List<String> involvedEntityNames;
}
