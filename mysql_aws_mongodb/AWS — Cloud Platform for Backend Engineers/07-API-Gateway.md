# API Gateway

## Status: Complete

---

## Table of Contents

1. [What is API Gateway](#what-is-api-gateway)
2. [REST vs HTTP vs WebSocket API](#rest-vs-http-vs-websocket-api)
3. [Stages & Stage Variables](#stages--stage-variables)
4. [Canary Deployments](#canary-deployments)
5. [Request/Response Transformation](#requestresponse-transformation)
6. [Throttling](#throttling)
7. [Caching](#caching)
8. [Custom Domains + ACM](#custom-domains--acm)
9. [Usage Plans & API Keys](#usage-plans--api-keys)
10. [Integration Types](#integration-types)
11. [Authorization Options](#authorization-options)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## What is API Gateway

> "**Managed front door** for your APIs — receives HTTP/WebSocket requests, optionally transforms, authenticates, throttles, caches, then forwards to backend (Lambda, ECS, EC2, on-prem, AWS service)."

Replaces:

- Roll-your-own NGINX in front of microservices
- Custom rate limiter
- Custom auth layer
- DIY WebSocket server

---

## REST vs HTTP vs WebSocket API

| Feature | **REST API** | **HTTP API** | **WebSocket API** |
|---------|--------------|--------------|-------------------|
| Released | 2015 | 2019 (newer) | 2018 |
| Cost | Higher | **~70% cheaper** | Per-msg + connection-min |
| Latency | ~30 ms overhead | **Lower (~10 ms)** | Persistent connection |
| Auth | Cognito, IAM, custom Lambda | JWT (Cognito/OIDC), IAM, Lambda authorizer | IAM, Lambda |
| Mapping templates (VTL) | **Yes** | No (use Lambda) | No |
| Request validation | Yes | Limited | No |
| Caching | Yes | No | n/a |
| Usage plans + API keys | Yes | **No** | No |
| WAF | Yes | Yes (since recent) | n/a |
| X-Ray | Yes | Yes | Yes |

### When to pick

- **HTTP API**: most modern Lambda/HTTP backends — **default choice**
- **REST API**: need VTL transforms, API keys/usage plans, request validation, response caching
- **WebSocket API**: real-time chat, live notifications, gaming, collaborative apps

---

## Stages & Stage Variables

> "**Stage** = deployed version of API at a URL prefix (`/dev`, `/staging`, `/prod`)."

```
https://abc123.execute-api.ap-south-1.amazonaws.com/prod/users
                                                    └─ stage
```

### Stage variables

> "**Per-stage config**: target Lambda alias, backend URL, custom auth header — without changing API definition."

```
Stage prod:
  lambdaAlias = prod
  backendUrl  = https://api.internal.prod.example.com

Stage dev:
  lambdaAlias = dev
  backendUrl  = https://api.internal.dev.example.com
```

Reference in integration: `${stageVariables.lambdaAlias}`.

---

## Canary Deployments

> "**Send X% of traffic** to a new deployment, rest to current. Roll out safely."

REST API only:

```bash
# 10% canary on prod stage
aws apigateway create-deployment --rest-api-id ... \
  --stage-name prod \
  --canary-settings 'percentTraffic=10,useStageCache=true'
```

Promote when confident:

```bash
aws apigateway update-stage --rest-api-id ... --stage-name prod \
  --patch-operations op=copy,from=/canarySettings/deploymentId,path=/deploymentId
```

---

## Request/Response Transformation

> "**Velocity Template Language (VTL)** mapping templates** — transform request before backend, transform response before client. REST API only.**"

### Example: pull from query string + add header

```vtl
#set($name = $input.params('name'))
{
  "userName": "$name",
  "tenantId": "$context.authorizer.claims.tenant"
}
```

### When to avoid VTL

- Complex logic — **let Lambda handle** instead (Lambda Proxy integration)
- Hard to test, hard to debug
- HTTP API doesn't support — forces cleaner pattern

---

## Throttling

> "**Rate (req/sec)** + **Burst (concurrent)** limits at account, stage, method, or per API key."

| Level | Default |
|-------|---------|
| **Account-level** | 10,000 RPS, 5,000 burst (per region) — request increase |
| **Per-stage / per-method** | Configurable override |
| **Per API key (Usage Plan)** | Configurable |

### Algorithm

- **Token bucket** — burst fills bucket, sustained fills at rate

### Behavior

- Over throttle → **429 Too Many Requests**
- Always **plan for back-pressure** in clients (retries with jitter)

---

## Caching

> "**Response cache** at API Gateway — TTL per method/path. Reduces backend load + latency."

REST API only:

- Cache size: **0.5 GB → 237 GB**
- TTL: 0–3600 s (default 300)
- Cache key includes URL + headers + query strings (configurable)
- Invalidate via `Cache-Control: max-age=0` header (with proper IAM permission)

### Cost vs benefit

- Cache cluster has **per-hour cost**
- Worth it for read-heavy idempotent endpoints with stable cache keys
- For dynamic per-user — use **CloudFront** instead

---

## Custom Domains + ACM

> "**Map your domain** (`api.example.com`) to API Gateway."

Steps:

1. Register cert in **ACM** (must be in **`us-east-1`** for **edge-optimized** APIs; same region for regional)
2. Create **Custom Domain Name** in API GW → attach cert
3. Create **API mapping** → which API + stage at which base path
4. Route 53: `A record` (alias) → API GW domain

### Endpoint types

- **Edge-optimized** — uses CloudFront globally (default REST)
- **Regional** — direct, no CloudFront
- **Private** — only accessible via VPC Endpoint (PrivateLink)

---

## Usage Plans & API Keys

> "**REST API only.** Distribute API keys to consumers, throttle / quota each."

Components:

- **API Key** — unique string per consumer
- **Usage Plan** — rate, burst, quota (e.g., 1000 req/day) → bound to stages

### Use cases

- Public APIs with **tiered pricing** (Free / Pro / Enterprise)
- Per-customer quotas
- Track usage per key (CloudWatch metrics by API key)

### Note

- API keys != auth — they identify, **don't authenticate strongly**
- Always combine with proper auth (Cognito/JWT/IAM)

---

## Integration Types

| Type | Backend | Notes |
|------|---------|-------|
| **Lambda Proxy** | Lambda — passes raw event | Most common, simplest |
| **Lambda (non-proxy)** | Lambda + VTL transform | Old style, needs templates |
| **HTTP Proxy** | Any HTTP endpoint (ALB, NLB, on-prem via VPC Link) | |
| **HTTP (non-proxy)** | HTTP + VTL transform | |
| **AWS Service** | Direct call to AWS API (e.g., put SQS msg, no Lambda) | Cheap, simple cases |
| **Mock** | No backend, return canned response | Testing, OPTIONS for CORS |

### VPC Link

- For HTTP API: connects to private NLB in VPC
- For REST API: VPC Link via NLB
- Lets API Gateway call **private** ALB/NLB / on-prem (via VPN/Direct Connect)

---

## Authorization Options

| Option | How |
|--------|-----|
| **None** | Public endpoint |
| **IAM** | Caller signs with SigV4 (AWS-to-AWS) |
| **Cognito User Pool** | JWT issued by Cognito |
| **JWT (HTTP API)** | Any OIDC issuer (Auth0, Okta, Cognito) |
| **Lambda Authorizer (Custom)** | Function returns IAM policy + context |
| **Resource Policy** | Restrict by source IP / VPC / account |

### Lambda Authorizer

Two types:

- **Token** (JWT in header) — cacheable
- **Request** (any params) — flexible, slower

```javascript
export const handler = async (event) => {
  const token = event.headers.authorization;
  const decoded = verifyJwt(token);
  return {
    principalId: decoded.sub,
    policyDocument: {
      Version: "2012-10-17",
      Statement: [{ Effect: "Allow", Action: "execute-api:Invoke", Resource: event.routeArn }]
    },
    context: { tenant: decoded.tenant }
  };
};
```

Cache responses (default 5 min) to reduce auth Lambda cost.

---

## Pitfalls

1. **Choosing REST when HTTP would do** — paying 70% more for unused features.
2. **VTL spaghetti** — move logic to Lambda.
3. **No throttle on free tier** of public API — DDoS friendly.
4. **API key as auth** — not authentication, only identification.
5. **No CORS** configured → browser sees `Access-Control-Allow-Origin` errors. Configure on API Gateway.
6. **No timeout tuning** — default integration timeout 29 s for REST/HTTP — plan accordingly.
7. **Edge-optimized cert** in wrong region — must be `us-east-1`.
8. **Cache TTL too high** for personalized data — wrong user sees other's data.
9. **Lambda Authorizer not cached** — auth function called per request → bill + latency.
10. **No X-Ray** — debugging multi-service request impossible.

---

## Cheat Sheet

| Need | Choose |
|------|--------|
| Modern Lambda backend | **HTTP API** |
| API keys / quotas / VTL | **REST API** |
| Real-time bi-directional | **WebSocket API** |
| Public domain | Custom Domain + ACM |
| Auth via SaaS IdP | JWT authorizer (HTTP) |
| AWS-to-AWS | IAM auth (SigV4) |
| Reduce backend load | Caching (REST) or CloudFront |
| Safe rollout | **Canary** stage |
| Per-env config | Stage variables |
| Private API | Private endpoint + VPC Endpoint |

---

## Practice

1. Build **HTTP API + Lambda** for a hello endpoint with **JWT** authorizer (Cognito).
2. Compare **REST vs HTTP** for same workload — measure latency + cost.
3. Add **canary** to REST stage, route 10% traffic to new Lambda alias.
4. Configure **usage plan** with 100 req/min per API key.
5. Set up **custom domain** `api.example.com` → API GW with ACM cert.
6. Implement **Lambda Authorizer** with caching; verify call-count drops.
