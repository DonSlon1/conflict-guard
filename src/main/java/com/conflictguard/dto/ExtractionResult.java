package com.conflictguard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper for AI extraction result containing multiple entities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResult {
    private List<ExtractedEntity> entities;
    private String documentSummary;
}
