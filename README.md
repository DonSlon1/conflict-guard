# ConflictGuard

**AI-Powered Legal Document Conflict Detection using Graph-Based Reasoning**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Neo4j](https://img.shields.io/badge/Neo4j-5-blue.svg)](https://neo4j.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

ConflictGuard ingests legal documents, extracts structured entities using AI, builds a Knowledge Graph in Neo4j, and detects logical contradictions through semantic reasoning.

---

## Table of Contents

- [The Problem](#the-problem)
- [Architectural Decision Record (ADR)](#architectural-decision-record-adr)
- [System Architecture](#system-architecture)
- [Quick Start](#quick-start)
- [Demo Script](#demo-script)
- [API Reference](#api-reference)
- [Project Structure](#project-structure)
- [Development](#development)
- [Testing](#testing)
- [Configuration](#configuration)
- [License](#license)

---

## The Problem

Legal departments manage thousands of documents: contracts, terms of service, internal directives, and regulatory requirements. These documents frequently contradict each other:

- **Contract A** says: *"Payment due in 14 days"*
- **Terms of Service** says: *"Standard payment term is 30 days"*

**Which one applies?** Legal principles like *lex specialis* (specific overrides general) provide the answer, but finding these conflicts manually is:

1. **Time-consuming**: Lawyers spend hours cross-referencing documents
2. **Error-prone**: Human reviewers miss subtle contradictions
3. **Unscalable**: Volume grows faster than review capacity

**ConflictGuard automates this process using AI-powered reasoning over a Knowledge Graph.**

---

## Architectural Decision Record (ADR)

### ADR-001: Why GraphRAG over Vector RAG?

**Context**: We needed to detect logical conflicts between legal documents. Two main approaches exist:

| Approach | How It Works | Strengths | Weaknesses |
|----------|--------------|-----------|------------|
| **Vector RAG** | Embed documents as vectors, retrieve by semantic similarity | Fast retrieval, good for Q&A | Loses structural relationships |
| **GraphRAG** | Store entities and relationships in a graph, traverse for reasoning | Preserves structure, enables multi-hop reasoning | More complex setup |

**Decision**: We chose **GraphRAG (Neo4j + AI Reasoning)**.

**Rationale**:

1. **Legal documents are relational, not just textual**
   - A contract *references* terms of service
   - A clause *overrides* a default condition
   - An amendment *modifies* a previous agreement

   Vector embeddings flatten these relationships into similarity scores. Graphs preserve them.

2. **Conflict detection requires multi-hop reasoning**
   ```
   Contract A --[CONTAINS]--> Entity: "14-day payment"
   Terms B --[CONTAINS]--> Entity: "30-day payment"
   Entity --[CONFLICTS_WITH]--> Entity
   ```
   Finding this path in a graph is O(1) per hop. In vectors, it requires multiple retrievals and post-processing.

3. **Legal principles are graph algorithms**
   - *Lex specialis*: Traverse to find the most specific document
   - *Lex posterior*: Sort by `createdAt` timestamp on edges
   - *Hierarchy*: Walk the document hierarchy tree

4. **Explainability matters in legal tech**
   - Graphs provide audit trails: "Conflict found via path A → B → C"
   - Vector similarity is a black box: "These are 0.87 similar"

**Consequences**:
- (+) Accurate conflict detection with explainable reasoning
- (+) Natural fit for legal domain concepts
- (-) Requires Neo4j infrastructure
- (-) More complex entity extraction pipeline

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        Frontend (Next.js 14 + TailwindCSS)                       │
│                              http://localhost:3000                               │
│    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                     │
│    │  Upload Docs │    │  View Graph  │    │ View Conflicts│                     │
│    └──────────────┘    └──────────────┘    └──────────────┘                     │
└─────────────────────────────────┬───────────────────────────────────────────────┘
                                  │ Apollo Client (GraphQL)
┌─────────────────────────────────▼───────────────────────────────────────────────┐
│                     Backend (Spring Boot 3.2 + Spring GraphQL)                   │
│                              http://localhost:8080                               │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                           GraphQL API Layer                              │    │
│  │   QueryController ──── MutationController ──── EntityResolver            │    │
│  └───────────────────────────────┬─────────────────────────────────────────┘    │
│                                  │                                               │
│  ┌───────────────────────────────▼─────────────────────────────────────────┐    │
│  │                          Service Layer                                   │    │
│  │   DocumentService ──── EntityService ──── ConflictService                │    │
│  └───────────────────────────────┬─────────────────────────────────────────┘    │
│                                  │                                               │
│         ┌────────────────────────┴────────────────────────┐                     │
│         │                                                  │                     │
│  ┌──────▼──────┐                                   ┌──────▼──────┐              │
│  │  AI Layer   │                                   │  Data Layer │              │
│  │             │                                   │             │              │
│  │ OpenRouter  │                                   │ Neo4j Repos │              │
│  │   Client    │                                   │             │              │
│  │             │                                   │ - Document  │              │
│  │ Extraction  │                                   │ - Entity    │              │
│  │  Service    │                                   │ - Conflict  │              │
│  │             │                                   │             │              │
│  │ Reasoning   │                                   └──────┬──────┘              │
│  │  Service    │                                          │                     │
│  └──────┬──────┘                                          │                     │
└─────────┼─────────────────────────────────────────────────┼─────────────────────┘
          │                                                  │
          │ HTTPS                                            │ Bolt
          ▼                                                  ▼
   ┌─────────────┐                                   ┌─────────────┐
   │  OpenRouter │                                   │    Neo4j    │
   │     API     │                                   │   Database  │
   │  (DeepSeek) │                                   │  bolt:7687  │
   └─────────────┘                                   └─────────────┘
```

### Data Flow

1. **Document Ingestion**
   ```
   User uploads document → GraphQL Mutation → DocumentService
   → EntityExtractionService (AI) → Extract entities & relationships
   → Neo4j → Store Document node with Entity nodes
   ```

2. **Conflict Detection**
   ```
   User triggers analysis → GraphQL Mutation → ConflictService
   → Load entities from Neo4j → ConflictReasoningService (AI)
   → Apply legal principles → Store Conflict nodes with INVOLVES edges
   → Return conflicts with reasoning
   ```

### Graph Schema

```cypher
// Nodes
(:Document {id, name, content, documentType, createdAt})
(:Entity {id, name, entityType, value, sourceContext})
(:Conflict {id, description, severity, reasoning, legalPrinciple, detectedAt})

// Relationships
(Document)-[:CONTAINS]->(Entity)
(Entity)-[:RELATES_TO {relationshipType}]->(Entity)
(Conflict)-[:INVOLVES]->(Entity)
```

---

## Quick Start

### Prerequisites

- Docker & Docker Compose
- OpenRouter API key ([Get one here](https://openrouter.ai/keys))

### One-Command Startup

```bash
# Clone the repository
git clone https://github.com/yourusername/conflictguard.git
cd conflictguard

# Start with your API key and Neo4j password
OPENROUTER_API_KEY=sk-or-v1-your-key-here \
NEO4J_PASSWORD=your-secure-password \
docker-compose up -d

# Wait for services to be healthy (~60 seconds)
docker-compose ps

# Open the application
open http://localhost:3000
```

### Service Endpoints

| Service | URL | Purpose |
|---------|-----|---------|
| **Frontend** | http://localhost:3000 | User Interface |
| **GraphQL API** | http://localhost:8080/graphql | API Endpoint |
| **GraphiQL** | http://localhost:8080/graphiql | API Explorer |
| **Neo4j Browser** | http://localhost:7474 | Database UI |

---

## Demo Script

### Step 1: Ingest a Contract

Open GraphiQL at http://localhost:8080/graphiql and run:

```graphql
mutation IngestContract {
  ingestDocument(input: {
    name: "Software License Agreement 2024"
    documentType: CONTRACT
    content: """
    SOFTWARE LICENSE AGREEMENT

    1. PAYMENT TERMS
    All invoices are due within 14 calendar days of the invoice date.
    Late payments will incur a 2% monthly penalty.

    2. TERMINATION
    Either party may terminate with 30 days written notice.
    Upon termination, all outstanding payments become immediately due.

    3. LIABILITY
    Total liability shall not exceed the fees paid in the last 12 months.
    """
  }) {
    id
    name
    entities {
      name
      entityType
      value
    }
  }
}
```

### Step 2: Ingest Terms of Service

```graphql
mutation IngestTerms {
  ingestDocument(input: {
    name: "Standard Terms of Service 2024"
    documentType: TERMS_AND_CONDITIONS
    content: """
    STANDARD TERMS OF SERVICE

    1. PAYMENT
    Standard payment terms are net 30 days unless otherwise specified.
    Late payments may result in service suspension.

    2. CANCELLATION
    Services may be cancelled with 60 days notice.
    No refunds for partial months.

    3. LIABILITY
    Our total liability is limited to fees paid in the last 6 months.
    """
  }) {
    id
    name
    entities {
      name
      entityType
      value
    }
  }
}
```

### Step 3: Detect Conflicts

```graphql
mutation AnalyzeConflicts {
  analyzeConflicts(documentIds: ["<contract-id>", "<terms-id>"]) {
    conflicts {
      id
      description
      severity
      reasoning
      legalPrinciple
      entities {
        name
        entityType
        value
      }
    }
    summary
    analyzedAt
  }
}
```

### Expected Output

The AI will detect conflicts such as:

| Conflict | Severity | Legal Principle |
|----------|----------|-----------------|
| Payment terms: 14 days vs 30 days | HIGH | **Lex Specialis** - Contract overrides general terms |
| Termination notice: 30 days vs 60 days | MEDIUM | **Lex Specialis** - Contract is more specific |
| Liability cap: 12 months vs 6 months | HIGH | **Conflict** - Ambiguous, needs resolution |

---

## API Reference

### Queries

```graphql
# Get all documents
query { documents { id name documentType createdAt } }

# Get document by ID with entities
query { document(id: "uuid") { id name entities { name entityType } } }

# Get all conflicts
query { conflicts { id description severity reasoning } }

# Filter conflicts by severity
query { conflicts(severity: HIGH) { id description } }
```

### Mutations

```graphql
# Ingest document (triggers AI entity extraction)
mutation { ingestDocument(input: { name, content, documentType }) { id } }

# Analyze conflicts between documents
mutation { analyzeConflicts(documentIds: ["id1", "id2"]) { conflicts { id } } }

# Delete document
mutation { deleteDocument(id: "uuid") }
```

---

## Project Structure

```
conflictguard/
├── src/main/java/com/conflictguard/
│   ├── ConflictGuardApplication.java    # Spring Boot entry point
│   ├── ai/                               # AI Integration Layer
│   │   ├── OpenRouterClient.java         # HTTP client for OpenRouter API
│   │   ├── EntityExtractionService.java  # Entity extraction orchestration
│   │   ├── ConflictReasoningService.java # Conflict detection orchestration
│   │   └── PromptTemplates.java          # Loads prompts from resources
│   ├── config/                           # Spring Configuration
│   │   ├── JacksonConfig.java            # JSON serialization
│   │   └── Neo4jConfig.java              # Transaction management
│   ├── domain/                           # Neo4j Entity Classes
│   │   ├── Document.java                 # @Node for documents
│   │   ├── Entity.java                   # @Node for extracted entities
│   │   ├── Conflict.java                 # @Node for detected conflicts
│   │   └── *Type.java                    # Enums (DocumentType, EntityType, etc.)
│   ├── dto/                              # Data Transfer Objects
│   │   ├── ExtractedEntity.java          # AI extraction response
│   │   └── DetectedConflict.java         # AI conflict response
│   ├── graphql/                          # GraphQL API Layer
│   │   ├── QueryController.java          # @QueryMapping handlers
│   │   ├── MutationController.java       # @MutationMapping handlers
│   │   └── EntityResolver.java           # @SchemaMapping resolvers
│   ├── repository/                       # Data Access Layer
│   │   ├── DocumentRepository.java       # Neo4j queries for documents
│   │   ├── EntityRepository.java         # Neo4j queries for entities
│   │   └── ConflictRepository.java       # Neo4j queries for conflicts
│   └── service/                          # Business Logic Layer
│       ├── DocumentService.java          # Document ingestion & management
│       ├── EntityService.java            # Entity queries & filtering
│       └── ConflictService.java          # Conflict analysis & storage
├── src/main/resources/
│   ├── application.yml                   # Spring Boot configuration
│   ├── graphql/schema.graphqls           # GraphQL schema definition
│   └── prompts/                          # AI Prompt Templates
│       ├── entity-extraction-*.txt       # Entity extraction prompts
│       └── conflict-reasoning-*.txt      # Conflict detection prompts
├── src/test/java/                        # Test Suite
│   ├── ai/                               # AI service unit tests
│   ├── service/                          # Business logic tests
│   ├── graphql/                          # GraphQL endpoint tests
│   ├── repository/                       # Integration tests (Testcontainers)
│   └── e2e/                              # End-to-end tests
├── frontend/                             # Next.js Frontend
│   ├── src/app/                          # App Router pages
│   ├── src/components/                   # React components
│   └── src/lib/                          # Apollo Client setup
├── docker-compose.yml                    # Full-stack orchestration
├── Dockerfile                            # Backend container
└── README.md                             # This file
```

---

## Development

### Backend Only (requires local Neo4j)

```bash
# Set environment variables
export OPENROUTER_API_KEY=sk-or-v1-your-key
export SPRING_NEO4J_AUTHENTICATION_PASSWORD=your-password

# Run with Gradle
./gradlew bootRun
```

### Frontend Only

```bash
cd frontend
npm install
npm run dev
```

### Full Stack with Docker

```bash
OPENROUTER_API_KEY=your-key NEO4J_PASSWORD=your-password docker-compose up --build
```

---

## Testing

```bash
# Unit tests (no Docker required)
./gradlew unitTest

# Integration tests (requires Docker for Testcontainers)
./gradlew integrationTest

# All tests with coverage report
./gradlew test jacocoUnitTestReport

# View coverage report
open build/reports/jacoco/jacocoUnitTestReport/html/index.html
```

---

## Configuration

### Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `OPENROUTER_API_KEY` | OpenRouter API key for AI | **Yes** | - |
| `NEO4J_PASSWORD` | Neo4j database password | **Yes** | - |
| `SPRING_NEO4J_URI` | Neo4j connection URI | No | `bolt://localhost:7687` |
| `OPENROUTER_MODEL` | AI model to use | No | `tngtech/deepseek-r1t2-chimera:free` |
| `LOG_LEVEL` | Logging verbosity | No | `INFO` |
| `GRAPHIQL_ENABLED` | Enable GraphiQL UI | No | `true` |

### Security Recommendations (Production)

1. **Disable GraphiQL**: Set `GRAPHIQL_ENABLED=false`
2. **Use secrets management**: HashiCorp Vault, AWS Secrets Manager, etc.
3. **Enable HTTPS**: Use a reverse proxy (nginx, Traefik)
4. **Restrict CORS**: Update `FRONTEND_URL` to your production domain
5. **Add authentication**: Implement JWT or OAuth2

---

## Tech Stack

| Layer | Technology | Purpose |
|-------|------------|---------|
| **Runtime** | Java 21 | Modern Java with Records, Pattern Matching |
| **Framework** | Spring Boot 3.2 | Application framework |
| **API** | Spring GraphQL | GraphQL server |
| **Database** | Neo4j 5 | Graph database for relationships |
| **AI** | OpenRouter (DeepSeek R1) | Entity extraction & reasoning |
| **Frontend** | Next.js 14 | React framework |
| **Styling** | TailwindCSS | Utility-first CSS |
| **GraphQL Client** | Apollo Client | Frontend data fetching |
| **Build** | Gradle 8.5 | Java build tool |
| **Containers** | Docker Compose | Orchestration |
| **Testing** | JUnit 5, Testcontainers | Unit & integration tests |

---

## License

MIT License - see [LICENSE](LICENSE) for details.

---

## Author

Built for the LegalTech Hackathon 2024.

*ConflictGuard demonstrates how Graph-based AI reasoning can solve real-world legal document analysis challenges.*
