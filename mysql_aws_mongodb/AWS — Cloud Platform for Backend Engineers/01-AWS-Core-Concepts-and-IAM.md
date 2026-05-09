# AWS Core Concepts & IAM

## Status: Complete

---

## Table of Contents

1. [Global Infrastructure](#global-infrastructure)
2. [Regions](#regions)
3. [Availability Zones](#availability-zones)
4. [Edge Locations](#edge-locations)
5. [IAM Building Blocks](#iam-building-blocks)
6. [Policy JSON Anatomy](#policy-json-anatomy)
7. [Principle of Least Privilege](#principle-of-least-privilege)
8. [Service Control Policies (SCP)](#service-control-policies-scp)
9. [Instance Profiles](#instance-profiles)
10. [AWS Organizations](#aws-organizations)
11. [Pitfalls](#pitfalls)
12. [Cheat Sheet](#cheat-sheet)

---

## Global Infrastructure

> "AWS ka **physical/network footprint**: **Regions → AZs → Edge Locations**."

```
Region (ap-south-1, Mumbai)
   ├── AZ ap-south-1a (DC cluster)
   ├── AZ ap-south-1b
   └── AZ ap-south-1c

Edge Locations (400+ POPs worldwide — CloudFront, Route53)
```

---

## Regions

> "**Geographic area** — independent set of AZs. Data + services region-isolated by default."

Examples:

| Code | Name |
|------|------|
| `ap-south-1` | Mumbai |
| `ap-south-2` | Hyderabad |
| `us-east-1` | N. Virginia (oldest, biggest, most services first) |
| `eu-west-1` | Ireland |

### Why region matters

- **Data residency / compliance** (GDPR, RBI, etc.)
- **Latency** to user base
- **Service availability** — new services launch `us-east-1` first
- **Pricing** varies per region

### Cross-region

- **Not automatic** — you replicate (S3 CRR, Aurora Global, DynamoDB Global Tables)
- **Egress between regions = $$$**

---

## Availability Zones

> "**Isolated data center clusters** within a region — separate power/cooling/network — but low-latency private fiber between them (<2ms typical)."

Rules of thumb:

- **Production HA → use ≥ 2 AZs**
- DBs (RDS Multi-AZ, Aurora) span AZs
- ALB/NLB target multiple AZs

### AZ != Subnet

- AZ is **physical**
- Subnet is **logical** — each subnet lives in **exactly one AZ**

---

## Edge Locations

> "**Hundreds of POPs** globally — used by **CloudFront** (CDN), **Route 53** (DNS), **Global Accelerator**, **Lambda@Edge / CloudFront Functions**."

→ Closer to users than regions — for **latency-sensitive** static + dynamic content.

---

## IAM Building Blocks

| Entity | Meaning |
|--------|---------|
| **User** | Long-term identity (human or programmatic — but for humans prefer SSO) |
| **Group** | Collection of users — attach policies to group, not individual |
| **Role** | Assumed by entity (EC2/Lambda/ECS task/cross-account/federated user) — **temporary credentials** |
| **Policy** | JSON document of permissions — attached to user/group/role |

### Identity-based vs Resource-based policy

- **Identity-based**: attached to **principal** (user/role) — "you can do X"
- **Resource-based**: attached to **resource** (S3 bucket, SQS queue) — "X principals allowed on me"

---

## Policy JSON Anatomy

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowReadOnS3Bucket",
      "Effect": "Allow",
      "Action": ["s3:GetObject", "s3:ListBucket"],
      "Resource": [
        "arn:aws:s3:::my-bucket",
        "arn:aws:s3:::my-bucket/*"
      ],
      "Condition": {
        "StringEquals": { "aws:RequestedRegion": "ap-south-1" },
        "IpAddress": { "aws:SourceIp": "203.0.113.0/24" }
      }
    }
  ]
}
```

### Field meaning

| Field | Use |
|-------|-----|
| **Effect** | `Allow` / `Deny` (Deny always wins) |
| **Action** | API actions (`s3:GetObject`, `dynamodb:PutItem`) |
| **Resource** | ARN(s) — `*` for all, but minimize |
| **Principal** | (resource-based only) Who is granted |
| **Condition** | Extra checks — IP, MFA, tag, region |

### Wildcards

- `s3:Get*` — all Get APIs
- `arn:aws:s3:::*` — all buckets — usually too broad
- Prefer **explicit** ARNs in production

---

## Principle of Least Privilege

> "**Sirf utna access do jitna kaam ke liye chahiye, na ek action zyada.**"

Practical workflow:

1. Start from **AWS-managed read-only** policy
2. Run actual workload, capture **CloudTrail** API calls
3. Use **IAM Access Analyzer** to **generate policy** from history
4. Replace broad with **narrow scoped** policy
5. Re-review every **90 days** — drift inevitable

### Anti-patterns

- Attaching **`AdministratorAccess`** to app role
- One **shared user** for entire team
- Long-lived **access keys** in code/Git

---

## Service Control Policies (SCP)

> "**Org-wide guardrails** — limit max permissions per account, even if account admin tries more."

- Lives in **AWS Organizations**
- **Doesn't grant** permissions — only **limits** what IAM in member accounts can grant
- Common uses:
  - Deny **regions** outside `ap-south-1` / `ap-south-2`
  - Deny **disabling CloudTrail** / GuardDuty
  - Deny **public S3 bucket** creation
  - Deny root access keys

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "DenyOtherRegions",
    "Effect": "Deny",
    "Action": "*",
    "Resource": "*",
    "Condition": {
      "StringNotEquals": {
        "aws:RequestedRegion": ["ap-south-1", "ap-south-2"]
      }
    }
  }]
}
```

---

## Instance Profiles

> "**Role attached to EC2 instance** — code on instance gets **temporary AWS credentials** auto-rotated via IMDSv2."

```
EC2 → IMDSv2 → temporary creds (expires ~6h, refreshed)
```

Why important:

- **No keys on disk**
- Compromised key = limited blast radius
- Same pattern: **ECS task role**, **Lambda execution role**, **EKS IRSA** (IAM Roles for Service Accounts)

### IMDSv2 (always require)

- v1 = unauthenticated, GET — SSRF abuse possible
- v2 = session-token + PUT first — block SSRF
- Set **`HttpTokens=required`** on launch

---

## AWS Organizations

> "**Multi-account strategy** — root org + OUs (Organizational Units) + member accounts."

Typical structure:

```
Root
├── Security OU
│   ├── Audit (CloudTrail aggregation)
│   └── Log Archive
├── Workloads OU
│   ├── prod-account
│   ├── staging-account
│   └── dev-account
└── Sandbox OU (per-developer)
```

Benefits:

- **Blast radius isolation** — prod compromise ≠ dev compromise
- **Per-account billing** clarity
- **SCP** enforcement
- **Consolidated billing** (volume discounts)

### IAM Identity Center (formerly SSO)

- Central identity → roles in all accounts
- No per-account IAM users for humans

---

## Pitfalls

1. **Root account daily use** — only use root for billing/account-level tasks; enable MFA + lock away.
2. **Long-lived access keys** in `~/.aws/credentials` — prefer **SSO** sessions or **roles**.
3. **`*` in Action and Resource** — recipe for blast radius.
4. **Forgetting region scope** — services region-isolated; resources "missing" because wrong region selected.
5. **AZ confusion** — same physical AZ has **different name** per account (AZ ID `aps1-az1` is constant, AZ name `ap-south-1a` varies per account).
6. **No MFA on root** — audit fail.
7. **Not enabling CloudTrail** in all regions / accounts.
8. **SCP test in prod first** — lockout horror; test in dev OU.

---

## Cheat Sheet

| Concept | Quick |
|---------|-------|
| Region | Geographic group |
| AZ | Isolated DC cluster (≥2 for HA) |
| Edge Location | CloudFront/R53 POP |
| User | Long-term identity |
| Group | Bag of users |
| Role | Assumable, temporary creds |
| Policy | JSON Effect/Action/Resource/Condition |
| SCP | Org-level deny guardrails |
| Instance profile | Role on EC2 |
| Organizations | Multi-account hierarchy |

---

## Practice

1. Write a policy: read-only on **one** S3 bucket, only from your office IP, only with MFA.
2. Enable **MFA** on root + an IAM user; experience the diff.
3. Configure **AWS CLI** with **SSO** profile (not access key).
4. Draft an SCP that **denies** any region outside India.
5. Attach a **role** to an EC2 → from instance run `aws s3 ls` without keys.
