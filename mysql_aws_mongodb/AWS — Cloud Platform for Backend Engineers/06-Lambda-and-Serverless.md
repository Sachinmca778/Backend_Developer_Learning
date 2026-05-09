# Lambda & Serverless

## Status: Complete

---

## Table of Contents

1. [What is Lambda](#what-is-lambda)
2. [Execution Model](#execution-model)
3. [Cold Starts](#cold-starts)
4. [Provisioned Concurrency](#provisioned-concurrency)
5. [Concurrency Limits](#concurrency-limits)
6. [Triggers](#triggers)
7. [Lambda Layers](#lambda-layers)
8. [Container Image Support](#container-image-support)
9. [Lambda@Edge & CloudFront Functions](#lambdaedge--cloudfront-functions)
10. [Cost Model](#cost-model)
11. [Power Tuning](#power-tuning)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## What is Lambda

> "**Run code without managing servers.** Pay per ms execution + memory. Scales 0 → thousands automatically."

Languages: Node.js, Python, Java, Go, .NET, Ruby, custom runtime via container image.

Limits to remember:

| Limit | Value |
|-------|-------|
| Max execution time | **15 minutes** |
| Memory | **128 MB → 10 GB** (CPU scales with memory) |
| Ephemeral `/tmp` | **512 MB → 10 GB** |
| Deployment package (zip) | **50 MB zipped, 250 MB unzipped** |
| Container image | **10 GB** |
| Env vars total size | 4 KB |
| Default concurrent executions | **1000 per region** (request increase) |

---

## Execution Model

> "**Event → Function instance → Response**. Each instance handles **one request at a time** (no in-process concurrency)."

```
1000 concurrent requests → 1000 instances of your function
```

Lifecycle of a single instance:

```
INIT (cold start)
  ├── Download code
  ├── Bootstrap runtime
  └── Run handler module-level code
INVOKE (warm)
  └── Run handler() per event (reused for many invokes)
SHUTDOWN (after idle ~15 min, or scale-down)
```

### Best practices

- Initialize **DB clients, HTTP clients, KMS clients** at **module level** — reused across warm invokes
- Don't open DB connection per invoke — bad
- Use **AWS SDK clients with `keepAlive: true`** — reuse TCP connections

```javascript
// outside handler — runs once per cold start
const dynamo = new DynamoDBClient({ region: "ap-south-1" });

export const handler = async (event) => {
  // reuse `dynamo`
  const out = await dynamo.send(new GetItemCommand({...}));
  return out;
};
```

---

## Cold Starts

> "**First request to a fresh instance** = init time + handler time. Latency-sensitive APIs feel this."

Typical cold start times (rough):

| Runtime | Cold start |
|---------|-----------|
| Node.js, Python | 100–400 ms |
| Go, Rust (custom runtime) | <100 ms |
| Java, .NET | 500 ms – 2 s+ (JVM init) |
| Container image | 200 ms – several seconds (depends on size) |

### Reduce cold starts

- **Provisioned Concurrency** (pre-warm)
- **SnapStart** for **Java** (snapshot of initialized JVM, near-zero cold start)
- **Smaller deployment** (drop unused deps, tree-shake)
- **Don't import giant SDKs** — use modular imports (AWS SDK v3)
- **Connection reuse** at module scope

### When to live with cold start

- Async / event-driven workloads (S3, SQS) — user not waiting
- Background jobs

---

## Provisioned Concurrency

> "**Pre-initialized instances** ready to serve. No cold start. **Pay per provisioned hour** even if idle."

Use cases:

- **API Gateway → Lambda** with strict p99 latency
- **Synchronous user-facing** Lambda

Configure on **alias / version** (not `$LATEST`):

```bash
aws lambda put-provisioned-concurrency-config \
  --function-name myFn --qualifier prod \
  --provisioned-concurrent-executions 50
```

### Auto-scaling provisioned concurrency

- Application Auto Scaling target tracking on utilization

---

## Concurrency Limits

| Knob | Use |
|------|-----|
| **Account concurrent executions** | Region-wide soft limit (default 1000) |
| **Reserved Concurrency** | **Caps** function at N — also **reserves** that pool from account budget |
| **Provisioned Concurrency** | Pre-warmed pool (subset of reserved) |

### Why reserve?

- Protect downstream (RDS) from being flooded — `Reserved=50` = max 50 DB conns
- Isolate noisy neighbor — function A spike won't starve function B
- **Throttle** runaway function explicitly (`Reserved=0` = disabled)

### When throttled

- Sync invoke → **429 TooManyRequests** to caller
- Async invoke → retried + DLQ
- Stream (Kinesis/DDB) → backpressure

---

## Triggers

| Service | Pattern | Notes |
|---------|---------|-------|
| **API Gateway / ALB** | HTTP request → response | Sync, user-facing |
| **S3** | Object created/deleted | Async, eventual |
| **SQS** | Poll + invoke (batched) | At-least-once; visibility timeout >= function timeout × 6 |
| **DynamoDB Streams / Kinesis** | Shard-based, ordered per shard | Batch size, parallelization factor |
| **EventBridge** | Pattern / scheduled | Cron jobs, decoupled events |
| **SNS** | Pub/sub fan-out | Async |
| **Cognito / Step Functions / IoT / etc.** | Many | |

### Sync vs Async vs Stream

| Type | Retry | Where errors go |
|------|-------|-----------------|
| **Sync** (API GW, ALB) | Caller retries | Response with error |
| **Async** (S3, SNS, EventBridge) | 2 retries default | **Lambda DLQ** or **on-failure destination** |
| **Stream/Poll** (SQS, Kinesis, DDB) | Retries until visibility expiry / shard checkpoint | DLQ / **bisect batch** for poison pills |

---

## Lambda Layers

> "**Reusable shared dependency package** — uploaded once, attached to many functions."

```
my-function.zip
my-shared-deps-layer.zip   ← attach to N functions
```

### Use

- Common libraries (boto3 helper, internal SDK)
- **Latest AWS SDK** override (built-in version may lag)
- **Native binaries** (libpq, ffmpeg, sharp)

### Limits

- **5 layers max** per function
- Combined unzipped size + function code ≤ **250 MB**

---

## Container Image Support

> "Package Lambda as **Docker image** (10 GB) — useful for big ML models, complex deps, custom runtimes."

```dockerfile
FROM public.ecr.aws/lambda/python:3.12
COPY app.py requirements.txt ./
RUN pip install -r requirements.txt
CMD ["app.handler"]
```

### vs ZIP

| | ZIP | Container |
|--|-----|-----------|
| Size | 250 MB | 10 GB |
| Cold start | Faster | Bigger images = slower (mitigated since 2023) |
| Tooling | AWS console / CLI | Standard Docker / ECR |
| Debugging local | sam local | docker run |

---

## Lambda@Edge & CloudFront Functions

| | Lambda@Edge | CloudFront Functions |
|--|-------------|----------------------|
| Runtime | Node.js / Python | JavaScript (subset of ES) |
| Trigger | viewer/origin request/response | viewer request/response only |
| Max execution | up to 30 s (origin events) / 5 s (viewer) | **<1 ms**, 10 KB memory |
| Use | Auth at edge, A/B routing, header rewrites | Header manipulation, redirects, URL rewrites at scale |
| Cost | Higher | Much cheaper, designed for high-volume |

### Use cases

- **Edge auth** (validate JWT before hitting origin)
- **A/B testing** routing
- **URL canonicalization / redirect**
- **Header injection** (security headers)

---

## Cost Model

```
Cost = Requests × $/req + GB-seconds × $/GB-s
```

- **Requests**: $0.20 per 1M (after free tier 1M/mo)
- **GB-seconds**: depends region, ~$0.0000167 per GB-s
- **Provisioned Concurrency**: hourly + reduced GB-s rate
- **Free tier**: 1M req + 400,000 GB-s per month — substantial

### Optimization

- **Reduce duration** (faster code, async I/O parallelism)
- **Right-size memory** (more memory = more CPU but more $/sec — there's a sweet spot)
- **Avoid synchronous chains** of Lambdas — each waits = pays for waiting

---

## Power Tuning

> "**AWS Lambda Power Tuning** (open-source by Alex Casalboni) — Step Functions tool that runs your function at multiple memory sizes, plots **cost vs performance**."

```
Test memory sizes: 128, 512, 1024, 1536, 3008, 5120, 10240
→ Find optimal for cost / speed / balance
```

Often counter-intuitive: **higher memory = faster + cheaper** (because CPU scales).

---

## Pitfalls

1. **DB connections in handler** — connection storm, RDS dies. Use **RDS Proxy** or module-scoped client.
2. **`$LATEST` with provisioned concurrency** — not allowed; use alias/version.
3. **Long Lambda chains** — each waits for next, paying double. Use Step Functions or async.
4. **No DLQ on async invoke** — silent failures.
5. **Logs not retained** — default CloudWatch Logs no expiry; or set retention to control cost.
6. **Container image not optimized** — bloated cold start.
7. **Polling SQS with visibility timeout = Lambda timeout** — duplicates on retry. Use **6× rule**.
8. **No reserved concurrency on critical fn** — neighbor spike starves.
9. **Storing state in `/tmp`** — only for hot instance lifetime.
10. **Overusing Lambda for steady high QPS** — Fargate/EC2 cheaper at constant load.

---

## Cheat Sheet

| Issue | Solution |
|-------|----------|
| Cold start latency | Provisioned Concurrency / SnapStart / smaller package |
| DB conn storm | RDS Proxy + module-level client |
| Throttled function | Increase reserved or account limit |
| Async failure visibility | Destinations / DLQ |
| Heavy ML model | Container image |
| Cron job | EventBridge schedule |
| HTTP front | API Gateway HTTP API or ALB |
| Edge logic | CloudFront Functions |

| Limit | Value |
|-------|-------|
| Timeout | 15 min |
| Memory | 10 GB |
| /tmp | 10 GB |
| Concurrency default | 1000 / region |

---

## Practice

1. Write a Lambda with module-scoped DynamoDB client; load test 100 concurrent.
2. Enable **Provisioned Concurrency** on alias `prod`; measure p99 vs default.
3. Run **Power Tuning** state machine on a CPU-bound function.
4. Configure **SQS trigger** with batch size 10, visibility 6× timeout, DLQ after 3 receives.
5. Build a **container image** Lambda with Python + heavy ML lib; deploy via ECR.
6. Add **CloudFront Functions** for HTTP-to-HTTPS redirect + security headers.
