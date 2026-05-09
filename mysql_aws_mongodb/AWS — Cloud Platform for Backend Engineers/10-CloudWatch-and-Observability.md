# CloudWatch & Observability

## Status: Complete

---

## Table of Contents

1. [Three Pillars on AWS](#three-pillars-on-aws)
2. [CloudWatch Metrics](#cloudwatch-metrics)
3. [Custom Metrics](#custom-metrics)
4. [CloudWatch Logs](#cloudwatch-logs)
5. [Metric Filters](#metric-filters)
6. [Logs Insights](#logs-insights)
7. [CloudWatch Alarms](#cloudwatch-alarms)
8. [Composite Alarms](#composite-alarms)
9. [CloudWatch Dashboards](#cloudwatch-dashboards)
10. [AWS X-Ray](#aws-x-ray)
11. [Container Insights & Lambda Insights](#container-insights--lambda-insights)
12. [CloudTrail](#cloudtrail)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Three Pillars on AWS

| Pillar | Service |
|--------|---------|
| **Metrics** | CloudWatch Metrics |
| **Logs** | CloudWatch Logs |
| **Traces** | **AWS X-Ray** |

Plus:

- **CloudTrail** — API audit log (security/compliance)
- **AWS Config** — resource config history
- **GuardDuty** — threat detection (security)
- **OpenSearch / Managed Grafana / Managed Prometheus** — open ecosystem if you don't want CloudWatch alone

---

## CloudWatch Metrics

> "**Time-series numeric data** — every AWS service publishes metrics by default."

### Built-in metric examples

| Service | Sample metrics |
|---------|----------------|
| EC2 | `CPUUtilization`, `NetworkIn`, `DiskReadOps` |
| RDS | `CPUUtilization`, `DatabaseConnections`, `ReadLatency`, `FreeableMemory` |
| Lambda | `Invocations`, `Duration`, `Errors`, `Throttles`, `ConcurrentExecutions` |
| ALB | `RequestCount`, `TargetResponseTime`, `HTTPCode_Target_5XX_Count` |
| SQS | `ApproximateNumberOfMessagesVisible`, `ApproximateAgeOfOldestMessage` |

### Resolution

- **Standard**: 1-minute granularity (free for many)
- **High-res custom metrics**: 1-second granularity (extra cost)
- **Detailed monitoring** (EC2): 1-min instead of 5-min default — paid

---

## Custom Metrics

> "**Publish your own** numeric data via `PutMetricData` API."

```python
import boto3
cw = boto3.client('cloudwatch')

cw.put_metric_data(
    Namespace='MyApp',
    MetricData=[
        {
            'MetricName': 'OrdersProcessed',
            'Dimensions': [
                {'Name': 'Env', 'Value': 'prod'},
                {'Name': 'Region', 'Value': 'ap-south-1'}
            ],
            'Value': 42,
            'Unit': 'Count'
        }
    ]
)
```

### Embedded Metric Format (EMF)

> "**Log JSON** with a special structure → CloudWatch **auto-extracts metrics** without API calls. Cheaper + faster for high-volume metrics."

```json
{
  "_aws": {
    "Timestamp": 1715000000000,
    "CloudWatchMetrics": [{
      "Namespace": "MyApp",
      "Metrics": [{ "Name": "OrderLatency", "Unit": "Milliseconds" }],
      "Dimensions": [["Env"]]
    }]
  },
  "Env": "prod",
  "OrderLatency": 145.2
}
```

→ Best practice for **Lambda / containers** publishing custom metrics.

---

## CloudWatch Logs

### Hierarchy

```
Log Group
  ├── Log Stream (per source: per Lambda instance, per EC2, per container)
       └── Log Events (timestamp + message)
```

### Important settings

- **Retention** — default **never expire** (cost trap!) — set to 7/14/30/90 days based on need
- **Encryption** with KMS
- **Subscription Filter** — stream logs to Lambda / Kinesis / Firehose (e.g., to S3, OpenSearch)

### Pricing

- **Ingestion** ~$0.50/GB
- **Storage** ~$0.03/GB/month
- **Logs Insights queries** ~$0.005/GB scanned

→ Log discipline matters: noisy logs = bill explosion.

---

## Metric Filters

> "**Pattern match log lines** → emit a CloudWatch metric. Then alarm on it."

```
Filter pattern: ERROR
Metric:        MyApp/ErrorCount
Default value: 0
```

```
[timestamp, request_id, level=ERROR, ...]
```

### Use cases

- Count `ERROR` log lines → alarm on rate
- Extract latency from log message → metric
- Detect specific exceptions

→ Less common now with **EMF**, but still useful for legacy logs.

---

## Logs Insights

> "**SQL-like query language** to slice/dice CloudWatch Logs."

```
fields @timestamp, @message
| filter @message like /ERROR/
| stats count() by bin(5m)
| sort @timestamp desc
| limit 100
```

```
fields @timestamp, @duration, @memorySize, @maxMemoryUsed
| filter @type = "REPORT"
| stats avg(@duration), max(@duration), pct(@duration, 99) by bin(5m)
```

### Tips

- Set **time range** wisely (scans = cost)
- Use **`stats`** for aggregations
- Save useful queries for team
- Query **multiple log groups** at once (great for distributed services)

---

## CloudWatch Alarms

> "**Watch a metric** → trigger action when threshold breached."

### States

- `OK`
- `ALARM`
- `INSUFFICIENT_DATA`

### Actions

- **SNS topic** → email, SMS, Lambda, PagerDuty
- **Auto Scaling** policy
- **EC2 actions** (reboot, stop, terminate, recover)
- **Systems Manager** OpsItem / Incident Manager

### Configuration knobs

| Knob | Meaning |
|------|---------|
| **Threshold** | Static value or **anomaly detection** band |
| **Datapoints to alarm** | E.g., 3 of 5 evaluation periods breach |
| **Treat missing data** | `notBreaching`, `breaching`, `ignore`, `missing` |
| **Period** | 1m / 5m / etc. |

### Example: Lambda error alarm

```
Metric: AWS/Lambda Errors (FunctionName=myFn)
Statistic: Sum
Period: 1 min
Threshold: > 5
Datapoints: 3 out of 3
→ ALARM → SNS → PagerDuty
```

### Anomaly detection

- ML model learns normal pattern → alerts on deviation
- Useful when **threshold is hard to pick**

---

## Composite Alarms

> "**Combine multiple alarms** with logic (`AND`, `OR`, `NOT`)."

```
ALARM("HighCpu") AND ALARM("HighLatency")
```

### Why

- Reduce **alarm fatigue** — only page when **multiple signals** agree
- Avoid noisy single-metric pages

---

## CloudWatch Dashboards

> "**JSON-defined visual dashboards** of metrics, logs, alarms."

- Per-account (no cross-account by default — use cross-account observability)
- Auto-refresh
- Embed Logs Insights query results

### Cost

- First 3 dashboards free per month, then ~$3/dashboard/mo

### Better tool

- **Managed Grafana** + CloudWatch datasource — much richer dashboards if you outgrow CW

---

## AWS X-Ray

> "**Distributed tracing.** Trace a request across Lambda → API Gateway → SQS → ECS → DynamoDB. Find slow hops."

### Concepts

- **Trace** — entire request journey (one trace ID)
- **Segment** — work done by one service
- **Subsegment** — sub-call (DB query, HTTP call)
- **Annotations** — indexed key-value (filter by)
- **Metadata** — non-indexed key-value (debugging info)

### Enable

- Lambda: enable **Active Tracing** + use AWS SDK (auto-instrumented)
- ECS/EC2: run **X-Ray daemon** sidecar + use SDK
- API Gateway / ALB / SQS: enable in service settings

### Service Map

> "Visual graph of services + latency + error rate per node."

→ Pinpoint bottlenecks in seconds.

### Modern alternative

- **OpenTelemetry (OTel) + AWS Distro for OpenTelemetry (ADOT)** → can send to X-Ray + others
- Avoid lock-in

---

## Container Insights & Lambda Insights

### Container Insights

- ECS / EKS / Fargate metrics: **per-pod CPU/mem/network/disk**
- Auto-discovers tasks and containers
- CloudWatch Logs auto-aggregation

### Lambda Insights

- Per-function: **CPU, memory, network, init duration, fn errors**
- Adds CloudWatch agent layer to Lambda
- Useful for diagnosing **memory leaks, cold start patterns**

→ Both are paid extensions — turn on for prod-critical workloads.

---

## CloudTrail

> "**Records every AWS API call** in your account — who did what, when, from where. Foundational for security and audit."

### Trail types

- **Management events** — control plane (CreateBucket, RunInstances) — free 90 days history
- **Data events** — data plane (S3 GetObject, Lambda Invoke) — paid, opt-in per resource
- **Insights events** — anomalous activity detection

### Best practices

- **Enable CloudTrail in all regions** (single trail)
- **Send to dedicated S3 bucket** in **separate Audit account** (org-wide trail)
- Enable **log file integrity validation**
- Stream to CloudWatch Logs + alarm on suspicious actions (root login, IAM policy change)

### Example: alert on root usage

```
Filter pattern: { $.userIdentity.type = "Root" }
Metric:        Security/RootAccountUsage
Alarm:         > 0 → SNS
```

---

## Pitfalls

1. **Logs without retention** → bill grows monthly forever.
2. **Custom metrics via `PutMetricData`** at 1-sec resolution from 1000 instances → expensive. Use EMF.
3. **No alarm on critical metrics** — RDS at 99% CPU, no one knows till users complain.
4. **Single-metric alarms** spamming pager — switch to **composite**.
5. **No X-Ray** in microservices — debugging takes hours.
6. **No CloudTrail in all regions** — attacker uses unmonitored region.
7. **Treat missing data wrong** — alarm flaps on intermittent metric publishing.
8. **Dashboards without owners** → become stale wallpaper.
9. **No log structure** (text only) — Logs Insights queries unfit.
10. **OTel + X-Ray duplicated** without strategy — double instrumentation overhead.

---

## Cheat Sheet

| Need | Tool |
|------|------|
| Service health | CloudWatch metrics + alarms |
| Log search | Logs Insights |
| Log → metric | Metric filter / EMF |
| Distributed trace | X-Ray (or OTel via ADOT) |
| Container metrics | Container Insights |
| Lambda perf | Lambda Insights |
| Audit who did what | CloudTrail |
| Threat detection | GuardDuty |
| Resource config history | AWS Config |
| Rich dashboards | Managed Grafana |

---

## Practice

1. Add **EMF JSON logging** in a Lambda → custom metric appears.
2. Build alarm: RDS `CPUUtilization > 80%` for 3/5 datapoints → SNS → email.
3. Write **Logs Insights** query: top 10 latency p99 by route in last 24 h.
4. Enable **X-Ray** end-to-end for an API GW → Lambda → DynamoDB chain. Find slowest hop.
5. Set **CloudTrail** org trail → S3 in audit account → CloudWatch Logs → alarm on root login.
6. Configure **composite alarm**: HighCpu AND HighLatency → page; either alone → silent.
