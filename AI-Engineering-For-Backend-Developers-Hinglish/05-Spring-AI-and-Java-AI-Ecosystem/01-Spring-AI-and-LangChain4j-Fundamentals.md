# Module 05: Spring AI & LangChain4j Fundamentals ☕🤖

Bohot se Java developers ko galatfehmi hoti hai ki AI apps banane ke liye Python (LangChain / LlamaIndex) sikhna padega aur pure backend ko rewrite karna padega. **Yeh bilkul galat hai! Java enterprise world ka king hai, aur Spring AI & LangChain4j ne Java ko Generative AI development ke liye first-class citizen bana diya hai.**

---

## 1. Why Java & Spring Boot for Enterprise AI?

Python experimentation aur Jupyter notebooks ke liye achha hai, lekin **Production Enterprise AI systems** me:
1. **Type Safety & Maintainability:** Large codebases me static typing runtime errors rokti hai.
2. **Concurrency & Throughput:** Java 21 ke **Virtual Threads (Project Loom)** hazaron concurrent LLM HTTP streaming calls ko ultra-low RAM footprint ke saath handle karte hain.
3. **Enterprise Integration:** Existing PostgreSQL databases, Kafka streams, Spring Security OAuth2, Redis caches aur Kubernetes pods ke saath direct seamless integration.
4. **Unified Stack:** Python ka alag microservice deploy karne ka overhead aur operational cost bach jaata hai!

---

## 2. Spring AI vs LangChain4j Comparison

Java ecosystem me do major frameworks lead kar rahe hain:

```
+─────────────────────────────────────────────────────────────+
|                     JAVA AI FRAMEWORKS                      |
+──────────────────────────────+──────────────────────────────+
|          SPRING AI           |         LANGCHAIN4J          |
|    (Official Pivotal/VMware) |   (Community Driven SOTA)    |
+──────────────────────────────+──────────────────────────────+
| - Pure Spring idiomatic style| - Inspired by Python         |
|   (`@Autowired`, auto-config,|   LangChain, lekin Java-idiom|
|   application.yml).          |   me clean rewrite.          |
| - `ChatClient` fluent API    | - `@AiService` declarative   |
|   (Similar to `RestClient`). |   interface magic.           |
| - Strong integration with    | - Super mature, fast release |
|   Spring Cloud & Metrics.    |   cycle, 20+ Vector DBs.     |
| - Best for existing Spring   | - Best for standalone Java   |
|   Boot 3.x microservices.    |   or Quarkus/Micronaut apps. |
+──────────────────────────────+──────────────────────────────+
```

---

## 3. Spring AI Architecture & Core Abstractions

Spring AI spring-like clean abstractions provide karta hai taaki aapka code kisi single vendor (OpenAI, AWS Bedrock, Google Vertex, Ollama) ke saath tightly couple na ho:

```
                          [ ChatClient (Fluent API) ]
                                      │
                     [ Advisor API (Memory, RAG, Logging) ]
                                      │
                                      ▼
                             [ ChatModel (Interface) ]
                                      │
            ┌─────────────────────────┼─────────────────────────┐
            ▼                         ▼                         ▼
   [ OpenAiChatModel ]       [ OllamaChatModel ]       [ BedrockChatModel ]
```

### Core Abstractions:
1. **`ChatModel`:** Low-level contract jo AI provider se baat karta hai. Request ko format karta hai aur response parse karta hai.
2. **`ChatClient`:** High-level fluent builder API (Spring 3.x ke `RestClient` jaisa). Ye prompt templating, tool calling aur advisors ko seamlessly chain karta hai.
3. **`VectorStore`:** Abstraction for vector databases (`PgVectorStore`, `MilvusVectorStore`, `QdrantVectorStore`).
4. **`Advisor`:** AOP (Aspect-Oriented Programming) jaisa interceptor. Har prompt request aur response ke beech me chat memory jodna, ya RAG context inject karna.

---

## 4. LangChain4j `@AiService` Declarative Magic

LangChain4j ka sabse kamaal ka feature `@AiService` hai. Jaise Spring Data me aap sirf interface banate hain (`interface UserRepository extends JpaRepository`), LangChain4j me aap sirf AI Interface declare karte hain:

```java
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface SentimentAnalysisService {

    @SystemMessage("You are an expert sentiment analyzer. Respond strictly with POSITIVE, NEGATIVE, or NEUTRAL.")
    String analyze(@UserMessage String reviewText);

    @SystemMessage("Extract user details from text.")
    @UserMessage("Input: {{text}}")
    UserDetails extract(@V("text") String text); // Automatically deserializes into Java POJO!
}

public record UserDetails(String name, String email, String phone) {}
```

LangChain4j runtime par dynamic proxy create karta hai aur LLM call + JSON parsing behind the scenes handle kar leta hai!

---

## 5. Streaming in Spring AI: Server-Sent Events (SSE)

User experience ke liye tokens ko real-time me stream karna padta hai. Spring WebFlux ya Spring MVC me Virtual Threads use karke reactive SSE return kiya jaata hai:

```java
package com.backend.ai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatStreamingController {

    private final ChatClient chatClient;

    public ChatStreamingController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestParam("message") String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .stream()
                .content(); // Emits tokens as SSE text stream to browser / mobile app
    }
}
```

---

## 6. Key Takeaways
- Java 21 + Spring Boot is a tier-1 stack for production AI systems.
- Spring AI provides `ChatClient`, `ChatModel`, and `Advisor` abstractions.
- LangChain4j offers `@AiService` for interface-driven clean code.
- Always use Reactive Streams (`Flux<String>`) / SSE for user-facing chat endpoints.

