# Module 04: RAG Foundations & Chunking Strategies 📚

Generative AI ka sabse practical aur high-demand backend pattern **RAG (Retrieval-Augmented Generation)** hai. Agar aapko ek aisa AI system banana hai jo aapki company ke internal HR policies, PDF documents, ya proprietary database par bina hallucinate kiye 100% accurate jawab de, toh RAG hi solution hai!

---

## 1. Why RAG? The Core Problems It Solves

```
+─────────────────────────────────────────────────────────────+
|               WHY BASE LLMs FAIL IN PRODUCTION              |
+─────────────────────────────────────────────────────────────+
| 1. HALLUCINATIONS:                                          |
|    Jab model ko answer nahi pata hota, woh confident tone   |
|    me jhooth (hallucination) bol deta hai.                  |
| 2. KNOWLEDGE CUTOFF:                                        |
|    Model train hone ke baad internet/world me kya hua, use  |
|    kuch nahi pata.                                          |
| 3. PRIVATE ENTERPRISE DATA:                                 |
|    Company ke internal Confluence docs, confidential code   |
|    aur customer orders public models ke paas nahi hain.     |
| 4. AUDITABILITY & CITATIONS:                                |
|    Enterprise me jawab ke saath proof (source document &   |
|    page number) chahiye hota hai.                           |
+─────────────────────────────────────────────────────────────+
```

**Solution:** LLM ko "open-book exam" do! User ke question ke basis par database se exact relevant paragraphs dhoondo, prompt me inject karo, aur LLM ko bolo: *"Sirf is context ke basis par answer do, bahar se kuch assume mat karo."*

---

## 2. End-to-End RAG Architecture

RAG system do independent asynchronous pipelines me kaam karta hai:

```
[ PIPELINE 1: INGESTION (Offline / Background Job) ]
Documents (PDF/Docx/HTML) ──> [ Chunker ] ──> Text Chunks ──> [ Embedding Model ] ──> [ Vector DB (pgvector) ]

───────────────────────────────────────────────────────────────────────────────────────────────────

[ PIPELINE 2: RETRIEVAL & GENERATION (Real-time Online User Request) ]
User Query ──> [ Embedding Model ] ──> Query Vector
                                            │
                                            ▼
                              [ Vector DB: Top-K Chunks ]
                                            │
                                            ▼
                  [ Augmented Prompt = System Prompt + Chunks + User Query ]
                                            │
                                            ▼
                                     [ LLM (GPT-4o) ]
                                            │
                                            ▼
                               [ Grounded Accurate Response ]
```

---

## 3. Chunking Strategies: The Foundation of RAG

Aapke RAG system ki quality 80% is baat par depend karti hai ki aapne documents ko chunks me kaise toda hai (**Garbage in = Garbage out**).

```
"Machine learning is a field of AI. [CHUNK BOUNDARY] It allows computers to learn."
```
Agar chunk beech me toot gaya, toh model context kho dega.

### A. Fixed-Size Chunking with Overlap (Standard Baseline)
- Chunks ko fixed number of characters ya tokens me todna (e.g. 500 tokens).
- **Chunk Overlap (e.g. 50-100 tokens):** Pichhle chunk ke aakhri kuch words agle chunk ke shuru me repeat hote hain.
  - *Kyun zaroori hai?* Taaki chunk boundary par sentences aadhe-adhoore na katein aur context flow barkarar rahe!

```
Chunk 1: [ .................... Sentence A. Sentence B. ]
                                    └─── Overlap ───┐
Chunk 2:                            [ Sentence B. Sentence C. Sentence D. ]
```

### B. Recursive Character Text Splitting (Industry Best Practice)
Yeh hierarchically separators try karta hai jab tak chunk size threshold ke andar na aa jaye:
1. Double Newline (`\n\n`) -> Paragraph boundary
2. Single Newline (`\n`) -> Line boundary
3. Space (` `) -> Word boundary
4. Empty string (`""`) -> Character boundary (Last resort)

Isse paragraphs aur sentences natural tareeqe se intact rehte hain!

### C. Document-Aware / Markdown Chunking
Markdown, HTML ya Code files ke liye. Yeh structure preserve karta hai:
- `# Header 1` ke neeche ka text alag chunk
- `## Header 2` ke neeche ka text alag chunk
- Code blocks (````java ... ````) kabhi beech me split nahi hote!
- Tables rows ek sath rehti hain.

### D. Semantic Chunking
- Sabhi consecutive sentences ka embedding nikalta hai.
- Agar do consecutive sentences ke embeddings me Cosine Distance sudden spike karti hai (topic shift hua), toh wahi chunk split ho jata hai.
- Super accurate, lekin thoda slow aur compute-intensive hota hai.

---

## 4. Chunk Size Trade-Off Matrix

| Chunk Size | Pros | Cons | Ideal For |
| :--- | :--- | :--- | :--- |
| **Small Chunks (100-250 tokens)** | High semantic precision, retrieval me noise kam hota hai. | Broader context miss ho sakta hai. | Fact retrieval, FAQ matching, glossary lookup. |
| **Medium Chunks (500-800 tokens)** | Good balance of context and specificity. Gold standard! | Thoda irrelevant context pass ho sakta hai. | General documents, technical documentation, manuals. |
| **Large Chunks (1000-2000 tokens)** | Pura narrative aur surrounding context milta hai. | Embedding diffuse ho jati hai (needle-in-haystack problem). | Legal contract review, book summarization. |

---

## 5. Multi-Tenancy & Metadata Filtering in Backend

Enterprise applications me sabse badi security requirement hoti hai: **User A ko User B ya kisi doosri company ka confidential data nahi dikhna chahiye!**

Sirf vector similarity search karne se data leak ho sakta hai. Isliye hum **Metadata Pre-Filtering** use karte hain:

```sql
SELECT content, similarity 
FROM document_chunks
WHERE tenant_id = :currentTenantId          -- Multi-tenant isolation
  AND allowed_roles && :userRoles           -- Role-based access (RBAC)
  AND is_archived = FALSE                   -- Soft delete filter
ORDER BY embedding <=> :queryVector
LIMIT 5;
```

Vector index ko filter ke saath combine karke database level par strict security guarantee ki jaati hai!

---

## 6. Summary Checklist
- [x] RAG solves Hallucination, Knowledge Cutoff, and Enterprise Data Privacy.
- [x] Ingestion pipeline is offline; Retrieval pipeline is real-time.
- [x] Always use 10-15% Chunk Overlap to prevent context cutoff.
- [x] Recursive Character Splitting preserves paragraph integrity.
- [x] Always apply Metadata Filtering for multi-tenancy and RBAC before returning context to LLM.

