# Module 08: Top 50 AI Interview Questions for Backend Developers 🎯🇮🇳

Yeh questions Tier-1 tech companies, high-growth startups aur enterprise architects ke interviews me puche jaate hain jab unhe ek aisa **Backend Engineer / AI Engineer** hire karna hota hai jo AI models ko reliably scale kar sake.

---

## 📑 Section 1: LLM Fundamentals & Architecture (Q1 - Q10)

### Q1: LLM internally kaise kaam karta hai? Kya ye sach me sochta hai?
**Answer:** Nahi, LLM koi conscious thinking entity nahi hai. Mathematically, LLM ek **Autoregressive Probabilistic Next-Token Predictor** hai jo Transformer architecture par based hai. Input prompt milne par model Attention weights compute karta hai aur vocab ke sabhi tokens par Softmax probability distribution nikalta hai. Sabse high probability wale token ko select karke context me jodta hai aur agla token predict karta hai jab tak stop token na aa jaye.

### Q2: Temperature aur Top-P me kya difference hai? Production backend me kya set karein?
**Answer:** 
- **Temperature:** Softmax logits ko divide karke probability curve ko flatten ya sharpen karta hai. Low (0.0 - 0.2) se deterministic output milta hai, high (0.8+) se randomness badhti hai.
- **Top-P (Nucleus Sampling):** Cumulative probability mass ka cutoff set karta hai (e.g. 0.9 ka matlab sirf top 90% probability wale pool se pick karo).
- **Backend Rule:** Deterministic structured data (JSON, SQL, code) ke liye **`temperature: 0.0`** set karein. Dono ko ek sath tune na karein.

### Q3: Tokens kya hote hain? 1000 words kitne tokens ke barabar hote hain?
**Answer:** Tokens text ke sub-word chunks hote hain jo Byte-Pair Encoding (BPE) algorithm se bante hain. English me standard rule hai: **100 tokens ≈ 75 words**. Toh 1000 English words lagbhag **1333 tokens** hote hain. Non-English ya code me token density zyada hoti hai.

### Q4: Self-Attention mechanism ka time aur space complexity kya hai?
**Answer:** Standard Self-Attention ka complexity **$O(N^2)$** hota hai jahan $N$ sequence length (tokens) hai. Chunki har token sentence ke har doosre token se multiply hota hai, context window double karne se compute aur memory 4 guna badh jati hai. FlashAttention aur KV-caching is overhead ko production me optimize karte hain.

### Q5: KV Caching kya hota hai aur LLM inference me kyun zaroori hai?
**Answer:** Autoregressive generation me har naye token ke liye pichhle tokens ke Key aur Value vectors dubara calculate karna compute waste hai. KV Cache GPU VRAM me purane tokens ke vectors ko cache kar leta hai taaki sirf naye token ka attention compute karna pade. Isse generation speed 5x-10x fast ho jati hai.

### Q6: TTFT (Time To First Token) aur TPS (Tokens Per Second) me kya antar hai?
**Answer:**
- **TTFT:** Prompt bhejne se pehla token stream hone tak ka latency. Yeh prompt processing (prefill phase) par depend karta hai.
- **TPS:** Pehla token aane ke baad model kitni speed se agle tokens spit out kar raha hai (decode phase throughput).

### Q7: Proprietary model (GPT-4o/Claude) kab use karein aur Open-Source (LLaMA/DeepSeek) kab?
**Answer:**
- Proprietary: Zero infra overhead, fast time-to-market, complex reasoning tasks, best-in-class tool use.
- Open-Source: Strict data privacy (banking/healthcare), offline air-gapped VPCs, high-throughput jahan API token cost prohibitive ho jati hai.

### Q8: Context Window limit exceed hone par kya hota hai?
**Answer:** API `400 Bad Request: Context length exceeded` error throw karti hai. Backend me sliding window buffer, summarization, ya conversation pruning implement karke context ko limit ke andar rakha jata hai.

### Q9: Prompt Caching kaise kaam karta hai aur iske financial benefits kya hain?
**Answer:** Anthropic aur OpenAI me agar prompt ka starting prefix (e.g. large system prompt ya RAG documentation) same ho, toh server GPU KV cache reuse karta hai. Isse **50% se 90% token cost save hoti hai** aur latency drastically drop hoti hai.

### Q10: DeepSeek-R1 aur OpenAI o1 jaise Reasoning Models regular LLMs se kaise alag hain?
**Answer:** Regular models direct response generate karte hain. Reasoning models user ko final answer dikhane se pehle internal **Chain-of-Thought (CoT)** tokens generate karte hain jahan woh problem ko verify karte hain, edge cases check karte hain, aur galti hone par backtrack karte hain. Yeh complex math aur coding logic ke liye best hain.

---

## 🛠️ Section 2: Prompt Engineering & Tool Calling (Q11 - Q20)

### Q11: Few-Shot Prompting kya hai aur ye Zero-Shot se behtar kab hoti hai?
**Answer:** Few-shot me hum model ko task ke sath 2-3 input-output examples dete hain. Yeh tab behtar hoti hai jab humein output ka specific formatting, edge-case handling ya custom tone guarantee karni ho bina model ko fine-tune kiye.

### Q12: Function Calling (Tool Calling) me backend aur LLM ke beech ka data flow kya hai?
**Answer:** 
1. Backend LLM ko prompt ke sath Tool Definitions (JSON Schema) bhejta hai.
2. LLM decide karta hai ki tool chahiye ya nahi, aur tool name + parameters JSON me return karta hai.
3. Backend us method ko execute karta hai (DB query, API call).
4. Backend tool execution result ko LLM ko bhejta hai.
5. LLM final answer generate karta hai.

### Q13: Direct aur Indirect Prompt Injection me kya fark hai?
**Answer:**
- Direct: User input me system prompt override instructions daal deta hai ("Ignore previous rules...").
- Indirect: Model kisi external web page, email, ya PDF ko parse karta hai jisme attacker ne hidden malicious instructions inject ki hoti hain.

### Q14: Strict Structured Outputs (`strict: true`) kaise guarantee karte hain ki JSON valid hoga?
**Answer:** Yeh **Grammar-Constrained Sampling** use karte hain. LLM inference engine token generation ke dauran probability distribution ko mask kar deta hai, taaki sirf wahi tokens emit ho sakein jo JSON Schema ke syntax aur types se match karte hain.

### Q15: Hallucination kam karne ke top 3 prompt engineering practices kya hain?
**Answer:**
1. Grounding instruction: *"Answer strictly using the provided context. If unknown, say I don't know."*
2. Chain-of-Thought forcing: *"Explain your reasoning step by step before the final conclusion."*
3. Few-shot boundary examples dikhana.

### Q16: System Prompt, User Prompt aur Assistant Prompt me kya difference hai?
**Answer:**
- System: Model ka behavioral constitution, persona aur security rules set karta hai (Highest authority).
- User: End-user ka question ya command.
- Assistant: LLM ka previous generated response (Chat history maintain karne ke liye).

### Q17: Agar LLM function calling me invalid arguments bhej de toh backend ko kya karna chahiye?
**Answer:** Backend ko crash hone ke bajaye error message ko catch karna chahiye aur LLM ko as a tool response wapas bhejna chahiye: *"Error: Invalid date format. Expected YYYY-MM-DD"*. LLM is error ko padhkar correct arguments ke saath retry karta hai.

### Q18: ReAct framework kya hai?
**Answer:** ReAct stands for **Reasoning + Acting**. Isme model har step par pehle sochta hai (Thought), fir action leta hai (Action/Tool), uska result dekhta hai (Observation), aur fir final conclusion par pahunchta hai.

### Q19: Delimiters (jaise XML tags `<context>...</context>`) prompt me kyun use karte hain?
**Answer:** Delimiters prompt ke instructions aur user-supplied untrusted data ke beech clear boundary banate hain, jisse model confused nahi hota aur prompt injection attacks ka risk drastically reduce ho jata hai.

### Q20: Negative Prompting kya hoti hai?
**Answer:** Model ko explicitly batana ki kya **NAHI** karna hai (e.g. *"Do NOT use technical jargon, do NOT hallucinate facts, do NOT include markdown backticks"*).

---

## 🐘 Section 3: Vector Databases & pgvector (Q21 - Q28)

### Q21: Cosine Similarity, Dot Product aur Euclidean Distance me kya fark hai?
**Answer:**
- Cosine: Vectors ke beech ka angle measure karta hai (Magnitude se independent). Text embeddings ke liye best.
- Dot Product: Angle aur magnitude dono consider karta hai. Agar vectors normalized hain, toh Dot Product = Cosine Similarity.
- Euclidean (L2): Geometric physical distance. Image embeddings ke liye common hai.

### Q22: Dedicated Vector DB (Pinecone/Milvus) ke bajaye PostgreSQL + `pgvector` kyun choose karein?
**Answer:** 
1. Single database for relational data, auth, aur vectors (Zero infra overhead).
2. ACID transactions aur direct SQL `JOIN` support.
3. No dual-write problem (Data relational DB me update hua toh vector bhi transaction me update hota hai).
4. Sub-millisecond latency for datasets up to 20-30 million vectors.

### Q23: HNSW (Hierarchical Navigable Small World) index kaise kaam karta hai?
**Answer:** Yeh multi-layer probabilistic graph network hai (Skip-List concept par based). Top layers me lambe distance jumps hote hain aur bottom layers me dense local connections. Yeh $O(\log N)$ search time me >98% recall provide karta hai.

### Q24: IVFFlat aur HNSW me kya trade-off hai?
**Answer:**
- IVFFlat: K-Means clustering use karta hai. Indexing bohot fast hoti hai aur memory kam lagti hai, lekin search latency aur recall kam hoti hai.
- HNSW: Memory zyada leta hai aur index banne me time lagta hai, lekin search ultra-fast (<5ms) aur recall highest hoti hai. Production me HNSW preferred hai.

### Q25: Vector Quantization (SQ8 vs PQ) kya hai?
**Answer:** Float32 vectors bohot RAM consume karte hain. Scalar Quantization (SQ8) 32-bit floats ko 8-bit integers me convert karti hai jisse 75% RAM bachti hai with minimal accuracy loss. Product Quantization (PQ) vectors ko clusters me todti hai for billion-scale search.

### Q26: pgvector me Cosine Distance operator kya hai?
**Answer:** Operator `<=>` hai. Cosine Similarity calculate karne ke liye formula hota hai: `1 - (embedding <=> query_vector)`.

### Q27: Hybrid Search kya hai aur kyun zaroori hai?
**Answer:** Pure vector search exact keyword match (jaise SKU codes, error codes, product numbers) me weak hoti hai. Hybrid search **Vector Semantic Search + Full-Text Search (BM25)** ko combine karke **Reciprocal Rank Fusion (RRF)** se results rank karti hai for 100% precision.

### Q28: Vector DB me multi-tenancy kaise implement karte hain?
**Answer:** Metadata Pre-Filtering use karke. Query me vector distance calculate karne ke saath-saath SQL filter lagaya jata hai: `WHERE tenant_id = :tenantId`.

---

## 📚 Section 4: RAG Architectures & Advanced Retrieval (Q29 - Q36)

### Q29: Naive RAG ke top 3 failure modes kya hain?
**Answer:**
1. Retrieval failures: Vague user queries ki wajah se irrelevant chunks fetch hona.
2. "Lost in the middle": LLM lambe context ke beech me rakhe facts ko ignore kar deta hai.
3. Out of date context ya bad chunk boundaries ki wajah se context loss.

### Q30: Chunking me Overlap kyun zaroori hota hai?
**Answer:** Agar text ko bina overlap ke split karein, toh critical sentences ya context chunk boundary par aadhi-adhoori kat sakti hain. 10-15% overlap sentences ki continuous continuity maintain karta hai.

### Q31: Two-Stage Retrieval (Cross-Encoder Re-ranking) kya hai?
**Answer:**
- Stage 1: Vector DB se lightweight Bi-Encoder ke zariye quickly Top-25 candidates nikalna.
- Stage 2: Ek powerful Cross-Encoder (jaise Cohere Rerank) se (Query + Chunk) ko deep attention dekar Top-5 highest relevance chunks filter karna.

### Q32: HyDE (Hypothetical Document Embeddings) kaise kaam karta hai?
**Answer:** User ki query ka direct embedding nikalne ke bajaye LLM se pehle ek draft hypothetical answer generate karwate hain. Fir us answer ka embedding vector banakar database me match karte hain. Document-to-Document search accuracy zyada hoti hai.

### Q33: RAGAS Framework ke 4 core metrics kya hain?
**Answer:**
1. **Faithfulness:** Answer context par kitna grounded hai (Hallucination check).
2. **Answer Relevance:** Answer user question ka direct jawab deta hai ya nahi.
3. **Context Precision:** Retrieved chunks me signal vs noise ka ratio.
4. **Context Recall:** Kya context me saari zaroori information mili.

### Q34: Parent Document Retrieval pattern kya hai?
**Answer:** Small chunks (100 tokens) par vector search perform hoti hai for accurate embedding matching, lekin LLM ko bhejte waqt uska pura Parent container chunk (1000 tokens) pass kiya jata hai taaki full context mile.

### Q35: Multi-Query Expansion kya hoti hai?
**Answer:** LLM single user query ko 3-4 alag-alag perspectives me rewrite karta hai. Sabhi variants ke vectors run hote hain aur results merge karke retrieval recall badhai jati hai.

### Q36: Semantic Chunking Fixed-size Chunking se behtar kyun hai?
**Answer:** Fixed-size chunking arbitrary token count par split karti hai. Semantic chunking consecutive sentences ke embedding distance par monitor karti hai — jaise hi naya topic start hota hai, wahi chunk split hota hai.

---

## ☕ Section 5: Spring AI, LangChain4j & Java Ecosystem (Q37 - Q42)

### Q37: Enterprise me Python ke bajaye Java Spring Boot for AI kyun use karein?
**Answer:** Type safety, high-throughput concurrency via Java 21 Virtual Threads, existing enterprise microservice ecosystem (Postgres, Kafka, Spring Security), and zero need to maintain a separate Python runtime microservice.

### Q38: Spring AI me `ChatClient` aur `ChatModel` me kya difference hai?
**Answer:**
- `ChatModel`: Low-level interface jo specific provider (OpenAI, Ollama, Bedrock) ke REST APIs se deal karta hai.
- `ChatClient`: Fluent, builder-based high-level API jo prompt templates, advisors, memory aur tool calling ko cleanly chain karta hai.

### Q39: Spring AI me Advisors ka kya role hai?
**Answer:** Advisors AOP interceptors ki tarah kaam karte hain. Yeh har LLM request aur response ko intercept karke dynamically Chat Memory attach karte hain (`MessageChatMemoryAdvisor`) ya RAG retrieval context inject karte hain (`QuestionAnswerAdvisor`).

### Q40: Spring Boot se LLM streaming response frontend ko kaise bhejte hain?
**Answer:** Server-Sent Events (SSE) use karke. Controller endpoint `MediaType.TEXT_EVENT_STREAM_VALUE` produce karta hai aur `Flux<String>` return karta hai, jisse tokens real-time me client screen par stream hote hain.

### Q41: LangChain4j ka `@AiService` annotation kya karta hai?
**Answer:** Yeh Spring Data repository jaisa declarative interface hai. Developer sirf interface method define karta hai annotations (`@SystemMessage`, `@UserMessage`) ke saath, aur runtime par dynamic proxy LLM call aur POJO serialization automatically handle kar leti hai.

### Q42: External AI APIs par Circuit Breaker (Resilience4j) lagana kyun mandatory hai?
**Answer:** External LLM APIs me rate limits (429), timeouts aur 5xx errors frequent hote hain. Circuit Breaker un failures ko isolate karta hai aur request ko fallback model (jaise local Ollama instance) par redirect kar deta hai taaki backend application hang na ho.

---

## 🤖 Section 6: AI Agents, Workflows & MCP (Q43 - Q46)

### Q43: AI Agent aur Simple RAG Chatbot me fundamental difference kya hai?
**Answer:** RAG Chatbot ek single-turn request-response pattern hai (Retrieve $\rightarrow$ Generate). AI Agent ek autonomous multi-step loop me operate karta hai jahan woh khud sub-goals plan karta hai, environment me tools execute karta hai, observations check karta hai aur goal achieve hone tak loop continue karta hai.

### Q44: Model Context Protocol (MCP) kya hai aur Anthropic ne ise kyun introduce kiya?
**Answer:** MCP ek open-standard JSON-RPC 2.0 protocol hai jo AI models aur data sources/tools ke beech standard communication define karta hai. Yeh $M \times N$ custom integrations ki problem ko solve karke universal plug-and-play architecture banata hai.

### Q45: Human-in-the-Loop (HITL) pattern backend me kyun critical hai?
**Answer:** High-risk actions (paisa deduct karna, database records delete karna, emails send karna) autonomous agent ko direct execute karne nahi di ja sakti. HITL interceptor agent execution ko pause karta hai aur human admin ke approval ke baad hi action trigger karta hai.

### Q46: LangGraph me "State" aur "Checkpointing" ka kya role hai?
**Answer:** LangGraph agents ko state machines ke roop me treat karta hai. Har node execution ke baad complete state database me persist (checkpoint) hoti hai, jisse long-running agent workflows fail hone par wahi se resume ho sakte hain aur time-travel debugging possible hoti hai.

---

## 🏛️ Section 7: System Design & Production Scale (Q47 - Q50)

### Q47: Semantic Caching kya hai aur ise Redis me kaise implement karte hain?
**Answer:** Semantic Caching incoming query ka embedding banakar Redis Vector Similarity Search me check karta hai. Agar similarity > 0.95 hai, toh LLM ko call kiye bina cached response 3ms me return ho jata hai, saving 100% cost and latency.

### Q48: LLM Gateway ke core components kya hote hain?
**Answer:**
1. Smart Routing (Cost/Latency/Capability ke basis par model pick karna).
2. Rate Limiting (Token Bucket per user).
3. PII Masking & Guardrails.
4. Fallback & Load Balancing across providers.
5. Centralized Cost & Token Tracking.

### Q49: PII (Personally Identifiable Information) masking kaise implement karte hain?
**Answer:** Input pipeline me NER (Named Entity Recognition) models ya regex patterns se sensitive data (Cards, Phones, SSN, Emails) ko anonymize token (`<PHONE_1>`) me replace kiya jata hai. LLM generation ke baad de-anonymizer original values restore karta hai.

### Q50: vLLM me PagedAttention GPU memory kaise optimize karta hai?
**Answer:** Traditional systems continuous memory blocks allocate karte hain jisse internal/external memory fragmentation 60-80% tak ho jati hai. PagedAttention OS virtual memory paging ki tarah non-contiguous physical pages me KV cache store karta hai, bringing memory waste down to <4% and enabling 10x higher batch concurrency.

