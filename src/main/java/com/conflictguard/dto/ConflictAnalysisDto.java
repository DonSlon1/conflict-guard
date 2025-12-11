package com.conflictguard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper for AI conflict analysis result.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictAnalysisDto {
    private List<DetectedConflict> conflicts;
    private String overallSummary;
}
