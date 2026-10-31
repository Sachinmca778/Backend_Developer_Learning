# Module 03: Vector Databases & pgvector Deep Dive 🐘

Naye developers aksar sochte hain ki AI project shuru karne ke liye ek alag se naya standalone Vector Database (jaise Pinecone ya Milvus) deploy karna padega. **Lekin backend developers ke liye sabse powerful, mature aur sensible option unka purana dost PostgreSQL hai via `pgvector` extension!**

---

## 1. Vector Database Landscape: Dedicated vs Multi-Model

```
+─────────────────────────────────────────────────────────────+
|                 VECTOR DATABASE SOLUTIONS                   |
+──────────────────────────────+──────────────────────────────+
|   DEDICATED VECTOR DATABASES | MULTI-MODEL EXTENSIONS       |
|   (Pinecone, Milvus, Qdrant) | (PostgreSQL pgvector, Redis) |
+──────────────────────────────+──────────────────────────────+
| PROS:                        | PROS:                        |
| - 100M+ vector scale out-    | - **Existing PostgreSQL infra**|
|   of-the-box.                |   reuse hoti hai (Zero new   |
| - Specialized GPU indexing.  |   servers or SaaS bills).    |
|                              | - **ACID Transactions** &    |
| CONS:                        |   Relational Joins (`JOIN`). |
| - Alag database maintain     | - Data sync ka koi jhanjhat  |
|   karna padta hai.           |   nahi (No dual-writes).     |
| - Relational data ke saath   |                              |
|   joins nahi ho sakte.       | CONS:                        |
| - Network hop & extra cost.  | - 50M+ vectors par high RAM  |
|                              |   aur tuning chahiye hoti hai|
+──────────────────────────────+──────────────────────────────+
```

> [!NOTE]
> **Backend Architecture Rule:** Agar aapka vector dataset < 20-30 Million documents hai (jo 95% startups aur enterprise systems ka size hota hai), **PostgreSQL + `pgvector` is unmatched in simplicity and reliability!**

---

## 2. PostgreSQL `pgvector` Setup & Mechanics

### Step 1: Enable Extension
PostgreSQL server me extension enable karein:
```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### Step 2: Table Creation with Vector Column
```sql
CREATE TABLE document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id VARCHAR(64) NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    metadata JSONB,
    embedding vector(1536), -- 1536 for OpenAI text-embedding-3-small
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Step 3: Indexing with HNSW (Hierarchical Navigable Small World)
HNSW index enable karein Cosine distance ke liye:
```sql
CREATE INDEX idx_document_chunks_hnsw 
ON document_chunks 
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);
```
- `m`: Ek node ke kitne links honge (Default 16). Zyada karne se recall badhti hai par memory zyada lagti hai.
- `ef_construction`: Index build karte waqt search depth (Default 64).

### Step 4: Distance Operators in pgvector
| Operator | Distance Metric | Query Description |
| :--- | :--- | :--- |
| `<=>` | **Cosine Distance** | `1 - cosine_similarity`. Text search me 99% yahi use hota hai. |
| `<->` | **Euclidean (L2)** | Straight line distance. |
| `<#>` | **Negative Inner Product** | Normalized vectors ke fast dot product ke liye. |

---

## 3. Querying pgvector: Top-K Similarity Search

User query ka embedding lekar nearest 5 documents fetch karna:

```sql
SELECT 
    id, 
    content, 
    metadata, 
    1 - (embedding <=> '[0.012, -0.451, ..., 0.891]'::vector) AS similarity_score
FROM document_chunks
WHERE metadata->>'tenant_id' = 'enterprise_org_99' -- Multi-tenant filter
ORDER BY embedding <=> '[0.012, -0.451, ..., 0.891]'::vector
LIMIT 5;
```

---

## 4. Spring Boot 3 + Spring Data JPA Integration

PostgreSQL + `pgvector` ko Spring Boot me clean aur performant tarike se kaise integrate karein:

### JPA Entity:
```java
package com.backend.ai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private String documentId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    // pgvector datatype representation
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private float[] embedding;

    // Getters and setters omitted for brevity
}
```

### Spring Data JPA Repository:
```java
package com.backend.ai.repository;

import com.backend.ai.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    @Query(value = """
        SELECT c.id, c.document_id, c.content, 
               (1 - (c.embedding <=> cast(:queryVector as vector))) AS similarity
        FROM document_chunks c
        ORDER BY c.embedding <=> cast(:queryVector as vector)
        LIMIT :topK
        """, nativeQuery = true)
    List<ChunkSimilarityProjection> findSimilarChunks(
            @Param("queryVector") String queryVectorString, 
            @Param("topK") int topK
    );

    interface ChunkSimilarityProjection {
        Long getId();
        String getDocumentId();
        String getContent();
        Double getSimilarity();
    }
}
```

---

## 5. Hybrid Search: Vector + Full-Text Search (BM25)

Pure vector search ki ek weakness hoti hai: Agar user exact product code search kare (jaise *"SKU-9874-TX"*), vector search kabhi-kabhi similar looking keywords dhund kar exact code miss kar deta hai.

Is problem ko solve karta hai **Hybrid Search**:
1. **Keyword Search (BM25 / tsvector):** Exact matches aur rare product IDs nikalta hai.
2. **Dense Vector Search (pgvector):** Conceptual aur semantic context nikalta hai.
3. **RRF (Reciprocal Rank Fusion):** Dono ranking scores ko merge karke sabse best results filter karta hai.

### Reciprocal Rank Fusion Formula:
$$RRF\_Score(d) = \sum_{m \in M} \frac{1}{60 + \text{rank}_m(d)}$$

PostgreSQL me aap ek hi query me `to_tsvector('english', content)` aur `embedding <=> vector` ko combine karke enterprise-grade search bana sakte hain!

---

## 6. Key Takeaways
- PostgreSQL + `pgvector` removes the need for separate vector DB infrastructure in most applications.
- HNSW index gives sub-millisecond search at high recall.
- Use Cosine Distance (`<=>`) for text retrieval.
- Hybrid Search (Vector + Full-Text) delivers unmatched accuracy for real enterprise products.

