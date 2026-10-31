# Module 01: Model Ecosystem & Backend API Integration 🌐

Backend systems me LLM integrate karne ke liye sabse pehle samajhna zaroori hai ki market me kaunse models available hain, kab kaun sa model choose karein, aur Spring Boot / Java backend se unhe reliable production-ready tarike se kaise call karein.

---

## 1. Proprietary vs Open-Weight Models

AI world do bade camps me divided hai:

```
+---------------------------------------------------------------+
|                       THE LLM LANDSCAPE                       |
+-------------------------------+-------------------------------+
|     PROPRIETARY (Closed)      |     OPEN-WEIGHT / OPEN SOURCE |
| (API-only, hosted by vendor)  | (Download weights, self-host) |
+-------------------------------+-------------------------------+
| - OpenAI: GPT-4o, o1, o3-mini | - Meta: LLaMA 3.1, LLaMA 3.3  |
| - Anthropic: Claude 3.5 Sonnet| - DeepSeek: V3, R1 (Reasoning)|
| - Google: Gemini 1.5/2.0 Flash| - Mistral AI: Mistral, Mixtral|
| - Microsoft: Copilot / Azure  | - Qwen (Alibaba): Qwen 2.5    |
+-------------------------------+-------------------------------+
```

### Head-to-Head Comparison:
| Parameter | Proprietary (OpenAI / Claude / Gemini) | Open-Weight (LLaMA / DeepSeek / Mistral) |
| :--- | :--- | :--- |
| **Hosting & Infra** | Zero maintenance. Vendor manages GPUs. | GPUs humein rent karni padti hain (AWS EC2 G5, RunPod) ya Ollama/vLLM par. |
| **Data Privacy** | Sensitive customer data third-party server par jaata hai (Enterprise DPA required). | **100% On-Premise / Private VPC**. Data kabhi server se bahar nahi jaata. |
| **Cost Dynamics** | Pay-as-you-go per million tokens. Zero upfront cost. | Fixed GPU cost chahe traffic ho ya na ho. High volume par bohot sasta padta hai. |
| **Customization** | System prompts, Function Calling, Fine-tuning limited. | Full model control, custom LoRA weights, quantization, offline deployment. |

---

## 2. Model Selection Matrix: Kaun Sa Model Kab Use Karein?

Backend architectural decisions me right tool choose karna sabse critical skill hai:

| Use Case | Recommended Model | Reason |
| :--- | :--- | :--- |
| **Complex Logic, Agentic Loops, Coding** | **Claude 3.5 Sonnet / GPT-4o** | Best-in-class tool use, coding precision, structured JSON output adherence. |
| **High-Throughput, Low-Latency APIs** | **Gemini 2.0 Flash / GPT-4o-mini** | Ultra-fast (sub-second TTFT), insanely cheap ($0.075 per 1M tokens), 1M token context. |
| **Deep Reasoning & Math/Logic Verification** | **DeepSeek-R1 / OpenAI o1, o3-mini** | Chain-of-Thought (CoT) internal reflection, complex edge-case algorithm solving. |
| **Private Healthcare / Banking / Offline VPC** | **LLaMA 3.3 70B (via vLLM) / Ollama** | Zero compliance risks, enterprise privacy, open-source freedom. |

---

## 3. Chat Completions API Anatomy

Lagbhag har LLM provider (OpenAI, Gemini, Anthropic, Ollama, vLLM) ek standard REST API specification follow karta hai. 

### Core Roles in API Payload:
1. `system`: Model ko persona, rules, boundaries aur instructions deta hai. (Highest priority)
2. `user`: End-user ka actual question ya prompt.
3. `assistant`: Model ka previous reply (Conversation history maintain karne ke liye).
4. `tool` / `function`: Tool execution ka result jo backend model ko wapas bhejta hai.

### Standard Request Payload:
```json
{
  "model": "gpt-4o",
  "temperature": 0.2,
  "max_tokens": 500,
  "messages": [
    {
      "role": "system",
      "content": "You are a senior Java architect. Reply strictly in concise technical points."
    },
    {
      "role": "user",
      "content": "Why should I prefer Virtual Threads over Reactive WebFlux in Java 21?"
    }
  ]
}
```

---

## 4. Streaming vs Non-Streaming: SSE (Server-Sent Events)

### Non-Streaming Mode:
- Client request bhejta hai.
- Model pura answer sochta hai (5-15 seconds lag sakte hain).
- Client ko blank screen / spinner dikhta hai. Bad UX for chat applications!

### Streaming Mode (`"stream": true`):
- Model jaise hi ek-ek token generate karta hai, server use **HTTP Server-Sent Events (SSE)** chunk me stream kar deta hai.
- Client ko pehle token 200-400ms me dikhne lagte hain (ChatGPT jaisa typewriter effect).

```
[ Client ] ── POST /chat (stream: true) ──> [ Spring Boot API ] ──> [ LLM Provider ]
                                                    │                      │
[ Client ] <── data: {"content": "Java"} ───────────┴<── chunk 1 ──────────┤
[ Client ] <── data: {"content": " 21"} ────────────┴<── chunk 2 ──────────┤
[ Client ] <── data: {"content": " introduces"} ────┴<── chunk 3 ──────────┤
[ Client ] <── data: [DONE] ────────────────────────┴<── finish ───────────┘
```

---

## 5. Production Backend Integration: Spring Boot Implementation

Backend se LLM API call karte waqt direct raw HTTP call nahi karte. Production grade integration me:
1. **Timeouts** (Connect timeout, Read timeout)
2. **Exponential Backoff & Jitter** (429 Rate Limit handling)
3. **Resilience & Fallback**

### Clean Spring Boot 3 `RestClient` Example:

```java
package com.backend.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiClientService {

    private final RestClient restClient;

    public OpenAiClientService(
            @Value("${ai.openai.api-key}") String apiKey,
            @Value("${ai.openai.base-url:https://api.openai.com/v1}") String baseUrl) {
        
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String generateCompletion(String systemPrompt, String userMessage) {
        var payload = Map.of(
            "model", "gpt-4o-mini",
            "temperature", 0.1,
            "max_tokens", 800,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
            )
        );

        return restClient.post()
                .uri("/chat/completions")
                .body(payload)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                    if (resp.getStatusCode().value() == 429) {
                        throw new RuntimeException("Rate Limit Exceeded (429)! Trigger Exponential Backoff.");
                    }
                    throw new RuntimeException("Client Error: " + resp.getStatusCode());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                    throw new RuntimeException("LLM Provider 5xx Downtime. Trigger Circuit Breaker Fallback.");
                })
                .body(String.class);
    }
}
```

---

## 6. Rate Limits (429) & Exponential Backoff Strategy

AI APIs me standard databases se alag 2 types ke rate limits hote hain:
- **RPM (Requests Per Minute):** Aap 1 minute me kitni API requests bhej sakte hain (e.g. 500 RPM).
- **TPM (Tokens Per Minute):** Aap 1 minute me kitne tokens (Input + Output) consume kar sakte hain (e.g. 100,000 TPM).

### Exponential Backoff with Full Jitter Formula:
Jab 429 error aaye, backend ko turant retry nahi karna chahiye:
$$\text{Sleep Time} = \text{Random}(0, \min(M, B \times 2^{\text{attempt}}))$$
Jahan:
- $B = \text{Base Delay (e.g. 1 second)}$
- $M = \text{Max Delay (e.g. 30 seconds)}$
- $\text{Full Jitter}$ sabhi parallel backend pods ko synchronised thundering herd hone se rokta hai!

---

## 7. Key Takeaways for Backend Developers

- Small/Cheap models (`gpt-4o-mini`, `gemini-2.0-flash`) 90% common tasks ke liye enough hain.
- Always implement timeout handling (LLM calls can hang for 30+ seconds).
- Conversational chat me hamesha SSE (`stream: true`) use karein responsive UX ke liye.
- Private / sensitive data ke liye open-weight models (LLaMA/DeepSeek via Ollama/vLLM) architecture evaluate karein.

