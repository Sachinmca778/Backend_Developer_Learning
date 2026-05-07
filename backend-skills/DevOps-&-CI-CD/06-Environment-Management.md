# Environment Management

## Status: Not Started

---

## Table of Contents

1. [Why Multiple Environments?](#why-multiple-environments)
2. [Environment Promotion](#environment-promotion)
3. [12-Factor App & Environment Parity](#12-factor-app--environment-parity)
4. [Feature Flags](#feature-flags)
5. [Deployment Strategies](#deployment-strategies)
6. [Blue-Green Deployment](#blue-green-deployment)
7. [Canary Deployment](#canary-deployment)
8. [Rolling Deployment](#rolling-deployment)
9. [Comparison & When to Use What](#comparison--when-to-use-what)
10. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Why Multiple Environments?

**Matlab:** Production se pehle **safe playgrounds** chahiye — test, validate, demo without breaking real users.

### Typical Environment Tiers

| Env | Purpose | Audience |
|-----|---------|----------|
| **local** | Developer machine | Dev |
| **dev** / **integration** | Latest unstable, ad-hoc testing | Dev/QA |
| **staging** / **pre-prod** | Prod-like, final validation | QA, PMs, customers |
| **production** | Real users | Everyone |

Some teams add: **QA**, **UAT**, **demo**, **load-test**.

---

## Environment Promotion

**Matlab:** Same artifact ko ek env se next mein promote karo — never re-build.

### Wrong Way

```
Build for dev → Build for staging → Build for prod
(3 different artifacts, slight differences possible)
```

### Right Way (Build Once, Promote)

```
Build artifact tagged v1.2.3-abc1234
        ↓
   Deploy to dev → smoke test
        ↓
   Promote to staging → smoke test, QA
        ↓
   Promote to prod (manual approval)
```

**Same image hash** flows through. Differences are **config**, not code.

### Promotion Mechanisms

- **Tag bump**: `staging` tag → `production` tag in registry
- **Helm value change**: `image.tag: v1.2.3`
- **GitOps**: bump tag in env folder, ArgoCD auto-syncs

```yaml
# k8s-config/overlays/prod/kustomization.yaml
images:
  - name: myapp
    newTag: v1.2.3-abc1234   # was v1.2.2-...
```

PR → review → merge → prod deploys.

---

## 12-Factor App & Environment Parity

**Matlab:** [12factor.net](https://12factor.net) — modern app principles. Multiple **directly relevant** to env management:

### Factor III: Config

- Config in **environment variables**, not in code
- Different env = different config (no rebuild)

```
DATABASE_URL=postgres://...      (different per env)
LOG_LEVEL=info                    (could be debug in dev, info in prod)
```

### Factor X: Dev/Prod Parity

**Matlab:** Dev, staging, prod **as similar as possible**.

| Gap | Why bad | Fix |
|-----|---------|-----|
| Time | Long deploy cycles → drift | Continuous deployment |
| Personnel | Devs don't deploy | DevOps culture, on-call rotation |
| Tools | SQLite in dev, Postgres in prod | Same DB everywhere (Docker) |

→ Use **same backing services** across envs (real Postgres, real Redis), differ only in scale/config.

### Factor V: Build, Release, Run

```
Build:    code → artifact (Docker image)
Release:  artifact + config → release (env-specific)
Run:      execute the release
```

Strict separation — **never edit running code**.

---

## Feature Flags

**Matlab:** Code production mein hai but **flag se on/off** — full deploy se decoupled feature release.

### Why?

- ✅ **Trunk-based dev** (incomplete features hidden)
- ✅ **Gradual rollout** (1% → 10% → 100%)
- ✅ **Instant kill switch** for buggy features
- ✅ **A/B testing** (50% see X, 50% see Y)
- ✅ **Per-user/region targeting**
- ✅ **Decouple deploy from release**

### Simple Flag (Code-level)

```java
if (featureFlags.isEnabled("new-checkout-flow", user)) {
    return newCheckoutHandler.handle(request);
} else {
    return oldCheckoutHandler.handle(request);
}
```

### Tools / Services

| Tool | Notes |
|------|-------|
| **LaunchDarkly** | Industry leader, SaaS, rich targeting |
| **Flagsmith** | OSS / SaaS, simpler |
| **Unleash** | OSS, self-host |
| **Spring Cloud Feature** (Spring) | Spring-native (community) |
| **Togglz** | Java library |
| **AWS AppConfig** | AWS-native |
| **GrowthBook** | OSS analytics + flags |

### LaunchDarkly Java Example

```java
LDClient client = new LDClient("sdk-key");

LDContext user = LDContext.builder("user-123")
    .set("country", "IN")
    .set("plan", "premium")
    .build();

if (client.boolVariation("new-pricing", user, false)) {
    // show new pricing
}
```

### Flag Types

- **Release flags** — temporary, removed after rollout
- **Experiment flags** — A/B test
- **Ops flags** — operational toggles (kill switch, throttling)
- **Permission flags** — premium/role-based features (long-lived)

### ⚠️ Flag Debt

Flags **temporary** by design. Stale flags accumulate → **delete** after rollout.

```java
// TODO LD-FLAG-123 (target 2026-Q2): remove flag after 100% rollout
```

---

## Deployment Strategies

### The Spectrum

```
Recreate (downtime)
   ↓
Rolling (gradual)
   ↓
Blue-Green (instant switch)
   ↓
Canary (gradual real traffic)
   ↓
Feature flags (per-user)
```

---

## Blue-Green Deployment

**Matlab:** **Do identical environments** — Blue (current prod) + Green (new version). Switch all traffic at once.

### Diagram

```
                ┌──────┐
   Traffic ──→ │  LB  │ ──→ Blue (v1.0)   ← current prod
                └──────┘     Green (v1.1) ← new, idle
                              ↑
                          deployed + tested

After validation:
                ┌──────┐
   Traffic ──→ │  LB  │ ──→ Green (v1.1)  ← now prod
                └──────┘     Blue (v1.0)  ← idle (rollback ready)
```

### Steps

1. Deploy v1.1 to Green env (parallel to Blue)
2. Run smoke tests against Green
3. **Switch LB** → all traffic to Green
4. Monitor; if bad → switch back to Blue (instant rollback!)
5. Eventually reuse Blue for next deploy

### Pros / Cons

| Pro | Con |
|-----|-----|
| Instant rollback (just flip switch) | 2x infrastructure cost during cutover |
| Simple mental model | Stateful issues (DB schema, sessions) |
| Zero downtime | Long-lived connections (WebSocket) drop |

### When?

- Critical apps where downtime expensive
- Fast rollback essential
- Have budget for double infrastructure briefly

---

## Canary Deployment

**Matlab:** Naya version ko **chhoti % traffic** ke liye release karo, monitor karo, slowly badhao.

### Diagram

```
                ┌──────┐    95% ──→ v1.0 (stable)
   Traffic ──→ │  LB  │
                └──────┘     5% ──→ v1.1 (canary)
                              ↑
                          monitor errors, latency

If healthy: 5% → 25% → 50% → 100%
If bad:    rollback canary instantly
```

### Steps

1. Deploy v1.1 alongside v1.0 (small instance)
2. Route **5%** traffic to v1.1
3. Monitor metrics: error rate, latency, business metrics
4. If healthy: increase gradually
5. If unhealthy: stop + rollback

### Pros / Cons

| Pro | Con |
|-----|-----|
| Real-traffic validation with limited blast radius | Complex routing/automation needed |
| Catches issues only seen at scale | Slow full rollout |
| Lower infra cost than blue-green | Some users hit bugs (the unlucky %) |

### Tools

- **Argo Rollouts** (Kubernetes)
- **Flagger** (Kubernetes)
- **Istio / Linkerd** (service mesh traffic split)
- **AWS App Mesh / ALB weighted target groups**
- **NGINX / HAProxy** weighted upstream

### Argo Rollouts Example

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: myapp
spec:
  strategy:
    canary:
      steps:
        - setWeight: 5
        - pause: { duration: 10m }
        - setWeight: 25
        - pause: { duration: 10m }
        - setWeight: 50
        - pause: { duration: 30m }
        - setWeight: 100
      analysis:
        templates:
          - templateName: success-rate
```

### When?

- High-stakes deploys
- Issues that only emerge at scale
- Have observability + auto-rollback

---

## Rolling Deployment

**Matlab:** Old pods/instances ko **dhire-dhire** new version se replace karo. Zero downtime, no extra capacity needed.

### Diagram

```
Initial: [v1.0] [v1.0] [v1.0] [v1.0]

Step 1:  [v1.1] [v1.0] [v1.0] [v1.0]   ← 1 replaced
Step 2:  [v1.1] [v1.1] [v1.0] [v1.0]
Step 3:  [v1.1] [v1.1] [v1.1] [v1.0]
Step 4:  [v1.1] [v1.1] [v1.1] [v1.1]   ← all done
```

### Kubernetes Default

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
spec:
  replicas: 4
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1    # at most 1 down
      maxSurge: 1          # at most 1 extra pod
```

### Pros / Cons

| Pro | Con |
|-----|-----|
| No extra cost | Slow rollback (must roll forward/back) |
| Default in K8s | Both versions live during rollout (compat issues!) |
| Simple | Bad version slowly poisons traffic |

### Backward Compatibility Required

During rollout, **both v1.0 and v1.1 serve traffic** — must coexist:

```
DB schema: backward compatible
API contract: no breaking changes mid-rollout
Cache keys: same format
```

---

## Comparison & When to Use What

| Strategy | Speed | Risk | Cost | Rollback |
|----------|-------|------|------|----------|
| **Recreate** | Fast | Downtime | Low | Re-deploy old |
| **Rolling** | Medium | Both versions live | Low | Roll forward/back |
| **Blue-Green** | Instant cutover | Stateful issues | 2x temp | Instant switch |
| **Canary** | Slow | Limited blast radius | +small | Stop + revert |
| **Feature Flags** | Per-user | Tiny per flag | Negligible | Toggle off |

### Decision Tree

```
Critical app, fast rollback?     → Blue-Green
Big company, gradual confidence?  → Canary
K8s default, simple service?     → Rolling
Decoupling deploy from release?   → Feature flags (combine with above)
Internal/dev tool?                → Recreate is fine
```

### Combine Strategies

Modern setups often:
- **Rolling deploy** as base
- **Canary** layer on top (Argo Rollouts)
- **Feature flags** for actual feature toggling

---

## Database Migration Strategies (Critical)

### Expand-Contract Pattern

For schema changes during rolling/blue-green:

```
Step 1: Expand — add new column (nullable)
        Both v1.0 and v1.1 work
Step 2: Backfill data
Step 3: Deploy v1.1 (uses new column)
Step 4: Verify all using new
Step 5: Contract — drop old column
```

Avoid: instant breaking schema changes during deploy.

### Migration Tools

- **Flyway** (Java)
- **Liquibase** (Java, XML/SQL/YAML)
- **Alembic** (Python)
- **Sequelize/Prisma** migrations (Node)

Run migrations as **separate step** before app deploy:

```
1. Run migrations (forward-compatible)
2. Deploy new app version
3. (Later) clean up old schema
```

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| **Promotion** | Same artifact dev → staging → prod |
| **12-factor config** | Env vars, not code |
| **Dev/prod parity** | Same backing services |
| **Feature flag** | Decouple deploy from release |
| **Blue-green** | 2 envs, instant switch, instant rollback |
| **Canary** | % traffic, gradual validation |
| **Rolling** | Replace instances one by one (K8s default) |
| **Argo Rollouts / Flagger** | K8s canary tools |
| **Expand-contract** | Safe DB schema changes |

---

## Practice

1. List config that differs between your dev/staging/prod — convert all to env vars.
2. Implement a simple feature flag in code (with env var) — toggle without redeploy.
3. Configure Kubernetes rolling deploy with `maxUnavailable: 0`.
4. Sketch blue-green setup using two ASGs + ALB target groups.
5. Implement canary using Argo Rollouts on local cluster.
6. Practice expand-contract: add a column without breaking running app.
