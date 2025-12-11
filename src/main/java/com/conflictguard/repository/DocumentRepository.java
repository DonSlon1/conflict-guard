package com.conflictguard.repository;

import com.conflictguard.domain.Document;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends Neo4jRepository<Document, String> {

    Optional<Document> findByName(String name);

    List<Document> findAllByOrderByCreatedAtDesc();

    @Query("""
            MATCH (d:Document)-[:CONTAINS]->(e:Entity)
            WHERE e.id = $entityId
            RETURN d
            """)
    Optional<Document> findByEntityId(String entityId);
}
