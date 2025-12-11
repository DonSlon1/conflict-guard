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

import java.util.ArrayList;
import java.util.List;

@Node("Entity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Entity {

    @Id
    @GeneratedValue(UUIDStringGenerator.class)
    private String id;

    private String name;

    private EntityType entityType;

    private String value;

    private String sourceContext;

    @Relationship(type = "RELATES_TO", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private List<EntityRelationship> relatedEntities = new ArrayList<>();

    public void addRelationship(Entity target, RelationshipType type) {
        if (relatedEntities == null) {
            relatedEntities = new ArrayList<>();
        }
        relatedEntities.add(EntityRelationship.builder()
                .targetEntity(target)
                .relationshipType(type)
                .build());
    }
}
