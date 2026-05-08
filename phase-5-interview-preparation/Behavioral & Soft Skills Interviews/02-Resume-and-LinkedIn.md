# Resume & LinkedIn

## Status: Not Started

---

## Table of Contents

1. [Resume Goals & Structure](#resume-goals--structure)
2. [1-Page Rule for 1-3 yr Engineers](#1-page-rule-for-1-3-yr-engineers)
3. [Action Verbs Library](#action-verbs-library)
4. [Quantified Bullet Formula](#quantified-bullet-formula)
5. [PHP → Java Transition Framing](#php--java-transition-framing)
6. [ATS Optimization](#ats-optimization)
7. [Project Section Templates](#project-section-templates)
8. [Sample Resume — Backend Engineer (1.8 yr)](#sample-resume--backend-engineer-18-yr)
9. [LinkedIn Profile Optimization](#linkedin-profile-optimization)
10. [GitHub Portfolio](#github-portfolio)
11. [Open Source Contributions](#open-source-contributions)
12. [Common Output Traps](#common-output-traps)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Resume Goals & Structure

> "Resume ka goal = **interview call generate karna** — not full bio."

Recruiter spends ~7-10 sec on first scan. Pass that bar with:

1. **Clear name + title + contact**
2. **Top section = strongest signal** (latest job for experienced; projects for transition)
3. **Quantified bullets**
4. **Tech keywords** prominently
5. **Clean layout** — single column, simple font

### Standard structure (1-3 yr)

```
1. Header (Name, Title, Email, Phone, LinkedIn, GitHub, Location)
2. Summary (2-3 lines — optional but useful for transition)
3. Skills (categorized — Languages, Frameworks, DBs, Tools)
4. Experience (most recent first; bullets quantified)
5. Projects (1-3 strong ones, especially for tech transition)
6. Education (concise — degree, institute, year, GPA only if strong)
7. Achievements / Certifications (if relevant)
```

→ For transition (PHP → Java) candidates: **Projects section = differentiator**. Make it strong.

---

## 1-Page Rule for 1-3 yr Engineers

| Experience | Pages |
|------------|-------|
| 0-3 years | **1 page** mandatory |
| 3-7 years | 1-2 pages |
| 7+ years | 2 pages |
| Senior / Staff | 2 pages max |

→ Indian recruiters sometimes accept 2 pages for 1-3 yr — **but 1 page stronger signal**.

### How to compress

- 0.5" margins (not 1")
- 10-11pt font (Calibri / Roboto / Inter)
- No "Hobbies", "References available on request"
- Skip courses / training that don't add value
- Tighten bullets (cut "the", "a", filler)

---

## Action Verbs Library

> "First word of every bullet = strong action verb. **Never** start with 'Worked on' or 'Responsible for'."

### Built / Created

`Built · Designed · Developed · Engineered · Implemented · Architected · Created · Programmed`

### Improved

`Reduced · Improved · Optimized · Enhanced · Streamlined · Accelerated · Refactored · Modernized`

### Led / Owned

`Led · Owned · Spearheaded · Drove · Coordinated · Mentored · Championed · Initiated`

### Migrated / Scaled

`Migrated · Scaled · Refactored · Decomposed · Consolidated · Decoupled · Automated`

### Analyzed / Diagnosed

`Diagnosed · Analyzed · Investigated · Identified · Resolved · Debugged · Profiled`

### Collaborated

`Collaborated · Partnered · Coordinated · Integrated · Aligned`

### Avoid

`Worked on, Helped with, Assisted with, Did, Was responsible for, Participated in`

---

## Quantified Bullet Formula

```
[Action Verb] + [What you did] + [How / Tech] + [Impact with NUMBER]
```

### Examples

❌ "Worked on payment integration."

✅ "**Built** payment gateway integration **using Spring Boot + Stripe SDK**, processing **₹2 cr+ monthly** with **99.9% success rate**."

---

❌ "Improved API performance."

✅ "**Reduced** REST API P95 latency **from 1.2s to 280ms** (-77%) **by introducing Redis caching layer**, eliminating ~40% DB load."

---

❌ "Helped fix bugs."

✅ "**Diagnosed and resolved 12 production bugs** across order + inventory services, reducing **Sentry error rate by 60%** over 2 months."

---

❌ "Did code reviews."

✅ "**Reviewed 80+ pull requests** as code reviewer, providing actionable feedback that reduced post-merge bug count by ~25%."

---

### Quantification ideas (when no exact number)

- Estimate honestly: "~30%", "approximately 5,000 users"
- Time: "shipped in 3 weeks", "reduced from 30 sec to 280ms"
- Volume: "handled 2 cr requests/month", "50K MAU app"
- Team: "team of 4 engineers", "8+ services"

→ **Even weak number beats no number.** Don't fabricate exact figures.

---

## PHP → Java Transition Framing

### Resume strategy

If active PHP role + learning Java:

1. **Skills section** — Java prominent + PHP secondary
2. **Experience section** — PHP role with **language-agnostic framing** (focus on backend concepts, not "PHP" repeatedly)
3. **Projects section** — **Java/Spring Boot projects** prominent

### Skill section

```
Languages:    Java (Spring Boot, JPA), PHP (Laravel/CodeIgniter), JavaScript (basics)
Backend:      REST APIs, Microservices, JWT Auth, Spring Security, OpenFeign
Databases:    PostgreSQL, MySQL, Redis, MongoDB
Messaging:    Apache Kafka, RabbitMQ, Spring AMQP
DevOps:       Docker, Docker Compose, Git, CI/CD (GitHub Actions)
Other:        JUnit 5, Mockito, Postman, IntelliJ IDEA
```

→ **Java first** in languages. Keep PHP credible.

### Experience bullet — language-agnostic when possible

```
PHP Backend Developer @ <Company>                     <Date> - Present

• Designed and shipped 30+ REST API endpoints for user/order modules,
  serving ~2 lakh DAU with P95 < 400ms.
• Reduced order processing time by 40% by introducing background job
  queue (Redis + worker) for non-critical work.
• Owned monitoring + alerting setup across 6 services using Prometheus + Grafana.
• Mentored 2 interns on backend fundamentals — Git workflow, REST design, testing.
```

→ Notice: barely any "PHP" repeated. Concepts (REST, queue, monitoring) transfer to Java.

### Summary section (helps for transition)

```
Backend engineer with 1.8+ years building production REST APIs and microservices.
Currently deepening expertise in Java + Spring Boot ecosystem (JPA, Security,
Kafka, Spring Cloud) through structured learning + side projects. Strong
fundamentals in OOP, system design, distributed systems, and clean architecture.
```

### Projects (Java) — strongest signal

(See [Project Section Templates](#project-section-templates).)

---

## ATS Optimization

> "60-70% large-co resumes filtered by **Applicant Tracking System** before human."

### What ATS does

- Parses resume text (struggles with images, tables, fancy designs)
- Matches keywords to JD
- Ranks → recruiter sees top X

### Tips

1. **Plain text-friendly** — single column, no tables, no headers/footers, no text inside images
2. **Standard section headers** — "Experience", "Education", "Skills"
3. **Match JD keywords** — if JD says "Spring Boot", don't write "Springboot"
4. **PDF (text-based)** preferred — not Word, not scanned PDF
5. **Avoid graphics-heavy templates** (Canva styled, two-column with sidebars)

### Keyword stuffing — avoid

❌ "Java Java Java Spring Boot Spring Boot Microservices Microservices..."

✅ Natural placement in skills + bullets + summary.

### Quick test

Copy-paste your PDF → Notepad. Result readable + structured? ATS will likely parse OK.

---

## Project Section Templates

### Template

```
PROJECT NAME — One-line summary                                 [Optional: GitHub link]
Tech: Java 17, Spring Boot 3.2, PostgreSQL, Redis, Docker

• Designed RESTful API for [domain] with [feature 1], [feature 2], handling [scale].
• Implemented [auth / caching / async] using [tech / pattern].
• Wrote [N] unit + integration tests achieving [coverage %].
• Containerized with Docker Compose for one-command local setup.
```

### Example 1 — URL Shortener

```
URL SHORTENER SERVICE                                            github.com/sachin/url-shortener
Tech: Java 17, Spring Boot 3, PostgreSQL, Redis, Docker

• Built scalable URL shortener with base62 encoding, custom alias support, and TTL-based expiry.
• Implemented Redis caching layer reducing DB lookup load by ~80% for hot URLs.
• Added rate limiting using token bucket algorithm (Bucket4j) at 100 req/min per IP.
• Containerized with Docker Compose; one-command setup including Postgres + Redis.
• Wrote 40+ JUnit 5 + Mockito tests covering controller + service layers (~85% coverage).
```

### Example 2 — Microservices Demo

```
ORDER MANAGEMENT — Microservices Demo                            github.com/sachin/order-mgmt
Tech: Java 17, Spring Boot 3, Spring Cloud Gateway, OpenFeign, Kafka, Postgres, Docker

• Decomposed monolithic order flow into 4 microservices: User, Order, Inventory, Notification.
• Implemented event-driven communication with Kafka — Order publishes events; Inventory + Notification consume.
• Set up API Gateway with Spring Cloud Gateway for routing + JWT auth filter.
• Implemented saga pattern (choreography) for order placement with compensating actions.
• Containerized full stack with Docker Compose for one-command local setup.
```

### Example 3 — Real-time Chat

```
REAL-TIME CHAT SERVICE                                           github.com/sachin/chat-service
Tech: Java 17, Spring Boot 3 (WebSocket + STOMP), Redis Pub/Sub, MongoDB, Docker

• Built WebSocket-based real-time messaging for 1-on-1 + group chat.
• Used Redis Pub/Sub for cross-instance message broadcast (horizontally scalable).
• Persisted chat history in MongoDB with index on (room_id, timestamp).
• Implemented JWT auth handshake on WebSocket upgrade.
```

→ Each project: **3-5 bullets**, every bullet has a verb + tech + impact.

---

## Sample Resume — Backend Engineer (1.8 yr)

```
═══════════════════════════════════════════════════════════════════════
SACHIN <LASTNAME>                       sachin@email.com  | +91-XXXXXXXXXX
                                        linkedin.com/in/sachin-...    
                                        github.com/sachin-...    
                                        Bengaluru, India
═══════════════════════════════════════════════════════════════════════

SUMMARY
Backend engineer with 1.8+ years building production REST APIs and microservices.
Strong fundamentals in OOP, distributed systems, and clean architecture. Currently
deepening Java + Spring Boot expertise (JPA, Security, Kafka, Spring Cloud).

SKILLS
Languages:    Java (Spring Boot, JPA), PHP (Laravel/CodeIgniter), JavaScript
Backend:      REST APIs, Microservices, JWT Auth, Spring Security, OpenFeign
Databases:    PostgreSQL, MySQL, Redis, MongoDB
Messaging:    Apache Kafka, RabbitMQ, Spring AMQP
DevOps:       Docker, Docker Compose, Git, GitHub Actions, Linux
Testing:      JUnit 5, Mockito, Postman, Testcontainers

EXPERIENCE

Backend Developer @ <Current Company>                  May 2024 — Present
• Designed 30+ REST API endpoints (PHP/Laravel) for user/order modules
  serving ~2 lakh DAU with P95 < 400ms.
• Reduced order processing time 40% by introducing Redis-backed background
  job queue for non-critical operations.
• Owned monitoring + alerting setup across 6 services using Prometheus + Grafana.
• Diagnosed + resolved 12 production bugs reducing Sentry error rate ~60% over 2 months.
• Mentored 2 interns on backend fundamentals — Git workflow, REST design, testing.

PROJECTS

Order Management — Microservices Demo                    github.com/.../order-mgmt
Tech: Java 17, Spring Boot 3, Spring Cloud Gateway, OpenFeign, Kafka, Postgres
• Decomposed monolithic order flow into 4 microservices.
• Event-driven via Kafka; saga pattern (choreography) for order placement.
• API Gateway with JWT auth filter; full stack containerized.

URL Shortener Service                                    github.com/.../url-shortener
Tech: Java 17, Spring Boot 3, PostgreSQL, Redis, Docker
• Base62-encoded URL shortener with TTL expiry + custom aliases.
• Redis cache layer reducing DB lookups ~80% for hot URLs.
• Token bucket rate limiter (Bucket4j) at 100 req/min per IP.

EDUCATION
B.Tech in Computer Science                              <Institute>, <Year>
CGPA: 8.4 / 10

CERTIFICATIONS / ACHIEVEMENTS
• Spring Professional Certification (or other)          [if any]
• Top contributor in <internal hackathon>               [if any]
═══════════════════════════════════════════════════════════════════════
```

---

## LinkedIn Profile Optimization

### Headline (most important — appears in search)

❌ "Software Engineer at <Company>"

✅ "Backend Developer | Java, Spring Boot, Microservices | Building Scalable REST APIs"

→ Recruiter searches "Java Spring Boot" — your profile surfaces.

### About / Summary

```
Backend engineer with 1.8+ years of production experience building REST APIs and 
microservices for high-traffic applications.

What I work on:
• Designing scalable backend services (Java + Spring Boot, REST + gRPC)
• Microservices architecture, Kafka-based event-driven systems
• PostgreSQL / MySQL data modeling, query optimization, Redis caching
• CI/CD with Docker + GitHub Actions

Recent: Currently expanding into JVM-based microservices + cloud-native patterns.

Open to: Mid-level Backend Engineer roles (Bengaluru / Remote / Hybrid).
Drop a message if you're hiring for Java + Spring Boot backend roles!
```

### Experience section

- **Same bullets as resume**, slightly more detailed
- Add company logo
- Tag your manager / coworkers (visibility)

### Featured section

Pin: **GitHub repo links**, certifications, articles, demo videos.

### Skills section

Top 3 visible:
1. Java
2. Spring Boot
3. Microservices

Add 20-30 relevant skills; get **endorsements** from coworkers.

### Recommendations

Ask 2-3 ex-coworkers / managers for short recommendations.

Template ask:

> "Hi <name>, I'm preparing for new opportunities. Would you be open to writing a short LinkedIn recommendation about our work on <project X>? Even 2-3 lines on <skill / impact> would help. Happy to write one for you in return!"

### Activity / Posts

Active engagement signals to recruiters:
- Comment on technical posts (don't just like)
- Share 1-2 posts/month — what you're learning
- Avoid politics / controversies

### Profile photo

- Professional, smiling, recent
- Plain background or office
- Smartphone front camera at well-lit window works

### URL

Custom: `linkedin.com/in/sachin-singh-backend` (cleaner).

### Searchability

Settings → make profile public.

---

## GitHub Portfolio

> "Recruiters check GitHub for backend roles. **Empty / inactive GitHub = red flag** for transition candidates."

### Profile setup

- **Profile README** with intro:

```markdown
### Hi, I'm Sachin 👋

Backend engineer working with **Java + Spring Boot**.

🔭 Currently exploring: Kafka, microservices, distributed systems
🌱 Learning: Spring Cloud, Docker / Kubernetes
📫 Reach me: sachin@email.com | LinkedIn

#### Featured Projects
- 🛒 [order-mgmt](https://...) — Microservices demo with Kafka + Saga
- 🔗 [url-shortener](https://...) — REST API + Redis cache
```

- Pin **3-6 best repos**

### Repo quality

Each pinned repo should have:

| Item | What |
|------|------|
| README.md | Clear (what, why, how to run) |
| Tech stack list | Top of README |
| Demo / screenshots | If web app |
| Setup steps | `docker compose up` ideal |
| Structure overview | Folder tree |
| License | MIT typical |

### Commit history

- **Frequent green squares** — signals active developer
- **Meaningful commits** — not "asdf", "wip"
- **Branch hygiene** — main branch clean

### Don'ts

- Tutorial follow-along projects ("To-do app from YouTube")
- Forks of others' repos as "your project"
- Empty / abandoned repos in pinned list

---

## Open Source Contributions

> "Even **1-2 merged PRs** to popular project = strong signal of code quality + collaboration."

### Starter strategy

1. **Pick a project you use** — Spring Boot, JUnit, Mockito, Lombok, etc.
2. Look for `good-first-issue` / `help-wanted` labels
3. Read CONTRIBUTING.md carefully
4. **Read existing tests** before fixing
5. Comment on issue: "Can I work on this?"
6. Submit PR with **tests** + clear description

### Easy wins

- **Documentation fixes** (typos, examples) — but mention "small fix" honestly
- **Test coverage gaps** — add missing tests
- **Bug fixes** with reproducer

### Less easy but bigger impact

- **Feature implementation** of a discussed enhancement
- **Performance improvement** with benchmark

### LinkedIn / resume mention

```
Open Source Contributions
• Contributed bug fix to <Project> — improved error message clarity (PR #1234, merged).
• Added test coverage for <module> in <Project> (PR #5678, +250 tests).
```

→ Link to PRs.

---

## Common Output Traps

### Q1. "Worked on..." bullets

Generic and weak. Replace with action verbs.

### Q2. Skills as tag cloud

Massive list with everything. Categorize + prioritize relevant.

### Q3. Dates missing / vague

"Recent" vs "May 2024 - Present" — be specific.

### Q4. Personal info overload

Skip: address (city OK), DOB, marital status, photo (in resume — India-specific norm varies, generally skip for tech).

### Q5. PDF that's image-based

ATS can't parse. Use text-based PDF.

### Q6. Generic LinkedIn headline

"Software Engineer" doesn't surface in searches.

### Q7. Empty GitHub

Red flag for transition candidates. Build 2-3 quality projects.

---

## Pitfalls

1. **Multi-page resume** for 1-3 yr — fluff content.
2. **No quantification** — every bullet should aim for a number.
3. **PHP-only experience** with no Java projects — transition will fail.
4. **Generic LinkedIn** headline + summary — invisible in search.
5. **GitHub with tutorial follow-alongs** — recruiters notice.
6. **No README** in pinned repos.
7. **Resume + LinkedIn inconsistency** — different titles / dates.
8. **Spelling / grammar errors** — auto-disqualify for some.
9. **References on resume** — never include; "available on request" outdated.
10. **Salary / expected CTC** in resume — mention only when asked.

---

## Cheat Sheet

| Section | Length |
|---------|--------|
| Resume | 1 page (1-3 yr) |
| Summary | 2-3 lines |
| Bullets | 1 line, action verb + impact |
| LinkedIn About | 4-6 short paragraphs |
| GitHub README | 5-15 lines |

| Verb | Use for |
|------|---------|
| Built / Designed / Engineered | Created something |
| Reduced / Improved / Optimized | Made better |
| Led / Owned / Drove | Leadership |
| Migrated / Refactored | Modernization |
| Diagnosed / Resolved | Bug fixing |

| Quantify | Examples |
|----------|----------|
| Time | latency, build time |
| Money | $ saved, revenue |
| Volume | users, requests/sec |
| Quality | bugs %, uptime |

---

## Practice

1. Re-write all bullets in your current resume with: **action verb + what + how + impact**.
2. Get LinkedIn headline review from 2 backend engineers — adjust based on feedback.
3. Pick 2 strongest projects → polish READMEs → pin on GitHub.
4. Find 1 `good-first-issue` in Spring Boot ecosystem → submit PR.
5. Recruiter friend review: "Would this resume make you call me?" — iterate.
