package com.conflictguard.repository;

import com.conflictguard.domain.Entity;
import com.conflictguard.domain.EntityType;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntityRepository extends Neo4jRepository<Entity, String> {

    List<Entity> findByEntityType(EntityType entityType);

    Optional<Entity> findByName(String name);

    @Query("MATCH (e:Entity) WHERE e.name IN $names RETURN e")
    List<Entity> findByNameIn(List<String> names);

    @Query("""
            MATCH (d:Document)-[:CONTAINS]->(e:Entity)
            WHERE d.id IN $documentIds
            RETURN DISTINCT e
            """)
    List<Entity> findByDocumentIds(List<String> documentIds);

    @Query("""
            MATCH (e1:Entity)-[r:RELATES_TO]->(e2:Entity)
            WHERE r.relationshipType = 'CONFLICTS_WITH'
            RETURN e1, e2
            """)
    List<Entity> findEntitiesWithConflicts();

    @Query("""
            MATCH (d:Document)-[:CONTAINS]->(e:Entity)
            WHERE d.id = $documentId
            RETURN e
            """)
    List<Entity> findByDocumentId(String documentId);

}
