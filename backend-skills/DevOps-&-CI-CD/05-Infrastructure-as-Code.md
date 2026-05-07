# Infrastructure as Code (IaC)

## Status: Not Started

---

## Table of Contents

1. [IaC Kya Hai?](#iac-kya-hai)
2. [Terraform Basics](#terraform-basics)
3. [Terraform State](#terraform-state)
4. [Modules](#modules)
5. [Terraform vs Pulumi vs CDK](#terraform-vs-pulumi-vs-cdk)
6. [AWS CloudFormation](#aws-cloudformation)
7. [Ansible (Configuration Management)](#ansible-configuration-management)
8. [GitOps (ArgoCD, Flux)](#gitops-argocd-flux)
9. [Best Practices](#best-practices)
10. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## IaC Kya Hai?

**Matlab:** Infrastructure (servers, DBs, networks) ko **code** se manage karo — version controlled, reviewable, reproducible.

### Manual vs IaC

```
❌ Manual (clicking AWS console):
  - Drift over time
  - "What's deployed?" — nobody knows
  - Disaster recovery painful
  - Audit trail missing

✅ IaC (Terraform/CDK):
  - Code in Git → review/PR
  - Reproduce env in minutes
  - Drift detection
  - Disaster recovery = re-apply code
```

### IaC Tools Categories

| Category | Tool |
|----------|------|
| **Provisioning** (cloud resources) | Terraform, Pulumi, AWS CDK, CloudFormation |
| **Config Management** (OS-level) | Ansible, Chef, Puppet, Salt |
| **Container orchestration config** | Helm, Kustomize, ArgoCD (declarative) |

---

## Terraform Basics

**Matlab:** Open-source, **multi-cloud**, declarative IaC tool by HashiCorp. Most popular.

### Core Concepts

```hcl
provider "aws" {
  region = "ap-south-1"
}

resource "aws_instance" "web" {
  ami           = "ami-0c55b159cbfafe1f0"
  instance_type = "t3.micro"
  
  tags = {
    Name = "web-server"
  }
}
```

### Building Blocks

#### 1. Provider

**Matlab:** Plugin for a cloud/service (AWS, GCP, Azure, Kubernetes, GitHub).

```hcl
provider "aws" {
  region = "ap-south-1"
  profile = "myprofile"
}

provider "kubernetes" {
  config_path = "~/.kube/config"
}
```

#### 2. Resource

**Matlab:** Single infrastructure object — VM, S3 bucket, IAM role.

```hcl
resource "aws_s3_bucket" "logs" {
  bucket = "my-app-logs-${var.environment}"
}

resource "aws_s3_bucket_versioning" "logs" {
  bucket = aws_s3_bucket.logs.id
  versioning_configuration {
    status = "Enabled"
  }
}
```

#### 3. Variables

```hcl
variable "environment" {
  description = "Deployment environment"
  type        = string
  default     = "dev"
  
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Must be dev, staging, or prod."
  }
}

variable "instance_count" {
  type    = number
  default = 1
}
```

Use:
```hcl
resource "aws_instance" "web" {
  count = var.instance_count
  # ...
}
```

#### 4. Outputs

```hcl
output "bucket_arn" {
  value       = aws_s3_bucket.logs.arn
  description = "ARN of logs bucket"
}

output "db_endpoint" {
  value     = aws_db_instance.main.endpoint
  sensitive = true   # masked in output
}
```

#### 5. Data Sources (Read existing resources)

```hcl
data "aws_vpc" "default" {
  default = true
}

resource "aws_subnet" "main" {
  vpc_id     = data.aws_vpc.default.id
  cidr_block = "10.0.1.0/24"
}
```

### Workflow Commands

```bash
terraform init       # download providers
terraform fmt        # format files
terraform validate   # syntax check
terraform plan       # show what would change (DRY RUN)
terraform apply      # apply changes
terraform destroy    # tear down everything
```

### `terraform plan` Output

```
+ create          → new resource
~ update          → modify existing
- destroy         → delete
-/+ replace       → destroy + create (sometimes for immutable changes)
```

⚠️ Always read plan before `apply`.

---

## Terraform State

**Matlab:** Terraform tracks "kya already deploy hai" in a `terraform.tfstate` file.

### Why State?

- Map config resources → real cloud resources
- Track metadata (created times, attributes)
- Detect drift

### Local State (Default — Don't Use in Teams)

```
terraform.tfstate    ← in working directory
```

Problems:
- Can't share between team members
- No locking → concurrent applies corrupt
- Sensitive data in plain text

### Remote State (Production)

#### S3 + DynamoDB (Locking)

```hcl
terraform {
  backend "s3" {
    bucket         = "my-tf-state"
    key            = "prod/network/terraform.tfstate"
    region         = "ap-south-1"
    encrypt        = true
    dynamodb_table = "terraform-locks"
  }
}
```

DynamoDB table provides **lock** so concurrent applies don't conflict.

#### Terraform Cloud / Enterprise

Managed remote state + UI + RBAC.

#### Other Backends

- Azure Blob Storage
- GCS
- Consul
- HTTP backend (custom)

### State Commands

```bash
terraform state list                  # list all resources
terraform state show aws_instance.web # show details
terraform state rm aws_instance.web   # remove from state (NOT cloud)
terraform import aws_instance.web i-1234abc  # adopt existing resource
```

---

## Modules

**Matlab:** Reusable bundles of configuration — DRY for IaC.

### Module Structure

```
modules/
  vpc/
    main.tf
    variables.tf
    outputs.tf
```

`modules/vpc/main.tf`:
```hcl
resource "aws_vpc" "this" {
  cidr_block = var.cidr
  tags       = { Name = var.name }
}
```

`modules/vpc/variables.tf`:
```hcl
variable "cidr" { type = string }
variable "name" { type = string }
```

`modules/vpc/outputs.tf`:
```hcl
output "vpc_id" {
  value = aws_vpc.this.id
}
```

### Using a Module

```hcl
module "prod_vpc" {
  source = "./modules/vpc"
  cidr   = "10.0.0.0/16"
  name   = "prod"
}

# Use module output
resource "aws_subnet" "app" {
  vpc_id = module.prod_vpc.vpc_id
}
```

### Public Registry Modules

```hcl
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "5.5.0"
  
  name = "my-vpc"
  cidr = "10.0.0.0/16"
}
```

→ Browse: [registry.terraform.io](https://registry.terraform.io)

---

## Terraform vs Pulumi vs CDK

### Terraform

- **Language:** HCL (declarative, custom DSL)
- **Multi-cloud:** ✅ best-in-class
- **State:** External file (S3/Cloud)
- **Maturity:** Most established

### Pulumi

- **Language:** TypeScript, Python, Go, C#, Java (real programming languages)
- **Multi-cloud:** ✅
- **State:** Pulumi service or self-hosted
- **Pros:** Use loops, classes, IDE support, real testing
- **Cons:** Younger ecosystem

```typescript
// Pulumi (TypeScript)
import * as aws from "@pulumi/aws";

const bucket = new aws.s3.Bucket("logs", {
    versioning: { enabled: true }
});
export const bucketName = bucket.id;
```

### AWS CDK (Cloud Development Kit)

- **Language:** TS, Python, Go, Java, C#
- **AWS-only**
- **Compiles to** CloudFormation under the hood
- **Pros:** AWS-native, high-level constructs (`Vpc`, `EcsService`)
- **Cons:** AWS lock-in, indirect (debug CloudFormation)

```typescript
import { aws_s3 as s3 } from 'aws-cdk-lib';

const bucket = new s3.Bucket(this, 'LogsBucket', {
    versioned: true
});
```

### Comparison

| | Terraform | Pulumi | AWS CDK |
|--|-----------|--------|---------|
| Language | HCL | TS/Py/Go/Java | TS/Py/Go/Java |
| Multi-cloud | ✅ | ✅ | ❌ AWS only |
| Real programming | ❌ | ✅ | ✅ |
| State management | External | External | CFN-managed |
| Maturity | Highest | Growing | Growing |
| Best for | Multi-cloud, ops teams | Devs who want code | AWS-heavy shops |

---

## AWS CloudFormation

**Matlab:** AWS-native IaC — YAML/JSON templates. Older, AWS-only.

### Example

```yaml
AWSTemplateFormatVersion: '2010-09-09'

Parameters:
  EnvName:
    Type: String
    Default: dev

Resources:
  LogsBucket:
    Type: AWS::S3::Bucket
    Properties:
      BucketName: !Sub "my-app-logs-${EnvName}"
      VersioningConfiguration:
        Status: Enabled

Outputs:
  BucketArn:
    Value: !GetAtt LogsBucket.Arn
```

### Pros / Cons

| Pro | Con |
|-----|-----|
| Native AWS integration | AWS-only |
| No state file mgmt (AWS handles) | YAML/JSON less ergonomic |
| Free | Slower iteration |
| Stack drift detection | Less flexible than HCL |

→ Most teams use **Terraform** or **CDK** instead.

---

## Ansible (Configuration Management)

**Matlab:** OS-level config — install packages, edit configs, start services. Agentless (SSH).

### Use Case

Provisioning gives you VM. Ansible configures **inside** the VM.

```
Terraform: Create EC2 instance
Ansible:   Install nginx, configure firewall, deploy app
```

### Inventory

```ini
[webservers]
web1.example.com
web2.example.com

[dbservers]
db1.example.com
```

### Playbook

```yaml
- name: Configure web servers
  hosts: webservers
  become: yes
  
  tasks:
    - name: Install nginx
      apt:
        name: nginx
        state: present
        update_cache: yes
    
    - name: Copy nginx config
      template:
        src: nginx.conf.j2
        dest: /etc/nginx/nginx.conf
      notify: restart nginx
    
    - name: Ensure nginx running
      service:
        name: nginx
        state: started
        enabled: yes
  
  handlers:
    - name: restart nginx
      service:
        name: nginx
        state: restarted
```

### Run

```bash
ansible-playbook -i inventory.ini site.yml
```

### Idempotent

Re-running same playbook → only changes what's drifted. Safe.

### When Use Ansible Today?

- Configuring VMs / bare metal
- Patching OS
- Legacy systems

**For containers / Kubernetes:** Less relevant — image is the artifact.

---

## GitOps (ArgoCD, Flux)

**Matlab:** **Git** is the **source of truth** for production state. CD agent in cluster pulls from Git.

### How

```
Developer → push manifests to Git
              ↓
         (auto-detected)
              ↓
       ArgoCD/Flux in cluster
              ↓
       Apply to Kubernetes
              ↓
       (continuous reconciliation)
```

### vs Push-Based CD

```
Push:  CI deploys to cluster (CI has cluster credentials)
Pull:  Cluster watches Git, deploys itself (cluster doesn't expose creds outward)
```

### Benefits

- ✅ Audit log = git log
- ✅ Easy rollback: `git revert`
- ✅ Single source of truth
- ✅ Self-healing (drift auto-corrected)
- ✅ No CI cluster credentials leak risk

### ArgoCD

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: myapp
spec:
  source:
    repoURL: https://github.com/myorg/k8s-config
    path: apps/myapp
    targetRevision: main
  destination:
    server: https://kubernetes.default.svc
    namespace: production
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

ArgoCD UI shows app sync status, diffs, drift.

### Flux

CRD-driven, more lightweight, integrates well with Helm.

### Patterns

#### App-of-Apps

One ArgoCD Application defines many child Applications.

#### Environment per Folder

```
k8s-config/
  apps/
    myapp/
      base/
      overlays/
        dev/
        staging/
        prod/
```

Use **Kustomize** or **Helm** for env-specific overrides.

---

## Best Practices

### 1. Modules + Reuse

Don't copy-paste config across envs. Modules + variables.

### 2. Remote State Always

Local state = lost work waiting to happen.

### 3. State Locking

DynamoDB / Terraform Cloud — prevent concurrent apply corruption.

### 4. Plan Before Apply

In CI: `terraform plan` on PR → reviewer sees changes → `apply` on merge.

### 5. Don't Manually Edit Cloud

Drift breaks reproducibility. Use IaC for everything.

### 6. Separate State Files per Environment

```
state/
  dev/
  staging/
  prod/
```

Don't mix → blast radius isolation.

### 7. Sensitive Data via Secrets Manager

```hcl
data "aws_secretsmanager_secret_version" "db" {
  secret_id = "prod/db/password"
}

resource "aws_db_instance" "main" {
  password = data.aws_secretsmanager_secret_version.db.secret_string
}
```

Don't hardcode in `.tf` files.

### 8. Pin Provider Versions

```hcl
terraform {
  required_version = ">= 1.6.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.30"
    }
  }
}
```

### 9. Tag Everything

```hcl
default_tags {
  tags = {
    Environment = var.environment
    ManagedBy   = "terraform"
    Project     = var.project
  }
}
```

→ Cost allocation, identification.

### 10. CI for IaC

- `terraform fmt -check`
- `terraform validate`
- `tflint` (linter)
- `tfsec` / `checkov` (security scanner)
- `terraform plan` posted as PR comment

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| **IaC** | Infra as code, version controlled |
| **Terraform** | Multi-cloud, HCL, declarative |
| **Provider** | AWS, GCP, K8s plugins |
| **Resource** | Single cloud object |
| **State** | Source of truth — remote in prod |
| **Module** | Reusable bundle |
| **Pulumi** | Real programming languages |
| **CDK** | AWS-only, code-first |
| **CloudFormation** | AWS-native YAML |
| **Ansible** | Config management (OS) |
| **GitOps** | Git = truth, agent pulls |
| **ArgoCD/Flux** | K8s GitOps |

---

## Practice

1. `terraform init/plan/apply` for a simple S3 bucket.
2. Set up remote state with S3 backend + DynamoDB lock.
3. Refactor 3 similar resources into a Terraform module.
4. Compare same VPC in Terraform vs Pulumi vs CDK syntax.
5. Set up ArgoCD on local kind cluster, deploy a sample app from Git.
6. Add `tfsec` to your CI to catch security misconfigurations.
