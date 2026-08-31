# AWS — Cloud Platform for Backend Engineers - Top 10 Interview Questions & Answers

> Covering: AWS Core Concepts, IAM, EC2, VPC, RDS/Aurora, S3, Lambda, API Gateway, SQS/SNS/EventBridge, ECS/EKS, CloudWatch, ElastiCache, CloudFront, Route 53, CI/CD, and Cost Optimization

---

## Q1: AWS Region, AZ aur Edge Location mein kya difference hai?

**Answer:**

AWS global infrastructure 3 core concepts par based hai:

| Concept | Meaning | Example |
|---------|---------|---------|
| **Region** | Geographic area | `ap-south-1` Mumbai |
| **Availability Zone (AZ)** | Isolated data center cluster inside region | `ap-south-1a` |
| **Edge Location** | CDN/DNS edge near users | CloudFront/Route 53 |

### High Availability:

Production workloads ko multiple AZs mein deploy karo:

```text
Region: ap-south-1
  AZ-1: app + DB standby
  AZ-2: app + DB primary/replica
```

### Edge:

CloudFront edge locations user ke paas content serve karte hain, latency aur origin load reduce hota hai.

**Short Interview Answer:**

Region is a geographic AWS area, AZ is an isolated data-center zone inside a region, and Edge Location is used by services like CloudFront/Route 53 to serve users closer to them. For HA, deploy across multiple AZs; for global latency, use edge services.

---

## Q2: IAM mein User, Group, Role aur Policy kya hote hain?

**Answer:**

IAM AWS access control ka foundation hai.

| IAM Entity | Use |
|------------|-----|
| User | Human/long-term identity (prefer SSO now) |
| Group | Users ka collection |
| Role | Temporary credentials for AWS services/apps |
| Policy | JSON permissions document |

### Policy Example:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject"],
      "Resource": "arn:aws:s3:::my-bucket/*"
    }
  ]
}
```

### Least Privilege:

App ko sirf required actions do. Example:
- EC2 instance → IAM Role via instance profile
- Lambda → execution role
- ECS Task → task role

**Important:**

Hardcoded access keys avoid karo. AWS services ke liye IAM Roles use karo. Humans ke liye IAM Identity Center/SSO preferred hai.

---

## Q3: VPC, Subnet, Route Table, Security Group aur NACL kya hote hain?

**Answer:**

VPC AWS mein private network boundary hai.

### Core Components:

| Component | Meaning |
|-----------|---------|
| VPC | Isolated network |
| CIDR | IP range (`10.0.0.0/16`) |
| Subnet | VPC ka smaller range inside AZ |
| Route Table | Traffic route rules |
| IGW | Internet Gateway for public internet |
| NAT Gateway | Private subnet outbound internet |

### Public vs Private Subnet:

| Subnet | Has route to IGW? | Use |
|--------|------------------|-----|
| Public | Yes | ALB, bastion |
| Private | No direct inbound | App, DB |

### SG vs NACL:

| Feature | Security Group | NACL |
|---------|----------------|------|
| Level | Instance/ENI | Subnet |
| Stateful | Yes | No |
| Rules | Allow only | Allow + deny |
| Common use | App firewall | Subnet guardrail |

**Short Interview Answer:**

VPC is your private AWS network. Public subnets route to IGW; private subnets use NAT for outbound. Security Groups are stateful instance-level firewalls; NACLs are stateless subnet-level controls.

---

## Q4: EC2, Auto Scaling Group aur Load Balancer ka production pattern kya hai?

**Answer:**

EC2 virtual servers provide compute. Production mein single EC2 run karna anti-pattern hai.

### Typical Pattern:

```text
User → ALB → Auto Scaling Group
             ├─ EC2 in AZ-1
             ├─ EC2 in AZ-2
             └─ EC2 in AZ-3
```

### EC2 Purchasing:

| Model | Use |
|-------|-----|
| On-Demand | Flexible, no commitment |
| Reserved / Savings Plan | Stable workloads |
| Spot | Batch/fault-tolerant workloads |

### Auto Scaling:

- Launch Template defines instance config
- Scaling policy based on CPU/RPS/queue depth
- Health checks replace bad instances

### Load Balancers:

| LB | Best for |
|----|----------|
| ALB | HTTP/HTTPS, path/host routing |
| NLB | TCP/UDP, ultra-low latency |

**Important:**

Backend services should be stateless so ASG can add/remove instances freely. Store sessions in Redis/JWT, not local EC2 memory.

---

## Q5: RDS Multi-AZ vs Read Replica vs Aurora difference kya hai?

**Answer:**

### RDS Multi-AZ:

- High availability
- Synchronous standby in another AZ
- Automatic failover
- Read scaling ke liye nahi

### Read Replica:

- Asynchronous replication
- Read scaling
- Reporting/analytics
- Lag possible

| Feature | Multi-AZ | Read Replica |
|---------|----------|--------------|
| Purpose | HA/failover | Read scaling |
| Replication | Sync | Async |
| Can serve reads | Usually no standby reads | Yes |
| Lag | Minimal | Possible |

### Aurora:

- AWS cloud-native relational DB
- 6-way replication across 3 AZs
- Up to 15 read replicas
- Faster failover
- Aurora Serverless v2 for variable workloads

### RDS Proxy:

Useful for Lambda/app connection pooling.

**Short Interview Answer:**

Multi-AZ is for high availability, read replicas are for read scaling, and Aurora is AWS-optimized relational storage with 6-way replication, fast failover, and high read replica scale.

---

## Q6: S3 object storage kaise kaam karta hai? Storage classes aur pre-signed URLs kya hain?

**Answer:**

S3 object storage hai, file system nahi. Data bucket ke andar objects ke form mein store hota hai.

### Concepts:

| Concept | Meaning |
|---------|---------|
| Bucket | Top-level container, globally unique name |
| Object | Data + metadata + key |
| Key | Object path-like name |
| Versioning | Multiple object versions |

### Storage Classes:

| Class | Use |
|-------|-----|
| Standard | Frequently accessed |
| Standard-IA | Infrequent access |
| One Zone-IA | Lower cost, less resilient |
| Intelligent-Tiering | Unknown access pattern |
| Glacier | Archive |

### Pre-signed URL:

Temporary secure URL for upload/download:

```text
Backend generates URL → client uploads directly to S3
```

### Multipart Upload:

Large files (>100MB) ke liye recommended.

**Important:**

S3 is highly durable, but permissions galat hue to data public leak ho sakta hai. Block Public Access enable rakho unless explicit public bucket needed.

---

## Q7: Lambda aur Serverless kab use karte ho? Cold start kya hai?

**Answer:**

Lambda event-driven compute hai jahan server manage nahi karna padta.

### Good Use Cases:
- API backend with API Gateway
- S3 file processing
- Scheduled jobs
- Queue consumers
- Lightweight event workflows

### Limits/Concerns:

| Concern | Note |
|---------|------|
| Max duration | 15 minutes |
| Cold start | First request latency after idle |
| Concurrency | Account/function limits |
| Stateless | Local state unreliable |

### Cold Start Mitigation:
- Provisioned Concurrency
- Smaller packages
- Avoid heavy initialization
- Keep dependencies lean

**Short Interview Answer:**

Lambda is best for event-driven, bursty, short-lived workloads. Cold start is initialization latency when a new execution environment is created. For latency-sensitive APIs, use Provisioned Concurrency or containers/EC2/ECS.

---

## Q8: API Gateway, SQS, SNS aur EventBridge mein kya use kab karein?

**Answer:**

### API Gateway:
Public HTTP/WebSocket front door for Lambda/HTTP services.

| Type | Use |
|------|-----|
| HTTP API | Cheaper/simple REST |
| REST API | Advanced features |
| WebSocket API | Real-time bidirectional |

### Messaging:

| Service | Pattern | Use |
|---------|---------|-----|
| SQS | Queue | Decouple workers, retry, DLQ |
| SNS | Pub/Sub fanout | Push event to many subscribers |
| EventBridge | Event bus/routing | SaaS/AWS/custom events |

### SQS Important Concepts:

- Visibility timeout
- Dead Letter Queue
- Long polling
- Standard vs FIFO

```text
Order Service → SNS Topic → SQS queues for Email/SMS/Analytics
```

**Important:**

SQS consumer idempotent hona chahiye because at-least-once delivery duplicates possible hain. FIFO queues ordering/dedup deti hain but throughput constraints hoti hain.

---

## Q9: ECS vs EKS vs Lambda kaise choose karte ho?

**Answer:**

| Platform | Best for |
|----------|----------|
| Lambda | Event-driven serverless functions |
| ECS Fargate | Simple container orchestration without servers |
| ECS EC2 | Containers with more infra control |
| EKS | Kubernetes ecosystem/control needed |

### ECS:

- Task Definition = container blueprint
- Service = desired count + deployment
- Fargate = no EC2 management
- ALB integration easy

### EKS:

- Managed Kubernetes control plane
- More flexibility
- Higher operational complexity

**Short Interview Answer:**

Use Lambda for short event-driven workloads, ECS Fargate for most containerized backend services with lower ops burden, and EKS when Kubernetes ecosystem, portability, or advanced orchestration is required.

---

## Q10: CloudWatch, CloudFront/Route 53, CI/CD aur cost optimization ke best practices kya hain?

**Answer:**

### Observability:

| Service | Use |
|---------|-----|
| CloudWatch Metrics | CPU, latency, errors |
| CloudWatch Logs | App/service logs |
| Alarms | Alerting |
| X-Ray | Distributed tracing |
| CloudTrail | Audit API calls |

### CDN + DNS:

| Service | Use |
|---------|-----|
| CloudFront | CDN, edge caching |
| Route 53 | DNS + routing policies |

Route 53 policies:
- Simple
- Weighted
- Latency
- Failover
- Geolocation

### CI/CD:

- GitHub Actions + OIDC to AWS
- CodeBuild for builds
- CodeDeploy for blue/green
- ECR image scanning
- IaC with CDK/Terraform/CloudFormation

### Cost Optimization:

| Area | Optimization |
|------|--------------|
| Compute | Savings Plans, right-sizing, Spot |
| S3 | Lifecycle + Intelligent-Tiering |
| NAT Gateway | Avoid unnecessary cross-AZ/data transfer |
| EBS/EIP | Delete unused resources |
| Monitoring | Budgets + Cost Explorer |

**Important:**

AWS interview mein architecture ke saath cost bhi discuss karo. Wrong NAT/data transfer/storage class choices production bills explode kar sakte hain.

**Short Interview Answer:**

For production AWS, I use CloudWatch/X-Ray/CloudTrail for observability, CloudFront + Route 53 for global delivery and DNS, GitHub Actions OIDC/CodePipeline for CI/CD, and Cost Explorer/Budgets/Savings Plans/right-sizing/S3 lifecycle policies for cost control.

