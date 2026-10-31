# Module 06: AI Agents & Autonomous Workflows 🤖⚙️

2024-2026 me AI industry "Simple Chatbots" se nikal kar **Autonomous AI Agents** ki taraf shift ho chuki hai. Backend developer ke liye Agent samajhna isliye zaroori hai kyunki future ki business logic hardcoded `if-else` flows ke bajaye self-correcting agentic workflows me convert ho rahi hai.

---

## 1. What is an AI Agent?

Ek standard LLM sirf sawal ka jawab deta hai. **AI Agent ek aisa autonomous system hai jo goal achieve karne ke liye khud plan banata hai, environment me tools execute karta hai, output observe karta hai, aur galti hone par khud ko correct karta hai.**

```
+─────────────────────────────────────────────────────────────+
|                       ANATOMY OF AN AGENT                   |
+─────────────────────────────────────────────────────────────+
|                                                             |
|                    ┌───────────────────┐                    |
|                    │   BRAIN (LLM)     │                    |
|                    │  (Reasoning Core) │                    |
|                    └─────────┬─────────┘                    |
|                              │                              |
|         ┌────────────────────┼────────────────────┐         |
|         ▼                    ▼                    ▼         |
|   ┌───────────┐        ┌───────────┐        ┌───────────┐   |
|   │  PLANNING │        │  MEMORY   │        │   TOOLS   │   |
|   │ (Breakdown│        │(Short-term│        │ (APIs, DB,│   |
|   │  into sub-│        │ buffer &  │        │  Bash, Web│   |
|   │   tasks)  │        │ Long-term)│        │  search)  │   |
|   └───────────┘        └───────────┘        └───────────┘   |
+─────────────────────────────────────────────────────────────+
```

### The Core Agentic Loop:
```
           ┌──────────────────────────────────────────────┐
           │                                              │
           ▼                                              │
[ Goal / User Task ] ──> [ REASON (Think) ] ──> [ ACT (Tool Call) ]
                                                        │
                                                        ▼
[ Final Goal Achieved? ] <── [ OBSERVE (Result) ] <─────┘
           │
           ├── NO  ──> (Loop continues with new context)
           └── YES ──> [ Return Final Output ]
```

---

## 2. Workflows vs Autonomous Agents

Anthropic ke landmark paper *"Building Effective Agents"* ke anusaar, developers ko dono ke beech clear distinction samajhna chahiye:

```
+─────────────────────────────────────────────────────────────+
| WORKFLOWS (Deterministic DAG)                               |
| Fixed code path jahan LLMs predefined steps me execute      |
| hote hain. Highly reliable, predictable, low cost.          |
+─────────────────────────────────────────────────────────────+
                              vs
+─────────────────────────────────────────────────────────────+
| AUTONOMOUS AGENTS                                           |
| LLM dynamically decide karta hai ki kaun se steps lene hain |
| aur kitni baar loop chalana hai. Highly flexible, but risky.|
+─────────────────────────────────────────────────────────────+
```

---

## 3. Top 5 Architectural Patterns for Agents & Workflows

### Pattern 1: Prompt Chaining (Sequential Pipeline)
Ek LLM call ka output agle LLM call ka input banta hai.
```
[ User Request ] ──> [ LLM 1: Outline Generator ] ──> [ LLM 2: Code Writer ] ──> [ LLM 3: Security Auditor ]
```
- *Backend Analogy:* Linux pipes (`cat file | grep error | awk '{print $1}'`) ya Java Stream pipeline.

### Pattern 2: Routing
Classifier LLM incoming request ko inspect karta hai aur specialized downstream worker ya microservice ko route kar deta hai.
```
                     ┌──> [ Billing Specialist Agent ]
[ User Ticket ] ──> [ Router LLM ] ──> [ Tech Support Specialist Agent ]
                     └──> [ Cancellation Specialist Agent ]
```

### Pattern 3: Parallelization (Sectioning & Voting)
Task ko parallel sub-tasks me split karke ek saath execute karna (Java 21 `StructuredTaskScope` ke saath perfect match):
- **Sectioning:** Ek complex legal document ke 5 sections ko 5 parallel LLMs ek saath summarize karte hain.
- **Voting:** 3 alag-alag prompts/models se answer generate karwa kar majority vote pick karna (High stakes decisions).

### Pattern 4: Orchestrator-Workers Pattern
Ek central **Manager Agent** task ko analyze karke sub-tasks me todta hai, worker agents ko delegate karta hai, aur sabhi ke outputs ko combine karke final result deta hai.
- *Real-world example:* Software Engineering Agent — Manager backend, frontend aur DB migration tasks create karta hai aur specialized worker agents ko assign karta hai.

### Pattern 5: Evaluator-Optimizer (Self-Correction Loop)
Ek LLM code generate karta hai, doosra LLM use test karta hai. Agar test fail hota hai, toh critique wapas pehle LLM ke paas jaati hai jab tak pass na ho jaye.

```
[ Task: Write Java Function ] ──> [ Generator LLM ] ──> Code
                                         ▲               │
                                         │               ▼
                                   [ Critique ] <── [ Evaluator (Unit Test / Linter) ]
                                                         │
                                                         └── (Passes all tests) ──> [ Output ]
```

---

## 4. Production Dangers: Runaway Loops & Budget Control

Autonomous agents me sabse bada risk hota hai: **Infinite Loops & Runaway API Bills!**
Model kisi tool error me fas sakta hai aur baar-baar wahi call retry karta rahega.

### Mandatory Production Guardrails:
1. **`max_iterations` Limit:** Kabhi bhi agent loop ko unbounded mat chodiye. Hard limit set karein (e.g. `max_iterations = 10`).
2. **Token & Dollar Budget:** Har agent execution context ke saath hard limit ($0.50 per task) set karein.
3. **Loop Detection:** Agar agent identical tool arguments 3 baar lagataar call kare, toh loop force-kill karke human intervention alert raise karein.

---

## 5. Human-in-the-Loop (HITL) for Backend Safety

Backend systems me financial ya irreversible actions (e.g. `delete_customer_account`, `refund_payment`, `send_marketing_blast`) agent ko bina approval execute karne ki ijazat **KABHI** nahi honi chahiye!

```
[ Agent decides: "Refund $500 to customer" ]
                     │
                     ▼
[ Tool interceptor flags: REQUIRES_HUMAN_APPROVAL ]
                     │
                     ▼
[ Create Pending Task in DB + Slack Notification to Admin ]
                     │
         ┌───────────┴───────────┐
         ▼                       ▼
    [ ADMIN APPROVES ]      [ ADMIN REJECTS ]
         │                       │
         ▼                       ▼
[ Resume Agent & Refund ]   [ Return Rejection Reason to Agent ]
```

Spring Boot backend me is pattern ko asynchronous workflow engine (jaise Temporal ya Camunda) ke saath implement kiya jaata hai!

---

## 6. Summary Checklist
- [x] Agent = LLM + Planning + Memory + Tools in an autonomous loop.
- [x] Prefer structured workflows over unconstrained autonomous agents wherever possible.
- [x] Use Evaluator-Optimizer loop for high-accuracy code and document generation.
- [x] Never deploy an agent without `max_iterations` and a hard budget cap.
- [x] Implement Human-in-the-Loop (HITL) for all destructive backend operations.

