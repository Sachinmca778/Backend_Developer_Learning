# High Availability Design

## Status: Complete

---

## Table of Contents

1. [What is Availability](#what-is-availability)
2. [The 9's Table](#the-9s-table)
3. [SLI / SLO / SLA / Error Budget](#sli--slo--sla--error-budget)
4. [Eliminate Single Points of Failure](#eliminate-single-points-of-failure)
5. [Redundancy at Every Layer](#redundancy-at-every-layer)
6. [Active-Active vs Active-Passive](#active-active-vs-active-passive)
7. [Failure Detection & Failover](#failure-detection--failover)
8. [MTBF, MTTR, MTTD](#mtbf-mttr-mttd)
9. [Chaos Engineering](#chaos-engineering)
10. [Blast Radius Reduction](#blast-radius-reduction)
11. [Runbooks & Game Days](#runbooks--game-days)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## What is Availability

> "**Availability** = system kitna time **up + serving requests** hai. Formula:  
> `Availability = uptime / (uptime + downtime)`"

Expressed as a **percentage** or **number of 9's**.

> "100% impossible hai. Cost vs availability ka curve **exponential** hota hai — 99.9% se 99.99% jaane mein 10× more engineering."

---

## The 9's Table

| Availability | Downtime/year | Downtime/month | Downtime/week | What it means |
|--------------|----------------|------------------|------------------|------------------|
| 99% (2 nines) | 3.65 days | 7.2 hrs | 1.68 hrs | Hobby / dev |
| 99.9% (3 nines) | **8.76 hrs** | 43.8 min | 10.1 min | Most B2C apps |
| 99.95% | 4.38 hrs | 21.9 min | 5.04 min | Mid-tier SaaS |
| 99.99% (4 nines) | **52.6 min** | 4.38 min | 1.01 min | Enterprise SaaS, e-commerce |
| 99.999% (5 nines) | **5.26 min** | 26.3 sec | 6.05 sec | Telecom, financial trading |
| 99.9999% (6 nines) | 31.5 sec | 2.63 sec | 0.6 sec | Niche; rarely required |

### Composability (chained services)

> "If A depends on B and C (sequential), availability = `A_int × B × C`. **Each dependency multiplies down**."

```
Service A (99.99%) calls B (99.9%) calls C (99.9%)
End-to-end ≈ 99.99% × 99.9% × 99.9% ≈ 99.79%
```

→ More dependencies = lower combined uptime. **Reduce critical-path dependencies** + **add fallbacks**.

---

## SLI / SLO / SLA / Error Budget

> "**Google SRE vocabulary** — be careful, interview-favorite."

| Term | Meaning |
|------|---------|
| **SLI** (Indicator) | A **measurable metric** — e.g., `success_rate`, `p99 latency`, `availability` |
| **SLO** (Objective) | **Target** for SLI — e.g., "99.95% requests succeed in 30 days" — internal goal |
| **SLA** (Agreement) | **Contract with customer** — usually **looser** than SLO (with penalties / refunds) |
| **Error Budget** | `100% - SLO` — how much **failure you can afford** in the period |

### Example

```
SLI: success_rate = OK_responses / total_responses
SLO: 99.95% over rolling 30 days
SLA: 99.9% (customer-facing — refund if breached)
Error budget: 0.05% × 30d × 24h × 60min ≈ 21.6 minutes/month
```

### Why error budget matters

- **Spent the budget?** → freeze risky deploys, focus on reliability
- **Budget remaining?** → ship features faster, take risks
- **Aligns dev + ops** incentives — both watch the same number

---

## Eliminate Single Points of Failure

> "**SPoF** = ek component fail → entire system down. Goal: **identify har layer pe SPoF, redundant karo**."

### Common SPoFs

| Layer | SPoF | Fix |
|-------|------|-----|
| **DNS** | Single DNS provider | Use 2+ DNS providers (Route 53 + Cloudflare) |
| **Load balancer** | Single LB instance | Multi-AZ managed LB (ALB), or active-passive HAProxy |
| **App server** | Single instance | ASG with min 2 instances across AZs |
| **Database** | Single primary | Multi-AZ replication + auto-failover |
| **Cache** | Single Redis node | Redis cluster + replicas |
| **Region** | Single AWS region | Multi-region active-passive (or active-active) |
| **Cloud provider** | Single cloud | Multi-cloud (only when ROI justifies) |
| **Deploy pipeline** | Single CI runner | Multi-runner; or managed (GitHub Actions) |
| **Secrets** | Hardcoded in repo | Secrets Manager + rotation |
| **People** | Bus factor 1 | Documented runbooks, on-call rotation |

### Methodology

1. **Architecture diagram** banaa lo
2. Har box pe pucho: **"agar yeh die, kya hota hai?"**
3. Single failure → service down? → SPoF
4. Add redundancy or accept the risk explicitly

---

## Redundancy at Every Layer

```
                     [ Route 53 (multi-region failover) ]
                                  ↓
                  ┌───────────────┼───────────────┐
              ALB (AZ-a, b, c)              ALB (DR region)
                                  ↓
                       ┌──────────┼──────────┐
                  ECS (AZ-a)  ECS (AZ-b)  ECS (AZ-c)
                                  ↓
                  ┌───────────────┼───────────────┐
                  RDS (Multi-AZ)         Cross-region replica
              Redis (cluster + replicas)
              S3 (auto-replicated)
```

### Redundancy types

| Type | Description |
|------|-------------|
| **N+1** | One extra unit on standby (cheap) |
| **N+2** | Two extra (handle 2 simultaneous failures or 1 + maintenance) |
| **2N** | Full duplicate (active + standby of equal capacity) |
| **2N+1** | Full duplicate plus spare |
| **Geographic** (multi-region) | Across regions / continents |

### Cost vs benefit

- N+1 cheap, but no headroom during failure (already at 100% with one less)
- **2N** expensive, full headroom
- **3+** AZs/zones often default in cloud — true 2N within region

---

## Active-Active vs Active-Passive

| | **Active-Active** | **Active-Passive** |
|--|--------------------|----------------------|
| Both serve traffic? | Yes | Only primary; secondary on standby |
| Failover time | Near-instant (already serving) | Seconds to minutes (warm-up, DNS, promotion) |
| Cost | Higher (full capacity always running) | Lower (standby idle or smaller) |
| Complexity | Higher (multi-master writes, conflict res) | Lower (one primary writer) |
| Use cases | Multi-region read/write, geo-distributed users | Most DBs, internal services with rare failover |

### Active-Active patterns

- **Multi-region read/write** (e.g., DynamoDB Global Tables, Aurora Global active writes — newer)
- **Stateless app + active-active LBs**
- **CRDT-based** apps (collaborative editing)

### Active-Passive patterns

- **RDS Multi-AZ** (standby unused for reads in classic mode)
- **Kafka MirrorMaker** to DR cluster
- **Route 53 failover** routing — primary down → secondary

### "Pilot light" / "warm standby" / "hot standby"

```
Cold standby   → standby off, manual provision (hours, cheapest)
Pilot light    → minimal infra running, scale up on failover (minutes)
Warm standby   → scaled-down active version (seconds–minutes)
Hot standby    → full-scale standby; near-instant failover (most expensive)
```

→ AWS recommended **"warm standby"** as good ROI for most DR.

---

## Failure Detection & Failover

> "**Detect karte ho ya nahi?** Half the outages are because monitoring missed it."

### Detection

| Mechanism | What |
|-----------|------|
| **Health checks** | Periodic `/health` ping (LB, K8s probe) |
| **Heartbeats** | Service publishes "I'm alive" to coordinator (Consul, Zookeeper) |
| **External synthetic monitor** | Pingdom / UptimeRobot — user-perspective |
| **Real user monitoring (RUM)** | Browser/SDK reports actual user errors |
| **Metrics + alarm** | Error rate spike, latency p99 jump → page on-call |
| **Distributed tracing** | Trace identifies failing hop in chain |

### Failover

| Style | Speed | Risk |
|-------|-------|------|
| **Automatic** | Fast | Flap risk if detection wrong (false positive triggers unnecessary failover) |
| **Manual** | Slow but careful | Human must wake up and act |
| **Semi-auto** | Auto with human confirm | Safer for risky failovers (DB primary swap) |

### DNS failover caveat

- TTL caching by clients/resolvers → failover not seen for **TTL seconds**
- Set TTL low (30–60 s) for HA endpoints; or use **anycast / Global Accelerator** for instant failover

---

## MTBF, MTTR, MTTD

| Metric | Meaning |
|--------|---------|
| **MTBF** (Mean Time Between Failures) | How often things break (longer = better) |
| **MTTR** (Mean Time To Recover) | How fast we fix (shorter = better) |
| **MTTD** (Mean Time To Detect) | How fast we notice (shorter = better) |
| **MTTA** (Mean Time To Acknowledge) | Page → on-call ack time |

### Availability ≈ `MTBF / (MTBF + MTTR)`

> "**You can't make MTBF infinite** — failures will happen. **Halve MTTR** = double availability gains. So invest in **detection + recovery**, not just prevention."

### Reduce MTTR with

- **Automated runbooks** (Step Functions / Rundeck)
- **Pre-built dashboards** (no scrambling at 3 AM)
- **Feature flags** to disable broken features instantly
- **Auto-rollback** in CodeDeploy / GitHub Actions on alarm
- **Standby environments** ready (active-passive)
- **Game days** (practice incidents)

---

## Chaos Engineering

> "**Intentionally break production** (or staging) **to verify resilience**. Pioneered by Netflix."

### Netflix Simian Army

| Tool | Behavior |
|------|----------|
| **Chaos Monkey** | Randomly kills EC2 instances |
| **Chaos Gorilla** | Kills entire AZ |
| **Chaos Kong** | Kills entire region |
| **Latency Monkey** | Injects network latency |
| **Janitor Monkey** | Removes unused resources (cost) |
| **Conformity Monkey** | Flags non-compliant resources |

### Modern tools

- **AWS Fault Injection Simulator (FIS)**
- **Gremlin** (commercial)
- **Litmus** (Kubernetes)
- **Chaos Mesh** (Kubernetes)
- **Toxiproxy** (network fault injection)

### Principles (chaosengineering.org)

1. Build **hypothesis** about steady-state behavior
2. Vary **real-world events** (server crash, network blip, region down)
3. Run experiments in **production** (carefully) or staging
4. **Automate** — continuous chaos
5. **Minimize blast radius** — kill switch ready

### Why it matters

> "Theoretical resilience ≠ proven resilience. Chaos engineering **proves** your design works under failure — finds gaps before customers do."

---

## Blast Radius Reduction

> "**When something fails, how much breaks?** Goal: **isolate failures**, not propagate."

### Techniques

| Technique | How |
|-----------|-----|
| **Cellular architecture** | Independent "cells" — each handles slice of users; one cell dies, others fine |
| **Bulkhead pattern** | Isolate resource pools per dependency (one slow downstream doesn't drain all threads) |
| **Sharding** | Per-tenant / per-region shards — one shard down, others fine |
| **Feature flags** | Disable broken feature without redeploy |
| **Circuit breaker** | Stop calling failing dependency |
| **Throttling** | Drop excess load before total collapse |
| **Graceful degradation** | Show stale / fallback content instead of error |
| **Read-only mode** | Disable writes during DB issue, keep reads alive |

### Cell-based example (AWS Builder's Library)

```
Region (us-east-1)
  ├── Cell 1  (users 0–25%)  — own LBs, app, DB shard
  ├── Cell 2  (users 25–50%)
  ├── Cell 3  (users 50–75%)
  └── Cell 4  (users 75–100%)
```

→ Cell 2 catches a bug → only **25% of users** affected, not 100%.

---

## Runbooks & Game Days

### Runbooks

> "**Step-by-step recovery instructions** for known failure modes. On-call follows them."

Examples:

- "DB primary down → trigger manual failover via this command"
- "Redis cluster split → run script X to rebalance"
- "Region outage → DNS update to switch to DR"

Format:

- **Symptom** (alarm fired, what user sees)
- **Diagnosis** (commands to verify)
- **Action** (commands / steps to recover)
- **Verification** (how to confirm fixed)
- **Post-recovery** (cleanup, paging stand-down)

### Game Days

> "**Scheduled chaos exercise** — entire team practices a known failure scenario. Tests runbooks + on-call readiness."

Run quarterly:

- Pick a scenario ("primary RDS dies")
- Inject the failure (in staging usually)
- Team responds **as if real**
- Time the response
- Update runbooks based on gaps

### Postmortems (blameless)

- After every real incident
- Focus on **systems & processes**, not individuals
- Action items tracked to completion
- Share learnings org-wide

---

## Pitfalls

1. **Promising 99.99% without infra investment** → SLA breach, refunds.
2. **Single AZ "production"** → guaranteed downtime when AZ blips.
3. **No automated failover** → manual at 3 AM.
4. **Auto-failover with bad detection** → flapping primary, data corruption.
5. **No DNS TTL planning** → 1-hour TTL means 1-hour recovery on failover.
6. **No DR tests** → DR plan turns out broken when needed.
7. **Untested runbooks** — first time you run it = during real outage.
8. **Single CI/CD provider down** → can't deploy fix during incident.
9. **Cascading retries** without backoff → minor blip → DDoS-yourself.
10. **Multi-region active-active** for stateful data without conflict strategy.
11. **No error budget tracking** → reliability work always deprioritized.
12. **No chaos testing** → first real chaos = customer-visible.

---

## Cheat Sheet

| 9's | Downtime/yr |
|-----|-------------|
| 99.9 | 8.76 h |
| 99.99 | 52.6 min |
| 99.999 | 5.26 min |

| Pattern | Use |
|---------|-----|
| **Active-Active** | Cross-region writes, instant failover |
| **Active-Passive** | DBs, lower cost |
| **Hot/Warm/Cold standby** | Pick by RTO/RPO + cost |
| **Cellular** | Blast radius isolation |
| **Bulkhead** | Resource pool isolation |
| **Feature flags** | Toggle off without deploy |
| **Read-only mode** | Survive write outage |

| Metric | Formula / use |
|--------|---------------|
| Availability | uptime / (uptime + downtime) |
| MTBF | Mean time between failures |
| MTTR | Mean time to recover (focus here!) |
| Error budget | 100% − SLO |

| Practice | Why |
|----------|-----|
| Chaos engineering | Prove resilience |
| Game days | Practice + verify runbooks |
| Postmortems (blameless) | Learn without fear |

---

## Practice

1. Compute composite availability: 5 services in chain @ 99.95% each.
2. Pick 99.99% SLO for an API → calculate error budget per month → propose alerting policy.
3. List SPoFs in a typical 3-tier web app; propose redundancy for each.
4. Write a runbook: "Primary RDS unavailable → switch to standby" — step by step.
5. Plan a game day for "AZ failure in ap-south-1a" — what to test, what to measure.
6. Compare cost of N+1 vs 2N vs 2N+1 redundancy for your stack.
