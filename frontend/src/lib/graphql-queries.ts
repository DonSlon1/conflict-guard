import { gql } from "@apollo/client";

export const GET_DOCUMENTS = gql`
  query GetDocuments {
    documents {
      id
      name
      documentType
      createdAt
      entities {
        id
        name
        entityType
        value
      }
    }
  }
`;

export const GET_ENTITIES = gql`
  query GetEntities($type: EntityType) {
    entities(type: $type) {
      id
      name
      entityType
      value
      sourceDocument {
        id
        name
      }
      conflicts {
        id
        description
        severity
      }
    }
  }
`;

export const GET_CONFLICTS = gql`
  query GetConflicts($severity: ConflictSeverity) {
    conflicts(severity: $severity) {
      id
      description
      severity
      reasoning
      legalPrinciple
      entities {
        id
        name
        value
        entityType
      }
    }
  }
`;

export const ANALYZE_CONFLICTS = gql`
  query AnalyzeConflicts($documentIds: [ID!]!) {
    analyzeConflicts(documentIds: $documentIds) {
      summary
      analyzedAt
      conflicts {
        id
        description
        severity
        reasoning
        legalPrinciple
        entities {
          id
          name
          value
        }
      }
    }
  }
`;

export const INGEST_DOCUMENT = gql`
  mutation IngestDocument($input: DocumentInput!) {
    ingestDocument(input: $input) {
      id
      name
      documentType
      entities {
        id
        name
        entityType
        value
      }
    }
  }
`;

export const DELETE_DOCUMENT = gql`
  mutation DeleteDocument($id: ID!) {
    deleteDocument(id: $id)
  }
`;
