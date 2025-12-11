package com.conflictguard.repository;

import com.conflictguard.domain.Conflict;
import com.conflictguard.domain.ConflictSeverity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConflictRepository extends Neo4jRepository<Conflict, String> {

    List<Conflict> findBySeverity(ConflictSeverity severity);

    @Query("""
            MATCH (c:Conflict)-[:INVOLVES]->(e:Entity)
            WHERE e.id = $entityId
            RETURN c
            """)
    List<Conflict> findByEntityId(String entityId);

    @Query("""
            MATCH (c:Conflict)-[:INVOLVES]->(e:Entity)<-[:CONTAINS]-(d:Document)
            WHERE d.id IN $documentIds
            RETURN DISTINCT c
            """)
    List<Conflict> findByDocumentIds(List<String> documentIds);

    @Query("MATCH (c:Conflict) RETURN c ORDER BY c.detectedAt DESC")
    List<Conflict> findAllOrderByDetectedAtDesc();

    @Query("""
            MATCH (c:Conflict)-[:INVOLVES]->(e:Entity)
            WHERE e.id IN $entityIds
            WITH c, collect(e.id) as involvedIds
            WHERE size(involvedIds) >= 2
            RETURN DISTINCT c
            """)
    List<Conflict> findByInvolvedEntityIds(List<String> entityIds);
}
