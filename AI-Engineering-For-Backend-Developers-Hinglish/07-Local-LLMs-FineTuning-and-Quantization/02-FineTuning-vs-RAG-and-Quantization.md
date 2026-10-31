# Module 07: Fine-Tuning vs RAG & Model Quantization ⚖️🔬

AI projects me sabse common aur costly galti hoti hai: **Bina soche Fine-Tuning shuru kar dena!** Bohot si teams lakhon rupaye GPU training me barbad kar deti hain sirf yeh realise karne ke liye ki unka problem 10 lines ke RAG prompt se solve ho sakta tha.

---

## 1. The Decision Matrix: Prompting vs RAG vs Fine-Tuning

```
                ┌──────────────────────────────────────────────┐
                │        DO YOU NEED EXTERNAL / NEW            │
                │              FACTUAL DATA?                   │
                └──────────────────────┬───────────────────────┘
                                       │
                     ┌─────────────────┴─────────────────┐
                     ▼ YES                               ▼ NO
       ┌───────────────────────────────┐   ┌───────────────────────────────┐
       │   DO YOU NEED TO RETRIEVE     │   │     DOES BASE MODEL FAIL      │
       │   SPECIFIC ACCURATE FACTS,    │   │     AT DESIRED TONE, STYLE,   │
       │   POLICIES, OR PRIVATE DOCS?  │   │     OR STRICT CUSTOM SYNTAX?  │
       └──────────────┬────────────────┘   └───────────────┬───────────────┘
                      │                                    │
           ┌──────────┴──────────┐              ┌──────────┴──────────┐
           ▼ YES                 ▼ NO           ▼ YES                 ▼ NO
    ╔═════════════╗        ╔═════════════╗╔═════════════╗       ╔═════════════╗
    ║   USE RAG   ║        ║ PROMPT ENG  ║║ FINE-TUNING ║       ║ PROMPT ENG  ║
    ║ (pgvector)  ║        ║ (Few-Shot)  ║║ (LoRA/QLoRA)║       ║ (Zero-Shot) ║
    ╚═════════════╝        ╚═════════════╝╚═════════════╝       ╚═════════════╝
```

### Clear Comparison Table:
| Dimension | Prompt Engineering | RAG (Retrieval-Augmented) | Fine-Tuning (LoRA) |
| :--- | :--- | :--- | :--- |
| **Primary Goal** | Direct model guidance | Add factual private domain knowledge | Change model's style, tone, format, or behavior |
| **New Knowledge?** | No (Limited to context) | **Yes (Dynamic & Real-time)** | Weak (Risk of hallucinations & catastrophic forgetting) |
| **Hallucination Risk** | Medium | **Lowest (Grounded in context)**| High (Memorization issues) |
| **Implementation Effort**| Hours | Days | Weeks |
| **Cost** | Low (API token cost) | Low to Medium | High (GPU compute + clean dataset prep) |
| **Updates / Freshness**| Instant | **Instant** (Bas DB me naya chunk insert karo) | Slow (Pura training loop dobara run karna padega) |

> [!CAUTION]
> **Golden Rule of AI Engineering:**
> - New Knowledge / Facts sikhane ke liye $\rightarrow$ **USE RAG!**
> - New Style, Grammar, ya Output Structure sikhane ke liye $\rightarrow$ **USE FINE-TUNING!**

---

## 2. Fine-Tuning: PEFT, LoRA & QLoRA Explained

Model ko train karte waqt sabhi 70 Billion parameters ko update karna (Full Fine-Tuning) bohot mehnga hai ($100,000+). Isliye modern AI me **PEFT (Parameter-Efficient Fine-Tuning)** use hota hai.

### LoRA (Low-Rank Adaptation)
LoRA original model ke pre-trained weights ko **freeze (lock)** kar deta hai. Woh sirf do chhoote low-rank matrices ($A$ aur $B$) ko train karta hai:

```
[ Pre-trained Frozen Weights (W) ]  +  [ LoRA Adapter (A × B) ]
        (70 Billion Params)                     (Only ~10 Million Params!)
               [ LOCKED ]                              [ TRAINED ]
```

- **Benefit:** Sirf 0.1% parameters train hote hain! Training GPU cost 90% drop ho jati hai.
- **Backend Benefit:** Aap ek hi base model par din me Marketing LoRA adapter switch kar sakte hain, aur raat me Coding LoRA adapter swap kar sakte hain!

### QLoRA (Quantized LoRA)
Base model ko 4-bit precision me compress karke LoRA train karna. Isse 70B parameter ka model ek single consumer GPU (NVIDIA RTX 4090 ya A10G) par fine-tune ho jata hai!

---

## 3. Model Quantization: Squeezing Models into RAM

Neural networks me har parameter (weight) by default ek **32-bit floating point number (FP32)** hota hai.
Quantization weights ki mathematical precision ko kam karke memory aur compute bachaane ki technique hai:

```
FP32 (4 Bytes)  ──────> [ 0.1294817293810... ]  (Full Precision - Overkill)
FP16 (2 Bytes)  ──────> [ 0.12948 ]              (Industry Standard Training)
INT8 (1 Byte)   ──────> [ 33 ]                   (50% Memory Save, < 0.5% Loss)
INT4 (0.5 Byte) ──────> [ 4 ]                    (75% Memory Save, Great for edge/laptops)
```

### Quantization Formats:
1. **GGUF (GPT-Generated Unified Format):**
   - Designed for `llama.cpp` and Ollama.
   - CPU aur Apple Silicon (M1/M2/M3/M4 unified memory) par super fast inference deta hai.
2. **AWQ (Activation-aware Weight Quantization):**
   - Production Linux GPUs ke liye SOTA 4-bit format (vLLM me natively supported).
3. **GPTQ:**
   - Popular 4-bit GPU format.

---

## 4. GPU VRAM Sizing Formula for Backend Engineers

Production server order karte waqt GPU memory (VRAM) calculate karne ka exact formula:

$$\text{Required VRAM (GB)} = \left( \frac{\text{Model Parameters in Billions} \times \text{Bits per weight}}{8} \right) \times 1.25$$

*(1.25 factor KV Cache aur activation memory overhead ko cover karta hai).*

### Quick Cheat Sheet:
| Model Size | Precision | Format | Minimum VRAM Needed | Target Hardware |
| :--- | :--- | :--- | :--- | :--- |
| **8B (LLaMA 3.3)** | 4-bit (INT4) | GGUF / AWQ | **~6 GB** | MacBook M-series, RTX 3060, T4 GPU |
| **8B (LLaMA 3.3)** | 16-bit (FP16) | HuggingFace | **~20 GB** | NVIDIA A10G / RTX 4090 |
| **14B (Qwen 2.5)** | 4-bit (INT4) | GGUF / AWQ | **~10 GB** | MacBook 16GB, RTX 4070 |
| **70B (LLaMA 3.3)**| 4-bit (INT4) | AWQ | **~42 GB** | 1x NVIDIA A100 (80GB) or 2x A10G |
| **70B (LLaMA 3.3)**| 16-bit (FP16) | HuggingFace | **~160 GB** | 2x A100 (80GB) or 4x A10G |

---

## 5. Summary Checklist
- [x] Use RAG for real-time facts and private knowledge bases.
- [x] Use Fine-Tuning (LoRA) only for specialized domain format, tone, or custom grammar.
- [x] Never fine-tune to teach dynamic facts.
- [x] Quantization (INT4/INT8) reduces RAM usage by 75% with negligible quality loss.
- [x] Use GGUF for local CPU/Mac and AWQ for vLLM production GPUs.

