# Monitoring & Alerting

## Status: Not Started

---

## Table of Contents

1. [Why Monitoring?](#why-monitoring)
2. [SLI, SLO, SLA](#sli-slo-sla)
3. [Error Budget](#error-budget)
4. [The Three Pillars: Metrics, Logs, Traces](#the-three-pillars-metrics-logs-traces)
5. [Alert Fatigue & Symptom-Based Alerts](#alert-fatigue--symptom-based-alerts)
6. [On-Call Best Practices](#on-call-best-practices)
7. [Incident Response](#incident-response)
8. [Blameless Post-Mortems](#blameless-post-mortems)
9. [Runbooks](#runbooks)
10. [Tools Landscape](#tools-landscape)
11. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Why Monitoring?

**Matlab:** Production mein **kya ho raha hai** (and **kya bigad raha hai**) ka real-time view — issues users se pehle catch karo.

### Goals

- **Detect** — kuch toot raha hai
- **Diagnose** — kahan, kya, kyon
- **Decide** — alert? auto-rollback? escalate?
- **Document** — post-mortem, learnings

### Without monitoring

```
User reports outage on Twitter → engineer sees it 30 min later → starts debugging blind.
```

### With monitoring

```
Alert fires at 03:24:15 (latency spike).
Engineer sees dashboard showing one DB query slow, latest deploy 5 min ago.
Auto-rollback triggers at 03:25. Incident closed by 03:30.
```

---

## SLI, SLO, SLA

The Google SRE foundation. Memorize this hierarchy.

### SLI — Service Level Indicator

**Matlab:** Ek **measurement** of service health.

```
"99.95% of HTTP requests in last 7 days returned 2xx/3xx within 200ms"
```

Common SLIs:
- **Availability:** % successful requests
- **Latency:** P50, P95, P99 response times
- **Throughput:** RPS
- **Error rate:** % 5xx responses
- **Freshness:** age of cached / batch-processed data

### SLO — Service Level Objective

**Matlab:** **Target** for SLI — kya goal achieve karna hai (internal commitment).

```
"99.9% availability (= 43.8 min downtime/month allowed)"
"P95 latency < 300ms"
```

### SLA — Service Level Agreement

**Matlab:** **Contract** with customers — financial/legal consequences if breached.

```
"99.95% uptime guaranteed; refund 10% if breached"
```

### Hierarchy

```
SLI  → what we measure
SLO  → internal target (more strict than SLA)
SLA  → external promise (less strict, has $)
```

**Example for HTTP API:**
```
SLI:  successful_requests / total_requests
SLO:  99.9% over 30 days
SLA:  99.5% over 30 days (with refund clause)
```

### Choosing SLOs

- Start with **what users care about**: availability + latency
- 4-9s (99.99%) is **really hard** — only for critical infra
- 3-9s (99.9%) good for most APIs
- 2-9s (99%) OK for internal/dev tools

### Availability vs Downtime

| SLO | Downtime/year | Downtime/month |
|-----|---------------|----------------|
| 99% (2 nines) | 3.65 days | 7.2 hours |
| 99.9% (3 nines) | 8.76 hours | 43.8 min |
| 99.95% | 4.38 hours | 21.9 min |
| 99.99% (4 nines) | 52.6 min | 4.4 min |
| 99.999% (5 nines) | 5.26 min | 26.3 sec |

---

## Error Budget

**Matlab:** SLO ke based par **kitni error allowed** hai = `1 - SLO`.

```
SLO: 99.9% availability
Error budget: 0.1% = 43.8 minutes downtime per month
```

### Why Error Budget?

- Quantify **acceptable risk**
- Negotiate **deploy frequency vs reliability**
- "If error budget exhausted, freeze risky deploys"

### Policy Examples

```
If error budget remaining > 50%:
  → ship freely, take risks

If 20-50%:
  → caution, more testing

If < 20%:
  → freeze new features, focus on reliability

If exhausted:
  → only critical fixes; no new deploys
```

### Burn Rate

**Matlab:** Error budget kitni jaldi consume ho raha hai.

```
2% error rate × 1 hour = consumes 30% of monthly budget!
→ alert immediately
```

Multi-window burn rate alerts (Google SRE workbook):
- **Fast burn (page):** 2% in 1h or 5% in 6h
- **Slow burn (ticket):** 10% in 3 days

---

## The Three Pillars: Metrics, Logs, Traces

### 1. Metrics

**Matlab:** Numerical time-series — counts, rates, gauges.

```
http_requests_total{status="200"} = 12534
db_connections_active = 23
memory_used_bytes = 4_500_000_000
```

**Tools:** Prometheus, Datadog, CloudWatch, InfluxDB.

**Cheap to store, fast to query, perfect for dashboards/alerts.**

### Common Metrics (RED + USE)

#### RED (for services)
- **R**ate — req/sec
- **E**rrors — % failed
- **D**uration — latency

#### USE (for resources)
- **U**tilization — % busy
- **S**aturation — queue depth
- **E**rrors — error count

### 2. Logs

**Matlab:** Time-stamped events — text or structured.

```json
{"timestamp":"2026-05-08T10:23:01Z","level":"ERROR","service":"payment","message":"Stripe timeout","order_id":"abc123"}
```

**Tools:** ELK (Elasticsearch + Logstash + Kibana), Loki, Splunk, Datadog Logs, CloudWatch.

**Best for:** Forensics ("why did this specific request fail?").

### Structured Logging

```java
log.info("Order placed", 
    kv("order_id", order.getId()),
    kv("user_id", user.getId()),
    kv("amount", order.getAmount()));
```

→ Searchable: `order_id="abc123"` in log explorer.

### Log Levels

| Level | When |
|-------|------|
| `ERROR` | Something broken needing attention |
| `WARN` | Suspicious but recoverable |
| `INFO` | Major business events |
| `DEBUG` | Detailed flow (off in prod usually) |
| `TRACE` | Very detailed (rare) |

### 3. Traces

**Matlab:** Request ka **end-to-end journey** across services — distributed tracing.

```
Trace abc123:
  [API Gateway      150ms ─────────────────]
    [Auth Service    20ms ──]
    [Order Service        100ms ─────────]
      [DB Query           60ms ─────]
      [Payment Service    30ms ──]
        [Stripe API       25ms ─]
```

Each span = one operation. Traces show **where time goes**.

**Tools:** Jaeger, Zipkin, Datadog APM, AWS X-Ray, Honeycomb.

**Standards:** OpenTelemetry (OTel) — vendor-neutral instrumentation.

### When Use What?

| Question | Pillar |
|----------|--------|
| "Is service up?" | Metrics |
| "What's the P95 latency?" | Metrics |
| "Why did request abc123 fail?" | Logs (+ Traces) |
| "Where is request slow?" | Traces |
| "What error caused alert?" | Logs |
| "What's the trend?" | Metrics |

---

## Alert Fatigue & Symptom-Based Alerts

### Alert Fatigue

**Matlab:** Itne alerts aaye ke engineers ignore karne lagein → real issues miss.

**Causes:**
- Alerting on every metric blip
- Non-actionable alerts ("CPU 80%" with no action needed)
- Flapping alerts (firing/resolving constantly)
- Alerts at 3 AM that could wait till morning

### Rules of Good Alerts

#### 1. Alert on Symptoms, Not Causes

```
❌ "Server X has high CPU"
   → so what? maybe healthy, maybe not. Page-worthy?

✅ "API P95 latency > 500ms for 5 min"
   → users experiencing slowness — actionable

❌ "Database connection pool 80% full"
   → not necessarily user-impacting

✅ "Checkout error rate > 1% for 5 min"
   → users can't pay — page!
```

**Why?** Multiple causes can produce same symptom. Alert on what users feel.

#### 2. Every Alert Must Be Actionable

If alert fires, on-call should know **what to do**.

```
Alert: "Disk usage > 90%"
Required:
  - Runbook link
  - Severity
  - Owner team
  - Action: extend volume / rotate logs / page on-call?
```

#### 3. Severity Levels

| Severity | When | Response |
|----------|------|----------|
| **P1 (Page)** | User-facing outage | Wake up at 3 AM |
| **P2 (Ticket)** | Degradation, manageable | Next business hour |
| **P3 (Warning)** | Trend concern, no immediate action | Review in standup |
| **P4 (Info)** | Just FYI | Optional |

#### 4. Avoid Flapping

Use:
- **Time windows:** "for 5 min" (not instant)
- **Hysteresis:** higher threshold to fire than to resolve
- **Burn rate** for SLO alerts

#### 5. Reduce Alert Volume

- Quarterly review: which alerts fired? actionable? signal vs noise?
- Auto-resolve when condition clears
- Group related alerts (don't 50 alerts for one outage)

### Examples — Bad to Good

```
❌ Page: every 5xx
✅ Page: 5xx error rate > 1% for 5 min

❌ Page: any pod restart
✅ Page: > 50% pods crashlooping

❌ Email per nightly batch slowness
✅ Page: batch SLA missed (didn't finish by 6 AM)
```

---

## On-Call Best Practices

### Rotation

- **Sustainable rotation** — not same person forever
- **Follow the sun** if global team
- **Fair shifts** — 1 week / month typical
- **Compensation** — paid or comp time

### Hand-off

- **Sync meeting** at start/end of shift
- **Open issues** documented
- **Ongoing context** transferred

### Tooling

- **Pager** (PagerDuty, Opsgenie, VictorOps)
- **Escalation policy** (primary → secondary → manager)
- **Mobile app** for response from anywhere

### Healthy On-Call Culture

✅ **Fix root causes** during normal hours, not band-aids overnight
✅ **Quiet pager = goal** — page volume metric tracked
✅ **Devs on-call for own services** (you build it, you run it)
✅ **Time off after rough shift**
❌ Don't make on-call burnout-inducing

### When You Get Paged

```
1. Acknowledge alert (PagerDuty: hit ack)
2. Open dashboard — what's symptoms?
3. Open runbook for that alert
4. Assess: is it real / actionable?
5. Investigate using metrics + logs + traces
6. Mitigate (rollback, scale up, etc.)
7. Communicate (status page, Slack channel)
8. After resolution: file post-mortem ticket
```

---

## Incident Response

**Matlab:** Outage / major issue handle karne ka structured process.

### Phases

#### 1. Detect

Alert fires → on-call paged.

#### 2. Triage

- How bad? (How many users affected?)
- Who needs to know? (Customer comms? CEO?)
- Severity assigned (SEV1, SEV2, SEV3)

#### 3. Mobilize

- **Incident Commander (IC)** — orchestrates response
- **Comms lead** — internal + customer updates
- **Subject matter experts** — engineers diagnosing
- War room (Zoom, Slack channel)

#### 4. Mitigate

**Goal: stop user pain ASAP**, not necessarily fix root cause.

- Rollback recent deploy
- Disable feature flag
- Scale up capacity
- Failover to backup

### Mitigation > Investigation

```
❌ "Let me find why this is broken first"
✅ "Rollback first — investigate after stable"
```

#### 5. Communicate

- **Status page** (statuspage.io, Atlassian Statuspage)
- **Internal Slack** — all hands aware
- **Customer email** if extended outage
- **Updates every 15-30 min** even if no progress

#### 6. Resolve

- Root cause fixed
- Service stable for sustained period
- Status page back to "operational"

#### 7. Post-Mortem

(Next section.)

### Severity Levels (Example)

| Sev | Criteria | Response |
|-----|----------|----------|
| SEV1 | Full outage, data loss | All hands, IC, status page |
| SEV2 | Major degradation, partial outage | On-call + IC |
| SEV3 | Minor issue, workaround exists | On-call ticket |
| SEV4 | Cosmetic / non-urgent | Backlog |

---

## Blameless Post-Mortems

**Matlab:** Incident ke baad **document** kya hua, **kyu** hua, **kya seekha** — blame nahi, learning focus.

### Why Blameless?

- People hide info if they fear blame → repeat outages
- "Human error" usually a system flaw — bad processes, missing safeguards
- Goal: **prevent class of issues**, not punish

### Template

```markdown
# Post-Mortem: [Brief description] (2026-05-08)

## Status
Resolved

## Severity
SEV2

## Timeline (UTC)
- 03:24 — Alert fires: "Checkout error rate > 5%"
- 03:26 — On-call (Rahul) acks, opens dashboard
- 03:31 — Identified: recent deploy v1.2.4 caused null pointer
- 03:34 — Rollback initiated
- 03:38 — Service restored
- 03:40 — Status page updated

## Impact
- Duration: 14 minutes
- Affected users: ~1,200
- Failed orders: 87
- Revenue impact: ~$4,500
- SLO impact: consumed 35% of monthly error budget

## Root Cause
The new shipping calculator assumed `address.zipcode` was non-null,
but legacy users have null zip codes. NPE caused checkout to 500.

## Trigger
Deploy of PR #1234 at 03:21 UTC.

## Detection
Alert fired 3 min after first error.

## Resolution
Rolled back via `kubectl rollout undo`. v1.2.3 stable since.

## What went well
- Alert fired quickly
- Rollback procedure worked smoothly
- IC + comms in <5 min

## What went wrong
- Test suite didn't cover null zipcode case
- Code review missed the assumption
- Canary stage didn't exist for this service

## Action items
| # | Action | Owner | Due |
|---|--------|-------|-----|
| 1 | Add test coverage for null zipcodes | Rahul | 2026-05-10 |
| 2 | Add NotNull/Optional in ShippingCalc API | Priya | 2026-05-15 |
| 3 | Set up canary deploy for checkout service | DevOps | 2026-05-20 |
| 4 | Update on-boarding docs with this lesson | Tech Lead | 2026-05-12 |

## Lessons learned
Always validate assumptions about legacy data with real prod sample queries
before deploys.
```

### Rules

- **No "John failed to..."** — say "the system failed when..."
- **Track action items** to completion
- **Share** post-mortems org-wide
- **Aggregate trends** quarterly — what classes of incidents recur?

---

## Runbooks

**Matlab:** Step-by-step **operational guide** for handling specific scenarios — written **before** the incident, used **during**.

### What Goes In

```markdown
# Runbook: High DB Connection Pool Usage

## Alert that triggers this
`db_connection_pool_usage > 90%`

## Initial assessment (2 min)
1. Open dashboard: [link to Grafana]
2. Check: Is traffic abnormally high?
3. Check: Is there a slow query? Run:
   `SELECT query, mean_exec_time FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;`

## Common causes & actions

### Cause 1: Slow query holding connections
1. Identify slow query (above)
2. If new: kill it: `SELECT pg_cancel_backend(pid) FROM pg_stat_activity WHERE ...`
3. Notify dev to fix the query

### Cause 2: Pool too small for load
1. Check recent traffic graph
2. If sustained: scale pool size in config (`hikari.maximumPoolSize`)
3. Restart app pods one at a time
4. Monitor connection usage

### Cause 3: DB itself overloaded
1. Check DB CPU, IOPS
2. If at limit: scale instance size or add read replica

## When to escalate
- If after 10 min usage still > 90%
- If error rate > 1%

## Related links
- [Service architecture diagram]
- [DB monitoring dashboard]
- [Tribal knowledge wiki]
```

### Best Practices

- **Linked from alerts** (PagerDuty includes runbook URL)
- **Recently tested** — outdated runbooks dangerous
- **Audit during post-mortem** — gaps update karo
- **Living docs** — version controlled

---

## Tools Landscape

### All-in-One

| Tool | Notes |
|------|-------|
| **Datadog** | SaaS, expensive, comprehensive |
| **New Relic** | APM strength |
| **Dynatrace** | AI-driven |
| **Grafana Cloud** | Open-source-friendly |

### Open-Source Stack

```
Metrics:  Prometheus + Grafana
Logs:     Loki (or ELK / OpenSearch)
Traces:   Tempo (or Jaeger)
Alerts:   Alertmanager
```

### Cloud-Native

- **AWS:** CloudWatch, X-Ray, OpenSearch
- **GCP:** Cloud Monitoring, Cloud Trace, Cloud Logging
- **Azure:** Monitor, Application Insights

### Paging

- **PagerDuty** (industry standard)
- **Opsgenie**
- **VictorOps** (Splunk On-Call)
- **Squadcast**

### Status Pages

- **Atlassian Statuspage**
- **Better Stack**
- **Instatus**

### Synthetic Monitoring

(Probe service from outside)
- **Pingdom**
- **Datadog Synthetics**
- **Uptime Robot**
- **CheckMK**

### OpenTelemetry

Vendor-neutral spec — instrument once, send to any backend.

```yaml
# OTel collector config (snippet)
exporters:
  prometheus: ...
  loki: ...
  jaeger: ...
```

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| **SLI** | Measurement (e.g., availability %) |
| **SLO** | Internal target (e.g., 99.9%) |
| **SLA** | External contract |
| **Error budget** | 1 - SLO; allowed failure |
| **Burn rate** | How fast budget consumed |
| **RED** | Rate, Errors, Duration (services) |
| **USE** | Utilization, Saturation, Errors (resources) |
| **Symptom alerts** | What users feel, not internals |
| **P50/P95/P99** | Latency percentiles |
| **Pillars** | Metrics, Logs, Traces |
| **OpenTelemetry** | Vendor-neutral instrumentation |
| **Runbook** | Pre-written response guide |
| **Post-mortem** | Blameless retrospective |
| **IC** | Incident Commander |
| **Mitigate first** | Rollback before debug |

---

## Practice

1. Define SLI + SLO for one of your services (availability + latency).
2. Calculate error budget for last month — what % consumed?
3. Pick an alert in your system — is it symptom-based or cause-based? Improve.
4. Write a runbook for one common alert.
5. Document a past incident as a blameless post-mortem.
6. Set up structured logging (`logback-json` or similar) — search by request ID.
7. Add basic OpenTelemetry instrumentation to a Spring Boot app.
