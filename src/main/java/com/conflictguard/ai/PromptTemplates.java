package com.conflictguard.ai;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Loads prompt templates from resource files.
 * This allows prompts to be edited without recompiling.
 */
@Component
@Slf4j
public class PromptTemplates {

  @Value("classpath:prompts/entity-extraction-system.txt")
  private Resource entityExtractionSystemResource;

  @Value("classpath:prompts/entity-extraction-user.txt")
  private Resource entityExtractionUserResource;

  @Value("classpath:prompts/conflict-reasoning-system.txt")
  private Resource conflictReasoningSystemResource;

  @Value("classpath:prompts/conflict-reasoning-user.txt")
  private Resource conflictReasoningUserResource;

  @Getter
  private String entityExtractionSystemPrompt;
  private String entityExtractionUserTemplate;
  @Getter
  private String conflictReasoningSystemPrompt;
  private String conflictReasoningUserTemplate;

  @PostConstruct
  public void loadTemplates() throws IOException {
    entityExtractionSystemPrompt = loadResource(entityExtractionSystemResource);
    entityExtractionUserTemplate = loadResource(entityExtractionUserResource);
    conflictReasoningSystemPrompt = loadResource(conflictReasoningSystemResource);
    conflictReasoningUserTemplate = loadResource(conflictReasoningUserResource);
    log.info("Loaded {} prompt templates from resources", 4);
  }

  private String loadResource(Resource resource) throws IOException {
    return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
  }

    public String getEntityExtractionUserPrompt(String documentType, String documentName, String content) {
    return entityExtractionUserTemplate
        .replace("{documentType}", documentType)
        .replace("{documentName}", documentName)
        .replace("{content}", content);
  }

    public String getConflictReasoningUserPrompt(String entities) {
    return conflictReasoningUserTemplate.replace("{entities}", entities);
  }
}
