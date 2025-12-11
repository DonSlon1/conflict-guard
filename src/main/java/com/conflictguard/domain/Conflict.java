package com.conflictguard.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Node("Conflict")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conflict {

    @Id
    @GeneratedValue(UUIDStringGenerator.class)
    private String id;

    private String description;

    private ConflictSeverity severity;

    private String reasoning;

    private String legalPrinciple;

    private LocalDateTime detectedAt;

    @Relationship(type = "INVOLVES", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private List<Entity> entities = new ArrayList<>();

    public void addEntity(Entity entity) {
        if (entities == null) {
            entities = new ArrayList<>();
        }
        entities.add(entity);
    }
}
