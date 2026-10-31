# Module 04: Advanced RAG, Re-ranking & Evaluation (RAGAS) 🚀

Simple Naive RAG tutorial level par kaam karta hai, lekin production me users vague questions puchte hain, complex queries daalte hain, aur irrelevant chunks retrieve ho jaate hain. **Advanced RAG un techniques ka collection hai jo retrieval precision ko 95%+ par le jaata hai.**

---

## 1. Why Naive RAG Fails in Production

1. **Vague / Poor User Queries:** User search karta hai: *"refund rules"*. Vector DB confuse ho jata hai kyunki query me context kam hai.
2. **"Lost in the Middle" Phenomenon:** LLMs prompt ke shuru aur aakhir ke text par zyada dhyaan dete hain, beech ke relevant chunks ko ignore kar dete hain.
3. **Bi-Encoder Limitations:** Vector DB fast ANN search ke liye Bi-Encoder embeddings use karta hai, jo deep cross-interaction miss kar deta hai.

---

## 2. Advanced Retrieval Strategies

```
                           [ User Query ]
                                 │
           ┌─────────────────────┼─────────────────────┐
           ▼                     ▼                     ▼
    [ Multi-Query ]           [ HyDE ]          [ Self-Query ]
   (3 Query Variants)   (Hypothetical Answer)  (Metadata Filters)
           │                     │                     │
           └─────────────────────┼─────────────────────┘
                                 ▼
                     [ Vector DB Search: Top 25 ]
                                 │
                                 ▼
                   [ Cross-Encoder / Reranker ]
                                 │
                                 ▼
                         [ Top 5 Best Chunks ]
                                 │
                                 ▼
                          [ LLM Generation ]
```

### A. Multi-Query Expansion
User ek sawal puchta hai, backend LLM ko bolta hai: *"Generate 3 different variations of this question from different perspectives."*
- Original: *"Can I cancel order after dispatch?"*
- Variant 1: *"Shipping cancellation policy post dispatch"*
- Variant 2: *"Return policy for orders in transit"*
- Variant 3: *"Charges for cancelling in-flight delivery"*

Teeno queries ke vectors run hote hain, aur union/deduplicated results aggregate kiye jaate hain. Isse missing information ka chance zero ho jata hai!

### B. HyDE (Hypothetical Document Embeddings)
Agar query bohot chhoti ya conceptual ho:
1. LLM se pehle ek "Hypothetical Answer" (Fake draft answer) generate karwao.
2. Us hypothetical answer ka embedding vector banao.
3. Database me search karo!
*Kyun kaam karta hai?* Query-to-Document similarity ke mukable Document-to-Document similarity hamesha zyada accurate hoti hai!

### C. Parent Document Retriever (Small-to-Big Retrieval)
- Chunks ko do sizes me store karte hain:
  - **Child Chunk (Small, e.g. 100 tokens):** Vector search ke liye (High semantic accuracy).
  - **Parent Document / Chunk (Big, e.g. 1000 tokens):** LLM context me bhejte waqt.
- Search small chunk par hoti hai, lekin jab match milta hai, backend uska pura parent container fetch karke LLM ko bhejta hai. Perfect accuracy + full context!

---

## 3. Two-Stage Retrieval: Bi-Encoders vs Cross-Encoders (Re-ranking)

Yeh production AI architectures ka sabse bada secret hai!

```
+─────────────────────────────────────────────────────────────+
| BI-ENCODER (Vector DB Embedding)                            |
| Text A ──> Model ──> Vector A ──┐                           |
|                                 ├──> Cosine Similarity (Fast|
| Text B ──> Model ──> Vector B ──┘                           |
| Speed: Microseconds | Precision: Medium                     |
+─────────────────────────────────────────────────────────────+
                               vs
+─────────────────────────────────────────────────────────────+
| CROSS-ENCODER (Re-ranker)                                   |
| (Text A + Text B) ──> Full Attention Layers ──> Score (0-1) |
| Speed: 20-50ms      | Precision: EXTREMELY HIGH             |
+─────────────────────────────────────────────────────────────+
```

### The Two-Stage Production Architecture:
1. **Stage 1 (Retrieval):** PostgreSQL pgvector se fast Top-25 documents nikale (< 5ms).
2. **Stage 2 (Re-ranking):** Ek lightweight Cross-Encoder (jaise `Cohere Rerank` ya local `bge-reranker-large`) ko (User Query + 25 Chunks) pass kiya.
3. Reranker sabhi 25 chunks ko deep re-score karta hai aur **Top-5 truly relevant chunks** shortlist karta hai.
4. LLM ko sirf Top-5 chunks bhejte hain! Isse prompt tokens bachte hain, cost kam hoti hai aur hallucinations vanish ho jaate hain.

---

## 4. RAG Evaluation Framework: The RAGAS Triad

Aapko kaise pata chalega ki aapka RAG system sach me achha perform kar raha hai ya nahi? Production me hum **RAGAS (Retrieval Augmented Generation Assessment)** framework ke 4 core metrics use karte hain:

```
                  ┌────────────────────────┐
                  │      User Question     │
                  └───────────┬────────────┘
                              │
               Answer         │ Context
             Relevance        │ Relevance
                              │
          ┌───────────────────┴───────────────────┐
          ▼                                       ▼
┌──────────────────┐  Faithfulness   ┌──────────────────┐
│ Generated Answer │ ──────────────> │ Retrieved Context│
└──────────────────┘                 └──────────────────┘
```

### 1. Faithfulness (Groundedness)
- *Question:* Kya generated answer me likhi har baat retrieved context par based hai, ya model ne apni taraf se man-ghadant baatein jodi hain?
- Score: 0.0 to 1.0 (Target: > 0.95).

### 2. Answer Relevance
- *Question:* Kya answer user ke original question ka direct jawab de raha hai, ya idhar-udhar bhatak raha hai?

### 3. Context Precision (Signal vs Noise)
- *Question:* Retrieved chunks me se kitne percent sach me relevant the aur top ranks par the?

### 4. Context Recall
- *Question:* Kya answer dene ke liye zaroori saari information database se retrieve hui, ya koi critical paragraph miss ho gaya?

---

## 5. Evaluation Workflow in CI/CD Backend Pipeline

Enterprise systems me har model prompt change ya chunking change par automated evaluation test run hota hai:

```
[ Git Pull Request ]
         │
         ▼
[ Run Test Suite of 100 Golden Questions ]
         │
         ▼
[ Compute RAGAS Scores (Faithfulness, Precision) ]
         │
         ├──> Faithfulness < 0.90 ──> ❌ Block PR (Regression detected!)
         │
         └──> Scores Pass (>= 0.95) ─> ✅ Auto-merge & Deploy to Production
```

---

## 6. Summary Checklist
- [x] Naive RAG is prone to vague queries and "lost in the middle" problems.
- [x] Multi-Query and HyDE expand short user prompts into rich search vectors.
- [x] Production RAG = Vector Search (Top 25) + Cross-Encoder Reranking (Top 5).
- [x] Evaluate your RAG pipeline continuously using RAGAS (Faithfulness, Relevance, Context Precision).

