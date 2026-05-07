# Dependency Security (SCA)

## Status: Not Started

---

## Table of Contents

1. [Why Dependency Security?](#why-dependency-security)
2. [The Modern Threat Landscape](#the-modern-threat-landscape)
3. [SCA — Software Composition Analysis](#sca--software-composition-analysis)
4. [Tools](#tools)
5. [CVE & CVSS Basics](#cve--cvss-basics)
6. [SBOM — Software Bill of Materials](#sbom--software-bill-of-materials)
7. [Patching Workflow](#patching-workflow)
8. [Choosing New Libraries Safely](#choosing-new-libraries-safely)
9. [Lockfiles & Version Pinning](#lockfiles--version-pinning)
10. [Container & OS Dependencies](#container--os-dependencies)
11. [Supply Chain Attacks](#supply-chain-attacks)
12. [CI/CD Integration](#cicd-integration)
13. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Why Dependency Security?

Modern apps = **80–90% third-party code**. Your app's security is only as strong as its weakest dependency.

### Famous Incidents

| Year | Vulnerability | Impact |
|------|---------------|--------|
| 2017 | **Equifax (Apache Struts CVE-2017-5638)** | 147M records breached |
| 2021 | **Log4Shell (Log4j CVE-2021-44228)** | RCE in millions of apps |
| 2022 | **Spring4Shell (CVE-2022-22965)** | Spring RCE |
| 2024 | **xz-utils backdoor (CVE-2024-3094)** | Near-miss SSH backdoor |

> Most breaches today involve **at least one** known-vulnerable dependency.

---

## The Modern Threat Landscape

### Direct vs Transitive

```
Your app
  ├── library A (you chose)
  │     └── library B (transitive)
  │           └── library C (transitive of transitive)
  └── library X
```

A typical Maven app has **10–20 direct** but **100+ transitive** deps.

Transitive vulns are common attack surface — you didn't pick them, but you ship them.

### Categories of Issues

1. **Known CVE** in a published version (most common)
2. **Malicious package** uploaded to public registry (typosquatting, package takeover)
3. **Backdoored package** by compromised maintainer
4. **License compliance** (GPL vs MIT etc. — legal not security but tracked)

---

## SCA — Software Composition Analysis

**Matlab:** **Automated** discovery of OSS components + their known vulnerabilities + license info.

### What SCA Does

- Parse `pom.xml` / `package.json` / `go.sum` / etc.
- Resolve full dependency tree (incl. transitive)
- Match versions against vulnerability database (NVD, GitHub Advisories)
- Report CVEs, severity, fix availability
- Often: license info, outdated versions

### vs SAST / DAST

| Type | What |
|------|------|
| **SAST** | Static code analysis (your code) |
| **DAST** | Dynamic — runs app, probes |
| **SCA** | Dependency vulns |
| **IAST** | Hybrid (instrumentation) |

---

## Tools

### Free / Open-Source

#### OWASP Dependency-Check

```bash
# Maven plugin
./mvnw org.owasp:dependency-check-maven:check
```

Produces HTML / JSON report. Fail build on threshold:

```xml
<plugin>
  <groupId>org.owasp</groupId>
  <artifactId>dependency-check-maven</artifactId>
  <version>9.0.7</version>
  <configuration>
    <failBuildOnCVSS>7</failBuildOnCVSS>   <!-- fail on High+ -->
  </configuration>
  <executions>
    <execution>
      <goals><goal>check</goal></goals>
    </execution>
  </executions>
</plugin>
```

#### Trivy

```bash
trivy fs .
trivy image myapp:latest
trivy fs --severity HIGH,CRITICAL --exit-code 1 .
```

Covers: file system, container images, Git repos, K8s manifests.

#### Grype (Anchore)

```bash
grype myapp:latest
grype dir:.
```

#### npm audit / yarn audit

```bash
npm audit
npm audit fix       # auto-update where safe
```

#### govulncheck (Go)

```bash
go install golang.org/x/vuln/cmd/govulncheck@latest
govulncheck ./...
```

### SaaS / Commercial

| Tool | Notes |
|------|-------|
| **Snyk** | Popular, IDE + CI + container, deep DB |
| **GitHub Dependabot** | Free, auto-PRs for updates |
| **GitHub Security Alerts** | Native, free |
| **Sonatype Nexus IQ / Lifecycle** | Enterprise, policy-rich |
| **JFrog Xray** | Artifactory-integrated |
| **WhiteSource / Mend** | Enterprise SCA |
| **Veracode SCA** | App security suite |

### GitHub Native (Mostly Free)

#### Dependabot Alerts

Auto-detects vulns, posts alerts.

#### Dependabot Updates

Auto PRs to bump deps. Configure:

```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: "maven"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 10
    
  - package-ecosystem: "npm"
    directory: "/frontend"
    schedule:
      interval: "weekly"
  
  - package-ecosystem: "github-actions"
    directory: "/"
    schedule:
      interval: "monthly"
  
  - package-ecosystem: "docker"
    directory: "/"
    schedule:
      interval: "weekly"
```

#### CodeQL

GitHub-hosted SAST (also catches some dep-related issues).

---

## CVE & CVSS Basics

### CVE — Common Vulnerabilities and Exposures

Identifier for a specific known vulnerability:

```
CVE-2021-44228   ← Log4Shell
CVE-2017-5638    ← Apache Struts
```

Database: [nvd.nist.gov](https://nvd.nist.gov/) — National Vulnerability Database.

### CVSS — Common Vulnerability Scoring System

Severity score 0.0–10.0.

| Range | Severity |
|-------|----------|
| 0.1–3.9 | Low |
| 4.0–6.9 | Medium |
| 7.0–8.9 | High |
| 9.0–10.0 | Critical |

Composed of metrics: attack vector, complexity, privileges, user interaction, impact.

```
CVSS 9.8 / Critical / Network / No Auth → drop everything, patch now
CVSS 4.0 / Medium / Local / Auth required → assess context
```

### Context Matters

CVE in a library you don't actually use → not exploitable. Context-aware tools (Snyk reachability, Datadog ASM) reduce noise.

---

## SBOM — Software Bill of Materials

**Matlab:** Inventory of all components in your app — versions, hashes, licenses.

### Formats

- **CycloneDX** (OWASP)
- **SPDX** (Linux Foundation)

### Generation

```bash
# Syft (popular)
syft myapp:latest -o cyclonedx-json > sbom.json

# Maven
./mvnw org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom

# npm
cyclonedx-npm --output-format JSON --output-file sbom.json
```

### Why?

- Compliance (US Executive Order 14028, EU CRA)
- Quick lookup: "Are we vulnerable to CVE-X?" → search SBOM
- Customer trust / supply chain transparency

### Distribute

Sign + publish alongside artifact:

```
ghcr.io/myorg/myapp:1.2.3
ghcr.io/myorg/myapp:1.2.3.sbom
```

`docker buildx` can produce SBOMs natively (`--sbom=true`).

---

## Patching Workflow

### Triage New CVE

```
1. Detect (Dependabot alert / CI scan)
2. Assess severity + reachability
3. Decide: patch now / next sprint / accept risk
4. Test patch (regression)
5. Deploy
6. Verify fixed
```

### SLA Examples

| Severity | Patch by |
|----------|----------|
| Critical (CVSS ≥ 9) | 24-72 hours |
| High (7-8.9) | 7-14 days |
| Medium | 30 days |
| Low | Best effort / quarter |

### Why Be Aggressive?

Once CVE is public, exploits public quickly. Log4Shell weaponized within hours.

### Update Strategy

#### Patch Updates (Auto-Merge Often Safe)

```
1.2.3 → 1.2.4
```

Configure Dependabot to auto-merge **patch** + **minor** if CI passes:

```yaml
labels: ["dependencies"]
auto-merge: true   # via separate workflow / Mergify
```

#### Major Updates (Manual Review)

```
1.x → 2.x
```

Breaking changes possible — review changelog, test thoroughly.

### Don't Fall Behind

Updating regularly = small painless updates.
Skipping a year = "version 1 → version 5, everything breaks" → painful.

---

## Choosing New Libraries Safely

Before `pom.xml` mein add karne se pehle:

### Quick Checks

- [ ] **Maintained?** Last commit / release recent?
- [ ] **Popular?** Stars / weekly downloads?
- [ ] **Vulnerabilities?** Past CVEs — how quickly fixed?
- [ ] **License?** Compatible with your use?
- [ ] **Dependencies?** Pulls in 100+ transitive?
- [ ] **Org?** Backed by reputable org / community?
- [ ] **Snyk Advisor / Socket / OpenSSF Scorecard?** Reputation tools

### Tools

- [snyk.io/advisor](https://snyk.io/advisor/) — package health score
- [scorecard.dev](https://scorecard.dev/) — OpenSSF Scorecard (CI checks, signed releases, etc.)
- [socket.dev](https://socket.dev/) — supply chain scanning
- [deps.dev](https://deps.dev/) — Google's dep insight

### Red Flags

- Single maintainer, recent (typosquat?)
- Suddenly active after years dormant (takeover?)
- Asks for excessive permissions
- Obfuscated code

### Use Established Alternatives

Prefer mature, widely-used libs:
- HTTP: Apache HttpClient / OkHttp
- JSON: Jackson
- JWT: jjwt (java-jwt)
- Crypto: Bouncy Castle (only when needed) / built-in

---

## Lockfiles & Version Pinning

**Matlab:** Reproducible builds — `1.2.x` resolves to **same exact** version every time.

### Lockfiles

| Ecosystem | Lockfile |
|-----------|----------|
| npm | `package-lock.json` |
| yarn | `yarn.lock` |
| pnpm | `pnpm-lock.yaml` |
| pip | `requirements.txt` (pinned) or `Pipfile.lock` / poetry's `poetry.lock` |
| Go | `go.sum` |
| Cargo | `Cargo.lock` |
| Maven | (no native lockfile; use `<version>` exact + Maven Enforcer) |
| Gradle | `gradle.lockfile` (opt-in) |

### Maven — Pin Direct, Verify Transitive

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.fasterxml.jackson</groupId>
      <artifactId>jackson-bom</artifactId>
      <version>2.16.1</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

→ BOMs control transitive versions cleanly.

### Why Pin?

- ❌ `^1.2` could install 1.99 with breaking change tomorrow
- ❌ Build today vs tomorrow could differ
- ❌ Malicious 1.2.99 published → auto-pulled in CI

### Renovate / Dependabot

Configure to **bump pins** in PRs you review — best of both worlds.

---

## Container & OS Dependencies

App deps aren't the whole story — base image has OS packages too.

### Scan Images

```bash
trivy image myapp:1.2.3
```

Lists OS-level CVEs (apt, apk, yum) + language deps inside.

### Reduce Surface

- **Distroless** / **Alpine** / **Chainguard** images → fewer packages → fewer CVEs
- **Multi-stage** — drop build tools from final image
- **Non-root** user

→ More: `DevOps-&-CI-CD/04-Docker-in-CI-CD.md`.

### Update Base Images

```dockerfile
# Pin to digest for reproducibility
FROM eclipse-temurin:21-jre-alpine@sha256:abc...
```

Dependabot can update Docker base images:

```yaml
- package-ecosystem: "docker"
  directory: "/"
  schedule:
    interval: "weekly"
```

---

## Supply Chain Attacks

### Common Attack Types

#### 1. Typosquatting

```
Real:    request
Malicious: requrest, requestz, request-promise-native
```

→ Validate exact names; use `socket.dev` or scoped packages.

#### 2. Dependency Confusion

Internal package name `@mycompany/utils` not registered on public npm → attacker registers same name with higher version → CI pulls malicious public version.

→ Configure registry / scope precedence; private registry mirror.

#### 3. Maintainer Takeover

Maintainer's account compromised → malicious release.

→ Mitigations: Sigstore signing, OpenSSF Scorecard checks, mfa-required, package transparency log.

#### 4. CI Pipeline Compromise

Build infra hacked → injects malicious code into legit packages.

→ SLSA (Supply-chain Levels for Software Artifacts) framework, signed provenance.

### Defenses

- **Pin versions + lockfiles**
- **Verify checksums / signatures** (`npm install --provenance`)
- **Internal mirror** (Artifactory, Nexus, Verdaccio) — only approved versions
- **Vendor approval process** for new deps
- **OpenSSF Scorecard** in CI
- **Sigstore / cosign** verification
- **Private registries** with allow-list

---

## CI/CD Integration

### Pipeline Stage

```
1. Lint
2. Build
3. Test
4. SCA scan ← here
5. SAST
6. Container scan
7. Package
8. Deploy
```

### Fail Fast on Critical

```yaml
- name: Trivy scan
  uses: aquasecurity/trivy-action@master
  with:
    image-ref: myapp:${{ github.sha }}
    severity: CRITICAL,HIGH
    exit-code: '1'
```

### Periodic Re-Scan

CVEs published continuously. Even unchanged image becomes vulnerable.

```yaml
on:
  schedule:
    - cron: '0 6 * * *'   # daily
```

→ Re-scan all production images, alert on new CVE.

### SARIF Upload

Send results to GitHub Security tab:

```yaml
- uses: github/codeql-action/upload-sarif@v3
  with:
    sarif_file: trivy.sarif
```

→ All findings centralized, deduplicated.

---

## Summary Cheat Sheet

| Practice | Status |
|----------|--------|
| Pin versions + lockfiles | ✅ always |
| Dependabot / Snyk in repo | ✅ enable |
| OWASP Dependency-Check / Trivy in CI | ✅ |
| Patch SLA (Critical < 72h) | ✅ |
| SBOM generation | ✅ regulated industries |
| Update base images regularly | ✅ |
| Allow-list registries | ✅ enterprise |
| Verify signatures | ✅ Sigstore |
| OpenSSF Scorecard | ✅ |
| Maintain inventory | ✅ |

| Tool | Layer |
|------|-------|
| Dependabot / Renovate | Dep updates |
| Snyk / OWASP DC / Grype | SCA |
| Trivy | Image + IaC + filesystem |
| Syft | SBOM generation |
| cosign / Sigstore | Signing & verification |
| Sonatype IQ / JFrog Xray | Enterprise SCA + policy |

---

## Practice

1. Add OWASP Dependency-Check or Trivy to CI; fix any High/Critical findings.
2. Enable Dependabot for Maven, npm, Docker, GitHub Actions.
3. Generate SBOM using Syft for one of your services; commit/upload.
4. Set patch SLAs in your team's runbook (`Critical < 72h` etc.).
5. Audit one new library before adding (Snyk Advisor, Scorecard).
6. Convert npm `^1.2` to `1.2.x` exact via lockfile + verify.
7. Schedule daily container re-scans; alert on new CVE in deployed image.
