# AWS — Cloud Platform for Backend Engineers

AWS ki **deep mastery** backend engineer ke perspective se — IAM, EC2, VPC, RDS/Aurora, S3, Lambda, API Gateway, SQS/SNS/EventBridge, ECS/EKS, CloudWatch, ElastiCache, CloudFront/Route53, CI/CD, Cost. Hinglish + CLI/Console examples + cost notes + pitfalls.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | AWS Core Concepts & IAM | [01-AWS-Core-Concepts-and-IAM.md](./01-AWS-Core-Concepts-and-IAM.md) | Complete |
| 2 | EC2 & Compute | [02-EC2-and-Compute.md](./02-EC2-and-Compute.md) | Complete |
| 3 | VPC & Networking | [03-VPC-and-Networking.md](./03-VPC-and-Networking.md) | Complete |
| 4 | RDS & Aurora | [04-RDS-and-Aurora.md](./04-RDS-and-Aurora.md) | Complete |
| 5 | S3 — Object Storage | [05-S3-Object-Storage.md](./05-S3-Object-Storage.md) | Complete |
| 6 | Lambda & Serverless | [06-Lambda-and-Serverless.md](./06-Lambda-and-Serverless.md) | Complete |
| 7 | API Gateway | [07-API-Gateway.md](./07-API-Gateway.md) | Complete |
| 8 | SQS & SNS & EventBridge | [08-SQS-SNS-EventBridge.md](./08-SQS-SNS-EventBridge.md) | Complete |
| 9 | ECS, EKS & Container Orchestration | [09-ECS-EKS-Container-Orchestration.md](./09-ECS-EKS-Container-Orchestration.md) | Complete |
| 10 | CloudWatch & Observability | [10-CloudWatch-and-Observability.md](./10-CloudWatch-and-Observability.md) | Complete |
| 11 | ElastiCache (Redis & Memcached) | [11-ElastiCache-Redis-Memcached.md](./11-ElastiCache-Redis-Memcached.md) | Complete |
| 12 | CloudFront & Route 53 | [12-CloudFront-and-Route53.md](./12-CloudFront-and-Route53.md) | Complete |
| 13 | CI/CD on AWS | [13-CI-CD-on-AWS.md](./13-CI-CD-on-AWS.md) | Complete |
| 14 | Cost Optimization | [14-Cost-Optimization.md](./14-Cost-Optimization.md) | Complete |

---

## What's Inside Each File?

### [01 — AWS Core Concepts & IAM](./01-AWS-Core-Concepts-and-IAM.md)
**Regions** (geographic — `ap-south-1` Mumbai), **AZs** (isolated DCs — 2+ for HA), **Edge Locations** (CloudFront). **IAM** (Users/Groups/Roles/Policies — JSON Effect/Action/Resource/Condition), **least privilege**, **SCPs**, **instance profiles**, **AWS Organizations**.

### [02 — EC2 & Compute](./02-EC2-and-Compute.md)
Instance families (**t3/m6i**, **c6i**, **r6i**, **i3**), purchasing models (**On-Demand**, **Reserved**, **Spot**), **ASG** (launch templates, scaling policies), **Placement Groups** (cluster/spread/partition).

### [03 — VPC & Networking](./03-VPC-and-Networking.md)
**VPC**, **CIDR**, **public vs private subnets**, **IGW**, **NAT Gateway** (cost!), **route tables**, **SG vs NACL** (stateful vs stateless), **VPC Peering**, **VPC Endpoints**, **PrivateLink**.

### [04 — RDS & Aurora](./04-RDS-and-Aurora.md)
RDS managed engines, **Multi-AZ** (HA, sync standby) vs **Read Replicas** (async, scale reads), **parameter/option groups**, **automated backups + PITR**, **RDS Proxy** (Lambda pooling), **Aurora** (6-way replication, up to 15 read replicas, **Aurora Serverless v2**).

### [05 — S3 Object Storage](./05-S3-Object-Storage.md)
**Buckets** (global names), **objects** (5TB max, multipart >100MB), **storage classes** (Standard / Standard-IA / One Zone-IA / Intelligent-Tiering / Glacier tiers), **lifecycle policies**, **versioning + MFA delete**, **static website hosting**, **pre-signed URLs**, **S3 Transfer Acceleration**, **Object Lock (WORM)**.

### [06 — Lambda & Serverless](./06-Lambda-and-Serverless.md)
Event-driven (15min max), **cold start** + **Provisioned Concurrency**, **concurrency limits**, **triggers**, **layers**, **container images** (10GB), **Lambda@Edge / CloudFront Functions**, cost model, **Power Tuning**.

### [07 — API Gateway](./07-API-Gateway.md)
**REST API** vs **HTTP API** (cheaper) vs **WebSocket API**, stages + variables, **canary deployments**, mapping templates (VTL), **throttling**, **response caching**, **custom domain + ACM**, **usage plans + API keys**, integration types.

### [08 — SQS, SNS & EventBridge](./08-SQS-SNS-EventBridge.md)
**SQS Standard** vs **FIFO** (3000 TPS batched), **DLQ**, **visibility timeout**, **long polling**, retention. **SNS** pub/sub fan-out (SQS/Lambda/HTTP/email/SMS). **EventBridge** event buses, rules, scheduled rules, custom buses, **Schema Registry**.

### [09 — ECS, EKS & Containers](./09-ECS-EKS-Container-Orchestration.md)
**ECS** (Fargate vs EC2 launch type), **task definitions**, **services + ALB + autoscaling**. **EKS** (managed K8s control plane), **Fargate for EKS**, **ECR**, **Service Connect** mesh.

### [10 — CloudWatch & Observability](./10-CloudWatch-and-Observability.md)
**Metrics** (built-in + custom), **Logs** (groups/streams, **metric filters**, **Logs Insights**), **Alarms** (threshold/anomaly/composite), **Dashboards**, **X-Ray** tracing, **Container Insights**, **Lambda Insights**, **CloudTrail** audit.

### [11 — ElastiCache (Redis & Memcached)](./11-ElastiCache-Redis-Memcached.md)
**Redis** data structures (String/Hash/List/Set/ZSet/Stream/Bitmap), persistence (RDB/AOF), **Pub/Sub**, **Lua**, **cluster mode**, **replication**, **Global Datastore**, **ElastiCache Serverless**. **Memcached** (multi-thread, cache-only).

### [12 — CloudFront & Route 53](./12-CloudFront-and-Route53.md)
**CloudFront** edge CDN, origins, behaviors, **cache key tuning**, **Origin Shield**, **Lambda@Edge / CloudFront Functions**, **signed URLs/cookies**. **Route 53** DNS routing policies (Simple, Weighted, Latency, Failover, Geolocation, Multivalue).

### [13 — CI/CD on AWS](./13-CI-CD-on-AWS.md)
**CodeCommit** (deprecated → GitHub/GitLab), **CodeBuild** (`buildspec.yml`), **CodeDeploy** (blue/green, EC2/ECS/Lambda), **CodePipeline** orchestration, **GitHub Actions + OIDC**, **ECR scanning**, **CDK / CloudFormation / Terraform**.

### [14 — Cost Optimization](./14-Cost-Optimization.md)
**Cost Explorer**, **Budgets**, **Trusted Advisor**, **Compute Optimizer**, **Savings Plans** vs **Reserved**, **S3 Intelligent-Tiering**, **data transfer (egress) costs**, **Spot for batch**, hygiene (unused EBS/EIP/snapshots).

---

## Recommended Order

```
1. Core & IAM            ← foundational vocabulary + security
2. VPC & Networking      ← everything else lives in a VPC
3. EC2 & Compute         ← classic compute primitives
4. S3                    ← storage layer + cost
5. RDS & Aurora          ← managed relational
6. ElastiCache           ← caching layer
7. Lambda                ← serverless compute
8. API Gateway           ← public surface for Lambda/HTTP
9. SQS/SNS/EventBridge   ← async messaging glue
10. ECS/EKS              ← container orchestration
11. CloudFront & Route53 ← CDN + DNS
12. CloudWatch           ← observability + alerts
13. CI/CD                ← shipping pipelines
14. Cost Optimization    ← FinOps discipline
```

---

## Companion Folders

- [MySQL — Relational Database Mastery](../MySQL%20%E2%80%94%20Relational%20Database%20Mastery/) — relational deep dive
- [MongoDB — Document Database Mastery](../MongoDB%20%E2%80%94%20Document%20Database%20Mastery/) — NoSQL deep dive
- [System Design](../../phase-5-interview-preparation/System%20Design%20Interviews/) — picking AWS services for design

---

## Quick Reference

| Need | Service |
|------|---------|
| Run servers | EC2 / ECS / EKS / Lambda |
| Object storage | S3 |
| Relational DB | RDS / Aurora |
| In-memory cache | ElastiCache |
| Async queue | SQS |
| Pub/sub fanout | SNS / EventBridge |
| HTTP front door | API Gateway / ALB / CloudFront |
| DNS | Route 53 |
| Logs/metrics/traces | CloudWatch + X-Ray |
| CI/CD | CodePipeline + CodeBuild + CodeDeploy / GitHub Actions |
| Identity | IAM (+ SSO / Cognito for end-users) |

---

## Status Tracker

```
[x] 01 — Core & IAM
[x] 02 — EC2 & Compute
[x] 03 — VPC & Networking
[x] 04 — RDS & Aurora
[x] 05 — S3
[x] 06 — Lambda
[x] 07 — API Gateway
[x] 08 — SQS / SNS / EventBridge
[x] 09 — ECS / EKS
[x] 10 — CloudWatch
[x] 11 — ElastiCache
[x] 12 — CloudFront / Route53
[x] 13 — CI/CD
[x] 14 — Cost Optimization
```

> "AWS = **Lego blocks**. Backend engineer ka kaam: **right blocks** + **least privilege** + **cost-aware** combos design karna. Hardware nahi — **architecture** decide karta hai bill."
