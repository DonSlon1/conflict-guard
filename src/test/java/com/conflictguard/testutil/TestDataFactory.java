package com.conflictguard.testutil;

import com.conflictguard.domain.*;
import com.conflictguard.dto.ConflictAnalysisDto;
import com.conflictguard.dto.DetectedConflict;
import com.conflictguard.dto.ExtractionResult;
import com.conflictguard.dto.ExtractedEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Factory class for creating test data objects.
 * Centralizes test data creation for consistency across tests.
 */
public final class TestDataFactory {

    private TestDataFactory() {
        // Utility class - no instantiation
    }

    // ===== Document Builders =====

    public static Document.DocumentBuilder sampleContract() {
        return Document.builder()
                .id(UUID.randomUUID().toString())
                .name("Test Contract")
                .content("This is a test contract with payment terms of 14 days.")
                .documentType(DocumentType.CONTRACT)
                .createdAt(LocalDateTime.now());
    }

    public static Document.DocumentBuilder sampleTermsAndConditions() {
        return Document.builder()
                .id(UUID.randomUUID().toString())
                .name("Terms and Conditions")
                .content("Standard terms: payment within 30 days of invoice date.")
                .documentType(DocumentType.TERMS_AND_CONDITIONS)
                .createdAt(LocalDateTime.now());
    }

    public static Document.DocumentBuilder sampleRegulation() {
        return Document.builder()
                .id(UUID.randomUUID().toString())
                .name("Industry Regulation")
                .content("Maximum penalty for late payment shall not exceed 0.5% per month.")
                .documentType(DocumentType.REGULATION)
                .createdAt(LocalDateTime.now());
    }

    // ===== Entity Builders =====

    public static Entity.EntityBuilder samplePaymentTermEntity() {
        return Entity.builder()
                .id(UUID.randomUUID().toString())
                .name("Payment Term")
                .entityType(EntityType.TIME_PERIOD)
                .value("14 days")
                .sourceContext("Payment shall be made within 14 days");
    }

    public static Entity.EntityBuilder samplePenaltyEntity() {
        return Entity.builder()
                .id(UUID.randomUUID().toString())
                .name("Late Payment Penalty")
                .entityType(EntityType.PENALTY)
                .value("0.05% per day")
                .sourceContext("Late payments incur a penalty of 0.05% per day");
    }

    public static Entity.EntityBuilder sampleMonetaryEntity() {
        return Entity.builder()
                .id(UUID.randomUUID().toString())
                .name("Contract Value")
                .entityType(EntityType.MONETARY_VALUE)
                .value("$50,000")
                .sourceContext("Total contract value: $50,000");
    }

    public static Entity.EntityBuilder sampleObligationEntity() {
        return Entity.builder()
                .id(UUID.randomUUID().toString())
                .name("Delivery Obligation")
                .entityType(EntityType.OBLIGATION)
                .value("Deliver within 5 business days")
                .sourceContext("Seller must deliver goods within 5 business days");
    }

    public static Entity.EntityBuilder sampleClauseEntity() {
        return Entity.builder()
                .id(UUID.randomUUID().toString())
                .name("Force Majeure")
                .entityType(EntityType.CLAUSE)
                .value("Suspension of obligations during force majeure")
                .sourceContext("Section 12: Force Majeure clause");
    }

    // ===== Conflict Builders =====

    public static Conflict.ConflictBuilder sampleHighConflict() {
        return Conflict.builder()
                .id(UUID.randomUUID().toString())
                .description("Payment term conflict between contract and terms")
                .severity(ConflictSeverity.HIGH)
                .reasoning("Contract specifies 14 days while T&C specifies 30 days")
                .legalPrinciple("Lex Specialis")
                .detectedAt(LocalDateTime.now());
    }

    public static Conflict.ConflictBuilder sampleCriticalConflict() {
        return Conflict.builder()
                .id(UUID.randomUUID().toString())
                .description("Penalty rate exceeds legal maximum")
                .severity(ConflictSeverity.CRITICAL)
                .reasoning("Contract penalty (5%) exceeds regulatory maximum (0.5%)")
                .legalPrinciple("Regulatory Compliance")
                .detectedAt(LocalDateTime.now());
    }

    public static Conflict.ConflictBuilder sampleMediumConflict() {
        return Conflict.builder()
                .id(UUID.randomUUID().toString())
                .description("Delivery timeframe inconsistency")
                .severity(ConflictSeverity.MEDIUM)
                .reasoning("Different delivery expectations in contract vs SLA")
                .legalPrinciple("Lex Posterior")
                .detectedAt(LocalDateTime.now());
    }

    public static Conflict.ConflictBuilder sampleLowConflict() {
        return Conflict.builder()
                .id(UUID.randomUUID().toString())
                .description("Minor wording inconsistency")
                .severity(ConflictSeverity.LOW)
                .reasoning("Different terminology for same concept")
                .legalPrinciple(null)
                .detectedAt(LocalDateTime.now());
    }

    // ===== DTO Builders for AI Service Mocking =====

    public static ExtractionResult.ExtractionResultBuilder sampleExtractionResult() {
        return ExtractionResult.builder()
                .documentSummary("Sample document with payment terms and penalties")
                .entities(List.of(
                        ExtractedEntity.builder()
                                .name("Payment Term")
                                .entityType(EntityType.TIME_PERIOD)
                                .value("14 days")
                                .sourceContext("Payment within 14 days")
                                .build()
                ));
    }

    public static ExtractedEntity.ExtractedEntityBuilder sampleExtractedEntity() {
        return ExtractedEntity.builder()
                .name("Sample Entity")
                .entityType(EntityType.CLAUSE)
                .value("Sample value")
                .sourceContext("Sample context");
    }

    public static ConflictAnalysisDto.ConflictAnalysisDtoBuilder sampleConflictAnalysisDto() {
        return ConflictAnalysisDto.builder()
                .overallSummary("Analysis complete: 1 conflict detected")
                .conflicts(List.of(
                        DetectedConflict.builder()
                                .description("Sample conflict")
                                .severity(ConflictSeverity.HIGH)
                                .reasoning("Sample reasoning")
                                .legalPrinciple("Lex Specialis")
                                .involvedEntityNames(List.of("Entity A", "Entity B"))
                                .build()
                ));
    }

    public static DetectedConflict.DetectedConflictBuilder sampleDetectedConflict() {
        return DetectedConflict.builder()
                .description("Sample detected conflict")
                .severity(ConflictSeverity.HIGH)
                .reasoning("Conflicting values between documents")
                .legalPrinciple("Lex Specialis")
                .involvedEntityNames(List.of("Entity 1", "Entity 2"));
    }

    // ===== Composite Test Data =====

    /**
     * Creates a document with associated entities.
     */
    public static Document createDocumentWithEntities(String name, DocumentType type, List<Entity> entities) {
        return Document.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .content("Content for " + name)
                .documentType(type)
                .entities(new ArrayList<>(entities))
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a conflict involving specified entities.
     */
    public static Conflict createConflictWithEntities(ConflictSeverity severity, List<Entity> entities) {
        return Conflict.builder()
                .id(UUID.randomUUID().toString())
                .description("Conflict involving " + entities.size() + " entities")
                .severity(severity)
                .reasoning("Test reasoning")
                .entities(new ArrayList<>(entities))
                .detectedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a typical payment term conflict scenario with two documents.
     */
    public static PaymentConflictScenario createPaymentConflictScenario() {
        Entity contractTerm = samplePaymentTermEntity()
                .name("Contract Payment Term")
                .value("14 days")
                .build();

        Entity tcTerm = Entity.builder()
                .id(UUID.randomUUID().toString())
                .name("Standard Payment Term")
                .entityType(EntityType.TIME_PERIOD)
                .value("30 days")
                .sourceContext("Standard payment terms: 30 days")
                .build();

        Document contract = createDocumentWithEntities(
                "Sales Contract", DocumentType.CONTRACT, List.of(contractTerm));

        Document tc = createDocumentWithEntities(
                "Terms and Conditions", DocumentType.TERMS_AND_CONDITIONS, List.of(tcTerm));

        DetectedConflict detected = DetectedConflict.builder()
                .description("Payment term conflict: 14 days vs 30 days")
                .severity(ConflictSeverity.HIGH)
                .reasoning("Contract specifies shorter payment term than T&C")
                .legalPrinciple("Lex Specialis")
                .involvedEntityNames(List.of("Contract Payment Term", "Standard Payment Term"))
                .build();

        return new PaymentConflictScenario(contract, tc, contractTerm, tcTerm, detected);
    }

    /**
     * Record containing a complete payment conflict test scenario.
     */
    public record PaymentConflictScenario(
            Document contract,
            Document termsAndConditions,
            Entity contractPaymentTerm,
            Entity tcPaymentTerm,
            DetectedConflict expectedConflict
    ) {
        public List<String> getDocumentIds() {
            return List.of(contract.getId(), termsAndConditions.getId());
        }

        public List<Entity> getAllEntities() {
            return List.of(contractPaymentTerm, tcPaymentTerm);
        }
    }
}
