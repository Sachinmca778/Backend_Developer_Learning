# Module 02: Function Calling & Structured JSON Outputs 🛠️

Backend developers ke liye LLMs ka sabse powerful feature **Function Calling (Tool Calling)** aur **Guaranteed Structured Outputs** hai. Iske bina LLM sirf ek chatbot hai; iske saath LLM aapke pure backend microservices ecosystem ka controller ban sakta hai!

---

## 1. Function Calling Under The Hood: Step-by-Step Flow

Ek baat dimag me clear kar lein: **LLM khud aapka backend code ya database query execute NAHI karta!**
LLM sirf decide karta hai ki *kaun sa function call karna hai* aur *uske arguments kya hone chahiye*. Execution 100% aapke backend server par hoti hai.

### End-to-End Sequence Diagram:

```
[ User ]          [ Spring Boot Backend ]                [ LLM (GPT-4o) ]         [ Database / 3rd Party ]
   │                        │                                   │                           │
   │── "Where is order #42?"──>                                 │                           │
   │                        │── Tools Definition + Prompt ─────>│                           │
   │                        │   (get_order_status schema)       │                           │
   │                        │                                   │                           │
   │                        │<── Returns tool_call: ────────────│                           │
   │                        │    {name: "get_order_status",     │                           │
   │                        │     args: {"orderId": "42"}}      │                           │
   │                        │                                   │                           │
   │                        │── Execute Java method ───────────────────────────────────────>│
   │                        │<── Returns: {status: "SHIPPED", tracking: "DELHIVERY-99"} ───│
   │                        │                                   │                           │
   │                        │── Send tool_result back ─────────>│                           │
   │                        │                                   │                           │
   │                        │<── Final Natural Response ────────│                           │
   │                        │    "Aapka order #42 ship ho gaya  │                           │
   │                        │     hai (Delhivery: 99)."         │                           │
   │<── Display message ────│                                   │                           │
```

---

## 2. Defining Tools via JSON Schema

LLM ko batana padta hai ki aapke backend me kaun-kaun se tools available hain. Iske liye standard **JSON Schema** use hota hai:

```json
{
  "type": "function",
  "function": {
    "name": "fetch_order_status",
    "description": "Customer ke order ID ke basis par current shipping status aur ETA fetch karta hai.",
    "parameters": {
      "type": "object",
      "properties": {
        "orderId": {
          "type": "string",
          "description": "Unique numeric or alphanumeric order identifier, e.g. ORD-1092"
        },
        "includeTrackingHistory": {
          "type": "boolean",
          "description": "True agar user detailed checkpoints chahta hai"
        }
      },
      "required": ["orderId"],
      "additionalProperties": false
    },
    "strict": true
  }
}
```

> [!TIP]
> **Tool descriptions matter immensely!** LLM tool description ko padh kar decide karta hai ki kab kis tool ko call karna hai. Description me clear likhein ki function kab call karna hai aur arguments ka exact matlab kya hai.

---

## 3. Structured Outputs: Guaranteed JSON (Strict Schema)

Purane time me developers prompt me likhte the: *"Return JSON only, no markdown"*. Fir bhi model kabhi-kabhi text ya extra trailing commas jod deta tha jisse `new ObjectMapper().readValue(...)` fail ho jata tha (`JsonParseException`).

Modern LLMs provide **Strict Structured Outputs** (`strict: true`). Yeh grammar-constrained sampling use karte hain — decoding step par model ko invalid JSON token spit karne ki mathematically ijazat hi nahi hoti!

### Example: Strict Schema Response Format
```json
{
  "model": "gpt-4o",
  "messages": [
    {"role": "system", "content": "Extract customer details from support email."},
    {"role": "user", "content": "Hi, I am Rahul Sharma from Bangalore. My phone is 9876543210. Order 551 is delayed."}
  ],
  "response_format": {
    "type": "json_schema",
    "json_schema": {
      "name": "CustomerExtraction",
      "strict": true,
      "schema": {
        "type": "object",
        "properties": {
          "customerName": { "type": "string" },
          "city": { "type": "string" },
          "phone": { "type": "string" },
          "orderId": { "type": ["string", "null"] },
          "issueCategory": { 
            "type": "string",
            "enum": ["BILLING", "DELIVERY_DELAY", "RETURN_REFUND", "GENERAL"]
          }
        },
        "required": ["customerName", "city", "phone", "orderId", "issueCategory"],
        "additionalProperties": false
      }
    }
  }
}
```

Is request ka response **100% guarantee ke saath valid JSON** hoga jo aapke Java DTO me direct deserialize ho sakta hai!

---

## 4. Java 21 Record & Jackson Mapping

Spring Boot me hum response ko type-safe Java Records me parse karte hain:

```java
package com.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CustomerExtractionResponse(
    @JsonProperty("customerName") String customerName,
    @JsonProperty("city") String city,
    @JsonProperty("phone") String phone,
    @JsonProperty("orderId") String orderId,
    @JsonProperty("issueCategory") IssueCategory issueCategory
) {
    public enum IssueCategory {
        BILLING, DELIVERY_DELAY, RETURN_REFUND, GENERAL
    }
}
```

### Parsing Service:
```java
package com.backend.ai.service;

import com.backend.ai.dto.CustomerExtractionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class ParsingService {

    private final ObjectMapper objectMapper;

    public ParsingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CustomerExtractionResponse parseAiResponse(String rawJsonFromLlm) {
        try {
            return objectMapper.readValue(rawJsonFromLlm, CustomerExtractionResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse AI response into DTO: " + e.getMessage(), e);
        }
    }
}
```

---

## 5. Spring AI `@Tool` Annotation Preview

Agar aap Spring AI use kar rahe hain, toh aapko manual JSON schemas likhne ki zaroorat nahi hoti. Aap simple Java bean par `@Tool` annotation laga sakte hain:

```java
package com.backend.ai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class InventoryTools {

    @Tool(description = "Check product stock availability in specific warehouse")
    public StockResponse checkStock(StockRequest request) {
        // Business logic: check PostgreSQL inventory table
        return new StockResponse(request.sku(), 45, "IN_STOCK");
    }

    public record StockRequest(String sku, String warehouseCode) {}
    public record StockResponse(String sku, int quantity, String status) {}
}
```

Spring AI automatically Java class ka JSON schema reflect karta hai, LLM ko offer karta hai, LLM ke response ko method parameters me map karta hai, method run karta hai, aur result model ko bhejta hai! (Iski deep implementation hum Module 05 me dekhenge).

---

## 6. Summary Checklist
- [x] Function Calling = LLM decides tool + arguments, Backend executes logic.
- [x] `strict: true` enables Grammar-Constrained Sampling, eliminating JSON syntax errors.
- [x] Clear descriptions in tools are critical for LLM accuracy.
- [x] Java Records + Jackson DTOs provide clean, type-safe integration.

