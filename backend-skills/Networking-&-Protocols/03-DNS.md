# DNS — Domain Name System

## Status: Not Started

---

## Table of Contents

1. [DNS Kya Hai?](#dns-kya-hai)
2. [DNS Hierarchy](#dns-hierarchy)
3. [DNS Resolution Flow](#dns-resolution-flow)
4. [Record Types](#record-types)
5. [TTL (Time-To-Live)](#ttl-time-to-live)
6. [DNS Caching](#dns-caching)
7. [Negative Caching](#negative-caching)
8. [DNSSEC](#dnssec)
9. [DNS-Based Load Balancing](#dns-based-load-balancing)
10. [Split-Horizon DNS](#split-horizon-dns)
11. [DNS over HTTPS / TLS](#dns-over-https--tls)
12. [Useful Tools & Debugging](#useful-tools--debugging)

---

## DNS Kya Hai?

**Matlab:** **Domain Name System** — internet ki "phonebook". Human-readable domain names (`google.com`) ko machine-readable IP addresses (`142.250.190.78`) mein translate karta hai.

```
User types: google.com
   ↓
DNS resolution
   ↓
142.250.190.78
   ↓
Browser connects to that IP
```

### Why DNS?

- IP addresses change (server migration, load balancing)
- Memorize karna mushkil
- Multiple servers behind one name
- Different IPs for different geographies (CDN)

### Default Port

- **UDP 53** (standard queries)
- **TCP 53** (large responses, zone transfers)
- **TCP 853** (DNS over TLS)
- **HTTPS 443** (DNS over HTTPS)

---

## DNS Hierarchy

DNS namespace **tree-structured**.

```
                    . (root)
                    │
       ┌────────────┼────────────┬─────────┐
       │            │            │         │
      com          org          net        in    ← TLD
       │
   ┌───┴───┐
   │       │
example  google                              ← second-level
   │
   ├── www
   ├── api
   └── mail
```

### FQDN — Fully Qualified Domain Name

```
www.example.com.
                ↑
             root (often omitted but technically there)
```

### Levels

| Level | Example |
|-------|---------|
| Root | `.` |
| TLD (Top-Level) | `.com`, `.org`, `.in`, `.io` |
| Second-Level | `example.com` |
| Subdomain | `www.example.com`, `api.example.com` |

---

## DNS Resolution Flow

### The 4 Players

1. **Stub Resolver** — your OS / browser (asks the next guy)
2. **Recursive Resolver** — does the legwork (your ISP's, Google's `8.8.8.8`, Cloudflare's `1.1.1.1`)
3. **Authoritative Servers** — root → TLD → domain's authoritative
4. **Cache** — at multiple levels

### Full Resolution (Cache Miss)

```
You type: www.example.com

[1] Browser → OS resolver → Recursive Resolver (e.g., 8.8.8.8)

[2] Recursive → Root server: "Where is .com?"
    Root: "Ask the .com TLD server at a.gtld-servers.net"

[3] Recursive → .com TLD server: "Where is example.com?"
    TLD: "Ask example.com's authoritative server at ns1.example.com"

[4] Recursive → ns1.example.com: "Where is www.example.com?"
    Auth: "It's at 93.184.216.34"

[5] Recursive caches it, returns to your OS

[6] Browser uses 93.184.216.34
```

### Diagram

```
┌─────────┐    ┌─────────┐    ┌──────┐    ┌──────┐    ┌──────────┐
│ Browser │ →→ │   OS    │ →→ │Resolv│ →→ │ Root │
└─────────┘    └─────────┘    │ er   │    └──────┘
                              │      │    ┌──────┐
                              │      │ →→ │ TLD  │
                              │      │    └──────┘
                              │      │    ┌──────┐
                              │      │ →→ │ Auth │
                              └──────┘    └──────┘
```

### Iterative vs Recursive

- **Recursive:** Resolver does all the work, returns final answer
- **Iterative:** Each server returns "ask the next one"

In practice: client → recursive (recursive query); recursive → authoritative chain (iterative).

---

## Record Types

DNS database mein **records** hote hain — different types alag information ke liye.

### A Record

**Matlab:** Domain → IPv4 address.

```
example.com.    A    93.184.216.34
```

### AAAA Record (Quad-A)

**Matlab:** Domain → IPv6 address.

```
example.com.    AAAA    2606:2800:220:1:248:1893:25c8:1946
```

### CNAME Record

**Matlab:** "Canonical Name" — domain → another domain (alias).

```
www.example.com.    CNAME    example.com.
api.example.com.    CNAME    api-lb.cloudprovider.com.
```

⚠️ **Rules:**
- Can't have CNAME at apex (root domain) — `example.com` can't be CNAME
- Can't coexist with other records (no MX + CNAME on same name)
- Causes additional DNS lookup

### MX Record

**Matlab:** Mail eXchanger — email server for the domain.

```
example.com.    MX    10  mail1.example.com.
example.com.    MX    20  mail2.example.com.
```

- **Priority** (lower = preferred)
- Multiple MX → fallback

### TXT Record

**Matlab:** Arbitrary text — verification, SPF, DKIM, DMARC.

```
example.com.    TXT    "v=spf1 include:_spf.google.com ~all"
example.com.    TXT    "google-site-verification=abc123"
```

### NS Record

**Matlab:** Name Server — which servers are authoritative for this zone.

```
example.com.    NS    ns1.example.com.
example.com.    NS    ns2.example.com.
```

### SOA Record

**Matlab:** Start Of Authority — zone metadata.

```
example.com.    SOA    ns1.example.com. admin.example.com. (
                       2024010101  ; serial
                       7200        ; refresh
                       3600        ; retry
                       1209600     ; expire
                       3600 )      ; minimum TTL
```

Used by secondary nameservers to know when to re-fetch zone data.

### PTR Record

**Matlab:** Reverse DNS — IP → domain name.

```
34.216.184.93.in-addr.arpa.    PTR    example.com.
```

Used for: email server reputation, logs.

### SRV Record

**Matlab:** Service location — protocol + port for a service.

```
_sip._tcp.example.com.    SRV    10 5 5060 sipserver.example.com.
                                  │  │  │
                                  │  │  └─ port
                                  │  └──── weight
                                  └─────── priority
```

### CAA Record

**Matlab:** Certification Authority Authorization — which CAs can issue certs.

```
example.com.    CAA    0 issue "letsencrypt.org"
```

### Common Records Summary

| Type | Purpose | Example value |
|------|---------|---------------|
| A | IPv4 | `93.184.216.34` |
| AAAA | IPv6 | `2606:2800::1` |
| CNAME | Alias | `target.example.com.` |
| MX | Mail server | `10 mail.example.com.` |
| TXT | Text data | `"v=spf1 ..."` |
| NS | Nameserver | `ns1.example.com.` |
| SOA | Zone metadata | (multi-field) |
| PTR | Reverse DNS | `example.com.` |
| SRV | Service location | (priority, port, target) |
| CAA | CA permission | `0 issue "letsencrypt.org"` |

---

## TTL (Time-To-Live)

**Matlab:** Record ko cache mein kitni der rakhna hai (in seconds).

```
example.com.    300    A    93.184.216.34
                ↑
              TTL = 5 minutes
```

### Trade-off

| Low TTL (e.g., 60s) | High TTL (e.g., 86400s = 1 day) |
|----------------------|----------------------------------|
| Quick changes propagate | Slow to update |
| More DNS queries | Less load on resolvers |
| Better for failover | Better for static records |

### Common Values

| TTL | Use case |
|-----|----------|
| 60-300s | Frequent changes, failover |
| 3600s (1h) | Default for many |
| 86400s (1d) | Stable records (NS, root) |

### TTL Reduction Strategy (Pre-Migration)

Before changing IP:
1. **Day -7:** Reduce TTL from 86400 to 300
2. Wait for old caches to expire (1 day)
3. **Day 0:** Change record
4. **Day +1:** Increase TTL back to 86400

---

## DNS Caching

Multiple cache layers — **DNS is heavily cached**.

```
┌────────────────────┐
│ Browser cache      │  (~minutes, browser-controlled)
├────────────────────┤
│ OS resolver cache  │  (system-wide, e.g., systemd-resolved)
├────────────────────┤
│ Recursive resolver │  (ISP, 8.8.8.8 — TTL-based)
├────────────────────┤
│ Authoritative      │  (no cache — source of truth)
└────────────────────┘
```

### Inspect

```bash
# macOS
sudo killall -HUP mDNSResponder        # flush
scutil --dns                            # show

# Linux (systemd-resolved)
resolvectl flush-caches
resolvectl statistics

# Windows
ipconfig /flushdns
ipconfig /displaydns
```

### Browser Caching

- Chrome: `chrome://net-internals/#dns` to see/clear

### CDN / Recursive Caching

Not under your control — TTL is your only lever.

---

## Negative Caching

**Matlab:** Failed lookups (NXDOMAIN — "doesn't exist") bhi cache hote hain. Repeated bad queries spare karne ke liye.

### How

When authoritative says "no such record":
```
example.com.    SOA    ... 3600 (minimum TTL)
```

The SOA's **minimum TTL** field controls negative cache duration.

### Why Important?

- Prevents flood of failed queries
- But if you later create the record → users may not see for negative TTL

**Best practice:** Keep negative TTL short (e.g., 300s) during active development.

---

## DNSSEC

**Matlab:** **DNS Security Extensions** — DNS responses ko cryptographically sign karna. Tampering / spoofing prevent.

### Without DNSSEC

```
You query: example.com
Attacker (MITM) intercepts and returns: 1.2.3.4 (attacker's server!)

You connect to attacker's IP, expecting example.com.
🚨 DNS spoofing / cache poisoning
```

### With DNSSEC

```
You query: example.com
Auth signs response with private key
Resolver verifies with public key (DNSKEY)
✅ Tamper-proof
```

### Records Added

| Record | Purpose |
|--------|---------|
| `DNSKEY` | Public key for the zone |
| `DS` | Delegation Signer — links parent to child key |
| `RRSIG` | Signature over a record set |
| `NSEC` / `NSEC3` | "Authenticated denial" — prove a name doesn't exist |

### Chain of Trust

```
Root signs .com's DS → .com signs example.com's DS → example.com signs its records
```

### Adoption

- ~30-40% of zones globally
- Some TLDs enforce; many don't
- DNS-over-HTTPS often combined for end-to-end security

### Verify

```bash
dig +dnssec example.com
# Look for "ad" (authenticated data) flag
```

---

## DNS-Based Load Balancing

**Matlab:** DNS layer pe traffic distribute karna — ek domain ke liye multiple IPs return.

### 1. Round-Robin DNS

```
example.com.    A    1.2.3.4
example.com.    A    5.6.7.8
example.com.    A    9.10.11.12
```

DNS server rotates order — different clients get different first IPs.

❌ Limitations:
- No health check (returns dead IP)
- Caching breaks rotation
- Uneven distribution

### 2. Geographic / GeoDNS

Resolver location ke basis par different IP return.

```
US client    → 52.x.x.x    (US data center)
India client → 13.x.x.x    (Mumbai data center)
EU client    → 18.x.x.x    (Frankfurt)
```

Used by AWS Route 53, Cloudflare, etc.

### 3. Latency-Based

Closest in **network latency** terms.

### 4. Weighted

```
50% traffic → 1.2.3.4
30% traffic → 5.6.7.8
20% traffic → 9.10.11.12
```

Useful for canary deployments, blue-green rollouts.

### 5. Failover (Health Checks)

Provider monitors targets; serves only healthy ones.

```
Primary 1.2.3.4 (healthy) → returned
Primary fails → 5.6.7.8 returned (failover)
```

### Limitations of DNS LB

- TTL-bound (fast failover requires low TTL → more queries)
- Browser/OS cache may stick to old IP
- Not real-time (vs L4/L7 load balancers)

→ Combine DNS LB with L4/L7 LB for production.

---

## Split-Horizon DNS

**Matlab:** Same domain, **different answers** based on who's asking.

### Use Case

```
Internal corporate network:
  api.example.com → 10.0.0.5 (internal IP)

Internet:
  api.example.com → 52.x.x.x (public IP)
```

### Implementation

- Internal DNS server returns internal records
- External DNS server returns public records
- Routing based on source IP / network

### Why?

- Internal services not exposed
- Faster routing within VPC
- Different security policies

---

## DNS over HTTPS / TLS

Encrypt DNS queries (otherwise plaintext over UDP).

### DoT — DNS over TLS

- Port 853
- Standard TLS
- Cleaner separation

### DoH — DNS over HTTPS

- Port 443 (looks like HTTPS!)
- Harder to block / detect
- Adopted by Cloudflare (`1.1.1.1`), Google (`8.8.8.8`), Mozilla, Apple iOS 14+, etc.

### Why?

- ❌ Plaintext DNS → ISP / governments can spy
- ❌ DNS hijacking by ISPs
- ✅ DoH/DoT → encrypted, harder to manipulate

### Configure (Browser)

Chrome/Firefox: built-in DNS-over-HTTPS settings.

---

## Useful Tools & Debugging

### `dig`

Most powerful DNS tool.

```bash
dig example.com                  # A record
dig example.com AAAA             # IPv6
dig example.com MX               # mail
dig example.com ANY              # all (often refused now)
dig @1.1.1.1 example.com         # specify resolver
dig +trace example.com           # show full chain
dig +short example.com           # just IP
dig +dnssec example.com          # show DNSSEC
dig -x 8.8.8.8                   # reverse lookup (PTR)
```

### `nslookup`

Older but ubiquitous.

```bash
nslookup example.com
nslookup example.com 8.8.8.8
> set type=MX
> example.com
```

### `host`

Simple, scriptable.

```bash
host example.com
host -t MX example.com
```

### `whois`

Domain registration info.

```bash
whois example.com
```

### Debugging Common Issues

#### "Site unreachable"
```bash
dig site.com               # is DNS resolving?
dig +trace site.com        # full chain
ping site.com              # is host reachable?
```

#### "DNS_PROBE_FINISHED_NXDOMAIN"
- Typo? Check spelling
- Domain expired? `whois`
- DNS propagation? Try `dig @8.8.8.8`

#### "Stale DNS"
```bash
# Flush local cache
sudo killall -HUP mDNSResponder      # macOS
ipconfig /flushdns                    # Windows
resolvectl flush-caches               # Linux
```

#### Test Propagation
- [dnschecker.org](https://dnschecker.org)
- `dig @8.8.8.8 example.com` (Google)
- `dig @1.1.1.1 example.com` (Cloudflare)

---

## Real-World Tips

### 1. Lower TTL Before Changes

Plan migrations: reduce TTL → wait → migrate → restore TTL.

### 2. Don't Use CNAME at Apex

`example.com` can't be CNAME. Use ANAME / ALIAS (provider-specific) or apex flattening.

### 3. Monitor DNS Health

- Pingdom, Uptime Robot, Datadog
- Alert on resolution failures

### 4. Use Authoritative DNS Provider with Anycast

Cloudflare, Route 53, NS1 — anycast means low-latency globally.

### 5. Add CAA Records

Restrict which CAs can issue certs for your domain.

### 6. SPF / DKIM / DMARC

For email deliverability (mostly TXT records).

### 7. Don't Forget IPv6

Add AAAA records — IPv6 increasingly common.

### 8. Test Before Going Live

```bash
dig @your-new-ns example.com
```

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| **A** | IPv4 |
| **AAAA** | IPv6 |
| **CNAME** | Alias (not at apex) |
| **MX** | Mail server (priority + target) |
| **TXT** | Free text (SPF, DKIM, verification) |
| **NS** | Authoritative nameservers |
| **SOA** | Zone metadata (serial, TTLs) |
| **PTR** | Reverse DNS |
| **TTL** | Cache duration in seconds |
| **NXDOMAIN** | Domain doesn't exist |
| **DNSSEC** | Cryptographic signatures |
| **DoH/DoT** | Encrypted DNS |
| **GeoDNS** | Location-based answers |
| **Split-horizon** | Different answers for internal vs external |

---

## Practice

1. Use `dig +trace example.com` and explain each step.
2. Check the MX, TXT records of your favorite email provider.
3. Set up a domain with multiple A records → observe round-robin.
4. Reduce TTL on a record, change IP, measure propagation time across resolvers.
5. Verify DNSSEC on a domain that supports it.
6. Test DoH with `curl --doh-url https://1.1.1.1/dns-query https://example.com`.
