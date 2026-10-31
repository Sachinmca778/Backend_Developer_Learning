# Module 07: Local LLMs, Ollama & Production Serving with vLLM 🖥️⚡

Cloud LLM APIs (OpenAI, Claude) fast aur easy hain, lekin enterprise backend systems me 3 badi challenges aati hain:
1. **Data Privacy & Compliance (GDPR, HIPAA, BFSI):** Customer ka sensitive PII data third-party servers par nahi bheja ja sakta.
2. **Skyrocketing Token Bills:** High-throughput systems (Millions of requests/day) me cloud APIs ka monthly bill lakho rupees ho jata hai.
3. **Vendor Lock-in & Downtime:** Agar OpenAI down hua, toh aapka pura backend stall ho jata hai.

**Solution:** Open-weight models (LLaMA 3, DeepSeek, Mistral) ko apne local laptop ya private AWS/GCP GPU servers par host karna!

---

## 1. Development vs Production Serving Engines

```
+─────────────────────────────────────────────────────────────+
|               LOCAL / PRIVATE LLM SERVING                   |
+──────────────────────────────+──────────────────────────────+
|         OLLAMA               |            vLLM              |
|  (Developer Laptop / Dev-Env)|   (Enterprise Production / K8s)|
+──────────────────────────────+──────────────────────────────+
| - Single binary installation.| - High-throughput GPU engine |
| - Runs on Mac (Metal) & CPU. |   built for Linux + NVIDIA.  |
| - Great for local testing,   | - **PagedAttention** &       |
|   offline dev, and CI/CD.    |   **Continuous Batching**.   |
| - Low concurrency (Single or | - 100+ concurrent requests   |
|   few users).                |   serve kar sakta hai.       |
| - Built on `llama.cpp`.      | - OpenAI-compatible REST API.|
+──────────────────────────────+──────────────────────────────+
```

---

## 2. Ollama Deep Dive: Installation & CLI

Ollama local models ko run karna Docker container run karne jitna simple bana deta hai:

```bash
# 1. Pull and run LLaMA 3.3 (8 Billion parameters)
ollama run llama3.3:8b

# 2. Pull DeepSeek-R1 reasoning model
ollama run deepseek-r1:8b

# 3. Pull fast text embedding model
ollama pull nomic-embed-text
```

### Ollama REST API:
Ollama background me `11434` port par REST server chalata hai:
```bash
curl http://localhost:11434/api/chat -d '{
  "model": "llama3.3:8b",
  "messages": [
    { "role": "user", "content": "Explain Virtual Threads in Java in 2 sentences" }
  ],
  "stream": false
}'
```

---

## 3. Connecting Spring Boot to Local Ollama

Spring AI me OpenAI se Ollama par switch karne ke liye sirf dependency aur 2 lines config badalni hoti hai!

### `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
</dependency>
```

### `application.yml`:
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: llama3.3:8b
        options:
          temperature: 0.2
      embedding:
        model: nomic-embed-text
```

Aapka Java code (`ChatClient`, `VectorStore`, RAG pipeline) bilkul same rahega, lekin ab saari computation aapke local GPU / CPU par chalegi — **Zero Internet, Zero Bills!**

---

## 4. Production Serving with vLLM: The Gold Standard

Agar aapko Kubernetes cluster par 1000 requests/second handle karni hain, toh Ollama nahi chalega. Wahan **vLLM** use hota hai.

```
[ 100 Concurrent Requests ] ──> [ Nginx Ingress ] ──> [ vLLM Pod (NVIDIA A100 GPU) ]
                                                              │
                                            ┌─────────────────┴─────────────────┐
                                            ▼                                   ▼
                                    [ PagedAttention ]                [ Continuous Batching ]
                                (Zero Memory Fragmentation)       (Dynamically injects new requests
                                                                   without waiting for batch to finish)
```

### The 2 Superpowers of vLLM:
1. **PagedAttention:** Operating System ke virtual memory paging algorithm se inspired. GPU VRAM me KV-cache ko continuous blocks me store karne ke bajaye "pages" me store karta hai. Isse VRAM waste 60-80% se drop hokar <4% ho jati hai!
2. **Continuous Batching:** Traditional batching me agar 10 requests hain, toh jab tak sabse lamba answer poora nahi hota, agla batch wait karta hai. vLLM jaise hi koi request khatam hoti hai, agle hi millisecond nayi request ko GPU batch me inject kar deta hai!

### Running vLLM via Docker in Production:
```bash
docker run --gpus all \
  -v ~/.cache/huggingface:/root/.cache/huggingface \
  -p 8000:8000 \
  --ipc=host \
  vllm/vllm-openai:latest \
  --model meta-llama/Llama-3.1-8B-Instruct \
  --max-model-len 8192 \
  --gpu-memory-utilization 0.90
```

Yeh container `http://<server-ip>:8000/v1` par bilkul OpenAI jaisa endpoint expose karta hai! Aap Spring Boot me `spring.ai.openai.base-url: http://<server-ip>:8000` set karke use kar sakte hain!

---

## 5. Summary Checklist
- [x] Use Ollama for local dev, offline prototyping, and unit tests.
- [x] Spring AI provides seamless drop-in support for Ollama.
- [x] For enterprise production scale, deploy vLLM on Kubernetes with NVIDIA GPUs.
- [x] PagedAttention and Continuous Batching provide 10x-20x throughput boost.

