# Module 05: Production Spring Boot 3 AI Implementation Guide 💻

Is guide me hum ek complete, enterprise-grade **Spring Boot 3 + Spring AI + pgvector RAG application** ka architecture aur production-ready code dekhenge, jisme Tool Calling, Chat Memory, aur Resilience4j Circuit Breaker Fallback shamil hain.

---

## 1. Enterprise Project Architecture

```
com.backend.ai
├── config
│   ├── AiConfig.java                # ChatClient, Advisors & VectorStore Beans
│   └── VectorStoreConfig.java       # pgvector connection & schema config
├── controller
│   ├── AiChatController.java        # REST & SSE Streaming Endpoints
│   └── DocumentIngestionController.java # PDF/Doc upload endpoint
├── service
│   ├── DocumentIngestionService.java# Chunking & Vector Ingestion
│   └── ResilientAiService.java      # Circuit Breaker & Fallback to Ollama
└── tools
    └── EnterpriseTools.java         # @Tool methods (Order status, Stock)
```

---

## 2. `pom.xml` Dependencies (Spring Boot 3.3+ & Spring AI)

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <!-- Spring AI OpenAI Starter -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    </dependency>

    <!-- Spring AI PgVector Store Starter -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
    </dependency>

    <!-- PostgreSQL Driver -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Resilience4j Circuit Breaker -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-spring-boot3</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.0.0-M3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## 3. `application.yml` Production Configuration

```yaml
spring:
  application:
    name: enterprise-ai-backend

  datasource:
    url: jdbc:postgresql://localhost:5432/enterprise_db
    username: postgres
    password: strongpassword

  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.1
      embedding:
        options:
          model: text-embedding-3-small
    vectorstore:
      pgvector:
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 1536
        table-name: enterprise_vector_store
        schema-name: public
        initialize-schema: true

# Resilience4j Circuit Breaker Config
resilience4j:
  circuitbreaker:
    instances:
      openaiService:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 15000ms
        permittedNumberOfCallsInHalfOpenState: 3
```

---

## 4. Configuration Beans (`AiConfig.java`)

```java
package com.backend.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient ragChatClient(
            ChatClient.Builder builder, 
            VectorStore vectorStore) {
        
        return builder
                .defaultSystem("Aap ek enterprise customer assistant hain. " +
                               "Answer strictly using the provided context. " +
                               "Agar jawab context me nahi hai, toh politely mana kar dein.")
                // Conversational Memory Advisor
                .defaultAdvisors(
                    new MessageChatMemoryAdvisor(new InMemoryChatMemory()),
                    new QuestionAnswerAdvisor(vectorStore) // Automatic RAG retrieval!
                )
                .build();
    }
}
```

---

## 5. Document Ingestion Service (`DocumentIngestionService.java`)

Yeh service PDFs aur text documents ko chunk karke pgvector me store karti hai:

```java
package com.backend.ai.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DocumentIngestionService {

    private final VectorStore vectorStore;

    public DocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void ingestDocument(String rawText, String docTitle, String department) {
        // 1. Create Document with Metadata
        Document rawDoc = new Document(rawText, Map.of(
            "title", docTitle,
            "department", department,
            "uploadedAt", System.currentTimeMillis()
        ));

        // 2. Token-aware text splitter (500 tokens per chunk, 50 token overlap)
        TokenTextSplitter splitter = new TokenTextSplitter(500, 50, 5, 10000, true);
        List<Document> chunks = splitter.apply(List.of(rawDoc));

        // 3. Batch ingest into pgvector (Automatically computes embeddings!)
        vectorStore.accept(chunks);
    }
}
```

---

## 6. Tool Calling Implementation (`EnterpriseTools.java`)

Backend methods ko LLM ke liye tools me convert karna:

```java
package com.backend.ai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class EnterpriseTools {

    @Tool(description = "Customer ke order ID ke basis par current live status fetch karta hai.")
    public OrderStatusResult getOrderStatus(OrderStatusQuery query) {
        // Real-world DB query or ERP API call
        if ("ORD-101".equalsIgnoreCase(query.orderId())) {
            return new OrderStatusResult("ORD-101", "OUT_FOR_DELIVERY", "BlueDart-8812", "Expected by 5 PM today");
        }
        return new OrderStatusResult(query.orderId(), "PROCESSING", "N/A", "Warehouse packing in progress");
    }

    public record OrderStatusQuery(String orderId) {}
    public record OrderStatusResult(String orderId, String status, String trackingNumber, String message) {}
}
```

---

## 7. Chat Controller with Streaming & Tool Calling

```java
package com.backend.ai.controller;

import com.backend.ai.tools.EnterpriseTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final ChatClient chatClient;
    private final EnterpriseTools enterpriseTools;

    public AiChatController(ChatClient ragChatClient, EnterpriseTools enterpriseTools) {
        this.chatClient = ragChatClient;
        this.enterpriseTools = enterpriseTools;
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatWithTools(
            @RequestParam("conversationId") String conversationId,
            @RequestParam("message") String userMessage) {

        return chatClient.prompt()
                .advisors(a -> a.param("chat_memory_conversation_id", conversationId))
                .tools(enterpriseTools) // Expose tools to LLM
                .user(userMessage)
                .stream()
                .content();
    }
}
```

---

## 8. Resilience & Circuit Breaker Fallback Pattern

Agar OpenAI down ho jaye ya billing limit exceed ho jaye, backend crash nahi hona chahiye! Hum Resilience4j se local Ollama (LLaMA 3) par failover karte hain:

```java
package com.backend.ai.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ResilientAiService {

    private final ChatClient openAiClient;
    private final ChatClient localOllamaClient;

    public ResilientAiService(ChatClient openAiClient, ChatClient localOllamaClient) {
        this.openAiClient = openAiClient;
        this.localOllamaClient = localOllamaClient;
    }

    @CircuitBreaker(name = "openaiService", fallbackMethod = "fallbackToLocalOllama")
    public String askAi(String prompt) {
        return openAiClient.prompt().user(prompt).call().content();
    }

    // Fallback method executed when OpenAI fails or Circuit Breaker is OPEN
    public String fallbackToLocalOllama(String prompt, Throwable ex) {
        System.err.println("OpenAI Down! Falling back to Local Ollama: " + ex.getMessage());
        return localOllamaClient.prompt()
                .system("You are a backup offline AI.")
                .user(prompt)
                .call()
                .content();
    }
}
```

---

## 9. Key Takeaways
- Spring AI integrates RAG, PgVector, and Chat Memory in just a few lines of configuration.
- `@Tool` annotation makes Java methods directly callable by LLMs.
- Always wrap external AI APIs with Resilience4j Circuit Breakers to maintain 99.99% backend SLA.

