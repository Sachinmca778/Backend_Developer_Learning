# SSL / TLS

## Status: Not Started

---

## Table of Contents

1. [SSL/TLS Kya Hai?](#ssltls-kya-hai)
2. [Symmetric vs Asymmetric Encryption](#symmetric-vs-asymmetric-encryption)
3. [TLS Handshake (Detailed)](#tls-handshake-detailed)
4. [Certificates](#certificates)
5. [Certificate Authority (CA) & Chain of Trust](#certificate-authority-ca--chain-of-trust)
6. [SNI — Server Name Indication](#sni--server-name-indication)
7. [ALPN — Application-Layer Protocol Negotiation](#alpn--application-layer-protocol-negotiation)
8. [mTLS — Mutual TLS](#mtls--mutual-tls)
9. [TLS 1.3 Improvements](#tls-13-improvements)
10. [Tools & Debugging](#tools--debugging)

---

## SSL/TLS Kya Hai?

**Matlab:** **Transport Layer Security** (TLS) — TCP ke upar encryption + authentication. SSL is its predecessor (now deprecated). HTTPS = HTTP + TLS.

```
Without TLS:
  Client →→ "password=hello" →→ Server
            ↑ anyone on path can read!

With TLS:
  Client →→ "asdf91j2k3jhasdf" →→ Server
            ↑ encrypted, useless to attacker
```

### What TLS Provides

| Property | Meaning |
|----------|---------|
| **Confidentiality** | Encrypted — eavesdroppers can't read |
| **Integrity** | Tampering detected (MAC) |
| **Authentication** | Verify you're talking to the real server |

### Versions

| Version | Year | Status |
|---------|------|--------|
| SSL 2.0 | 1995 | ❌ Insecure, deprecated |
| SSL 3.0 | 1996 | ❌ POODLE attack — disabled |
| TLS 1.0 | 1999 | ❌ Deprecated 2020 |
| TLS 1.1 | 2006 | ❌ Deprecated 2020 |
| TLS 1.2 | 2008 | ✅ Widely used |
| TLS 1.3 | 2018 | ✅ Recommended (faster, simpler) |

**Use TLS 1.2 minimum, prefer 1.3.**

---

## Symmetric vs Asymmetric Encryption

### Symmetric Encryption

**Matlab:** Ek hi key encrypt aur decrypt dono karti hai.

```
Plaintext → [Encrypt with K] → Ciphertext
Ciphertext → [Decrypt with K] → Plaintext
```

**Pros:** Fast (CPU-friendly), good for bulk data.
**Cons:** Key kaise share karein securely?

**Algorithms:** AES (128/256-bit), ChaCha20.

### Asymmetric Encryption (Public Key)

**Matlab:** Do keys — public key (sab dekh sakte hain) and private key (secret).

```
Encrypt with Public Key  → only Private Key can Decrypt
Sign    with Private Key → Public Key can Verify
```

**Pros:** No prior shared secret needed.
**Cons:** Slow (~1000x slower than symmetric).

**Algorithms:** RSA, ECDSA, Ed25519.

### Hybrid Approach in TLS

TLS uses **both**:

1. **Asymmetric** — for handshake (key exchange + auth)
2. **Symmetric** — for actual data (after key derived)

```
Handshake (slow, one-time):
  Use RSA / ECDHE → both sides agree on a shared secret K

Data transfer (fast):
  Encrypt all messages with AES using K
```

Best of both worlds.

---

## TLS Handshake (Detailed)

### TLS 1.2 Handshake (Classic)

```
Client                                 Server
  │                                       │
  │── ClientHello ───────────────────────→│
  │   - TLS versions supported            │
  │   - Cipher suites supported           │
  │   - Random number (client_random)     │
  │   - Extensions (SNI, ALPN, ...)       │
  │                                       │
  │←── ServerHello ──────────────────────│
  │    - Chosen TLS version               │
  │    - Chosen cipher suite              │
  │    - Random number (server_random)    │
  │                                       │
  │←── Certificate ──────────────────────│
  │    - Server's X.509 cert chain        │
  │                                       │
  │←── ServerKeyExchange (for ECDHE) ────│
  │    - Server's ephemeral DH params     │
  │    - Signed with cert's private key   │
  │                                       │
  │←── ServerHelloDone ──────────────────│
  │                                       │
  │── ClientKeyExchange ─────────────────→│
  │   - Client's DH public params         │
  │                                       │
  │  (Both compute shared secret)         │
  │  (Both derive session keys from       │
  │   shared secret + random numbers)     │
  │                                       │
  │── ChangeCipherSpec ──────────────────→│
  │── Finished (encrypted) ──────────────→│
  │                                       │
  │←── ChangeCipherSpec ─────────────────│
  │←── Finished (encrypted) ─────────────│
  │                                       │
  │ ====== Encrypted Application Data ===│
```

**Total: 2 RTT before first byte of data.**

### TLS 1.3 Handshake (Streamlined)

```
Client                                 Server
  │                                       │
  │── ClientHello ───────────────────────→│
  │   - TLS 1.3                           │
  │   - Cipher suites                     │
  │   - Key shares (DH params)            │
  │                                       │
  │←── ServerHello ──────────────────────│
  │    - TLS 1.3 chosen                  │
  │    - Cipher chosen                    │
  │    - Key share                        │
  │                                       │
  │   {EncryptedExtensions}               │
  │   {Certificate}                       │
  │   {CertificateVerify}                 │
  │   {Finished}        ← all encrypted! │
  │←─────────────────────────────────────│
  │                                       │
  │── {Finished} ────────────────────────→│
  │                                       │
  │ ====== Encrypted Application Data ===│
```

**Total: 1 RTT** (or **0-RTT** if resuming previous session!)

### Key Concepts

#### Cipher Suite

A combination of algorithms. Format:

```
TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
    │     │      │             │
    │     │      │             └─ MAC / hash
    │     │      └─ Symmetric encryption (bulk data)
    │     └─ Authentication (cert signing)
    └─ Key exchange
```

**TLS 1.3 simplified:**
```
TLS_AES_256_GCM_SHA384
TLS_CHACHA20_POLY1305_SHA256
```

(Key exchange + auth negotiated separately.)

#### Forward Secrecy

If server's private key leaks, **past sessions still safe** because each session used a unique ephemeral key.

- ECDHE / DHE → ✅ provides forward secrecy
- Plain RSA key exchange → ❌ no forward secrecy (deprecated in TLS 1.3)

#### Session Resumption

Avoid full handshake on reconnect.

- **Session IDs** (TLS 1.2) — server caches state
- **Session Tickets** (TLS 1.2) — encrypted state given to client
- **PSK** (TLS 1.3) — pre-shared key from previous session

→ Enables 0-RTT in TLS 1.3.

---

## Certificates

**Matlab:** Cryptographic document jo bind karta hai **public key + identity (domain name)**, signed by a Certificate Authority (CA).

### X.509 Format

Standard certificate format. Contains:

```
Subject:        CN=example.com, O=Example Inc., C=IN
Issuer:         CN=Let's Encrypt Authority X3
Valid From:     Jan 1, 2024
Valid Until:    Apr 1, 2024
Public Key:     (RSA 2048-bit / ECDSA P-256)
Signature:      (CA signs hash of all above)
Serial Number:  abc123...
Extensions:
  Subject Alternative Names: example.com, www.example.com, *.example.com
  Key Usage: Digital Signature, Key Encipherment
  Extended Key Usage: Server Authentication
```

### Subject Alternative Names (SAN)

Modern certs use SAN to list all valid domains.

```
SAN: example.com, www.example.com, api.example.com
```

⚠️ Common Name (CN) is **deprecated** for hostname matching — use SAN.

### Wildcard Certificates

```
*.example.com  ← matches: api.example.com, www.example.com
                ❌ doesn't match: foo.api.example.com (only one level)
```

### Multi-Domain (SAN) Certs

One cert covers many unrelated domains.

```
SAN: example.com, mybrand.in, foo.org
```

### File Formats

| Format | Description | Extensions |
|--------|-------------|------------|
| **PEM** | Base64 + headers | `.pem`, `.crt`, `.cer`, `.key` |
| **DER** | Binary | `.der`, `.cer` |
| **PKCS#12** | Cert + key bundle | `.p12`, `.pfx` |
| **PKCS#7** | Cert chain | `.p7b`, `.p7c` |

### PEM Example

```
-----BEGIN CERTIFICATE-----
MIIDXTCCAkWgAwIBAgIJAKZ... (base64)
-----END CERTIFICATE-----
```

### View Cert (OpenSSL)

```bash
# View cert details
openssl x509 -in cert.pem -text -noout

# Check expiry
openssl x509 -in cert.pem -noout -enddate

# Check issuer
openssl x509 -in cert.pem -noout -issuer
```

---

## Certificate Authority (CA) & Chain of Trust

**Matlab:** CA = trusted third party who **signs** certificates. Browsers/OSes pre-trust major CAs.

### Chain Structure

```
Root CA (self-signed, in browser trust store)
   │ signs
   ↓
Intermediate CA (operational)
   │ signs
   ↓
Your Server's Certificate
```

### Why Intermediate?

Root CA's private key is **kept offline** (super secure). Intermediate CA's used for daily signing. If compromised → revoke just the intermediate.

### Validation Flow

```
Server presents:
  - Server cert (signed by Intermediate CA)
  - Intermediate cert (signed by Root CA)

Client:
  1. Verify server cert signature using intermediate's public key  ✅
  2. Verify intermediate's signature using root's public key       ✅
  3. Root in client's trust store?                                 ✅
  → Trusted!
```

### Common CAs

- **Let's Encrypt** — free, automated, 90-day certs
- **DigiCert**, **GlobalSign**, **Sectigo** — paid, enterprise
- **Amazon Trust** (AWS), **Google Trust** — for their clouds
- **ZeroSSL** — free alternative

### Self-Signed Certificates

```bash
# Generate self-signed cert
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout server.key -out server.crt
```

⚠️ Browser warns "untrusted" — fine for dev / internal, never for public.

### Certificate Validation Levels

| Level | Verification | Issuance time | Cost |
|-------|-------------|---------------|------|
| **DV** (Domain Validation) | Own the domain | Minutes | Free (Let's Encrypt) |
| **OV** (Organization Validation) | Org legally exists | Days | $$ |
| **EV** (Extended Validation) | Deep org check | Weeks | $$$ |

(EV's "green bar" mostly removed from browsers now.)

### Revocation

Compromised cert needs to be invalidated.

#### CRL (Certificate Revocation List)

CA publishes list of revoked serials. Client downloads + checks. **Slow, large lists.**

#### OCSP (Online Certificate Status Protocol)

Client asks CA "is this cert revoked?" in real time.

**Issue:** Privacy + latency.

#### OCSP Stapling

Server fetches OCSP response, "staples" to TLS handshake. Client doesn't need to ask CA.

```nginx
ssl_stapling on;
ssl_stapling_verify on;
ssl_trusted_certificate /path/to/chain.pem;
```

#### CRLite / Mozilla's approach

Compressed revocation data shipped with browser updates.

---

## SNI — Server Name Indication

**Matlab:** Client TLS handshake ke **ClientHello** mein hostname bhejta hai → server multiple certificates host kar sakta hai on same IP.

### Problem (Pre-SNI)

```
Server IP: 1.2.3.4
Hosts: example.com, mybrand.in, foo.org

Client connects to 1.2.3.4 — which cert to send?
Server doesn't know yet!
(HTTP Host header comes AFTER TLS handshake — too late)
```

### Solution: SNI

Client tells server "I want example.com" in ClientHello.

```
ClientHello:
  Extension: SNI = example.com
```

Server picks correct cert. Done.

### Required For

- Virtual hosting on shared IPs
- CDNs serving many domains
- Cloud LBs with multiple HTTPS apps

### Encrypted SNI / ECH

⚠️ Plain SNI is **visible to network observers** → reveals which site you're visiting (even though traffic encrypted).

**ECH (Encrypted Client Hello)** — encrypts SNI too. Cloudflare, Mozilla pushing adoption.

---

## ALPN — Application-Layer Protocol Negotiation

**Matlab:** TLS handshake mein hi negotiate kar lo "HTTP/1.1 ya HTTP/2 ya HTTP/3?". One round-trip saved.

### How

```
ClientHello:
  ALPN extension: ["h2", "http/1.1"]    ← preferences

ServerHello:
  ALPN extension: ["h2"]                ← chosen
```

→ Both sides know to use HTTP/2 immediately.

### Common ALPN Values

| Value | Protocol |
|-------|----------|
| `h2` | HTTP/2 |
| `http/1.1` | HTTP/1.1 |
| `h3` | HTTP/3 |

### Configure (Nginx)

```nginx
listen 443 ssl http2;
# Nginx auto-advertises h2 + http/1.1 via ALPN
```

### Inspect

```bash
openssl s_client -connect example.com:443 -alpn h2,http/1.1
# Look for: ALPN protocol: h2
```

---

## mTLS — Mutual TLS

**Matlab:** **Both client and server present certificates.** Classic TLS = only server authenticates. mTLS = both sides verify each other.

### Standard TLS

```
Server: "Here's my cert"
Client: ✅ verify
Client connects (no cert from client)
```

### mTLS

```
Server: "Here's my cert. Show me yours too."
Client: "Here's my cert"
Server: ✅ verify
Two-way authentication.
```

### Use Cases

#### 1. Service-to-Service (Microservices)

In a service mesh, every service verifies every other service.

```
Order Service ↔ Payment Service
Both authenticated via mTLS — even if attacker gets inside network, can't impersonate.
```

Common with Istio, Linkerd, Consul Connect.

#### 2. Banking / High-Security APIs

Open Banking, payment gateways often require mTLS.

#### 3. IoT Devices

Each device has unique cert; server verifies it's authorized device.

#### 4. Internal APIs

Replace shared API keys with mTLS — better security.

### Setup

```nginx
server {
    listen 443 ssl;
    
    ssl_certificate /etc/ssl/server.crt;
    ssl_certificate_key /etc/ssl/server.key;
    
    # Enable mTLS
    ssl_client_certificate /etc/ssl/ca.crt;   # CA that signs client certs
    ssl_verify_client on;                      # require client cert
    
    location / {
        # Client cert details available
        proxy_set_header X-Client-Cert-DN $ssl_client_s_dn;
    }
}
```

### Generate Client Cert

```bash
# CA
openssl genrsa -out ca.key 2048
openssl req -x509 -new -key ca.key -out ca.crt -days 365

# Client cert
openssl genrsa -out client.key 2048
openssl req -new -key client.key -out client.csr
openssl x509 -req -in client.csr -CA ca.crt -CAkey ca.key -out client.crt
```

### Test

```bash
curl --cert client.crt --key client.key https://api.example.com/
```

---

## TLS 1.3 Improvements

### vs TLS 1.2

| Feature | TLS 1.2 | TLS 1.3 |
|---------|---------|---------|
| Handshake RTT | 2 | 1 (or 0 for resume) |
| Forward secrecy | Optional (cipher choice) | Mandatory |
| Cipher suites | Many (some weak) | Few, only strong |
| Static RSA | Allowed | Removed |
| Insecure ciphers | RC4, 3DES allowed | Removed |
| Compression | Allowed (CRIME attack) | Removed |
| Renegotiation | Allowed | Removed |
| Certificate encrypted | ❌ | ✅ (in handshake) |

### Mandatory Modern Crypto

TLS 1.3 only allows AEAD ciphers (authenticated encryption):
- AES-GCM
- ChaCha20-Poly1305

### 0-RTT Resumption

```
Client (resuming previous session):
  ClientHello + EncryptedAppData (in same packet!)
  
Server: 
  ServerHello + AppDataResponse
  
0-RTT for first byte of response.
```

⚠️ Replay risk for non-idempotent requests — use carefully.

---

## Tools & Debugging

### `openssl s_client`

Manual TLS connection.

```bash
openssl s_client -connect example.com:443
```

Shows cert, chain, cipher, etc.

```bash
# Test specific TLS version
openssl s_client -connect example.com:443 -tls1_2

# Test with SNI
openssl s_client -connect 1.2.3.4:443 -servername example.com

# Test ALPN
openssl s_client -connect example.com:443 -alpn h2

# Test mTLS
openssl s_client -connect example.com:443 \
    -cert client.crt -key client.key
```

### `curl`

```bash
# Verbose TLS
curl -v https://example.com

# Specific TLS version
curl --tlsv1.3 https://example.com

# Show cert
curl -v https://example.com 2>&1 | grep -i "certificate\|subject\|issuer"

# Skip verification (dev only!)
curl -k https://self-signed.example.com
```

### Online Tools

- **[ssllabs.com/ssltest](https://www.ssllabs.com/ssltest/)** — comprehensive grade
- **[hardenize.com](https://www.hardenize.com)** — security report
- **[crt.sh](https://crt.sh)** — CT log search (find issued certs)

### Common Errors

| Error | Cause |
|-------|-------|
| `unable to verify the first certificate` | Missing intermediate in chain |
| `certificate has expired` | Renew it |
| `Hostname mismatch` | Cert's SAN doesn't include the domain |
| `unable to get local issuer certificate` | Trust store outdated |
| `tlsv1 alert protocol version` | Client/server version mismatch |
| `wrong version number` | Plain HTTP hitting HTTPS port (or vice versa) |

### Cert Renewal Best Practices

- **Automate** — `certbot` for Let's Encrypt
- **Monitor expiry** — alerts at 30, 14, 7 days
- **Test reload** — Nginx `nginx -s reload` after cert update
- **Use shorter validity** — 90-day Let's Encrypt forces good hygiene

---

## Recommended Configuration

### Nginx (Modern, secure)

```nginx
server {
    listen 443 ssl http2;
    
    ssl_certificate /etc/ssl/example.crt;
    ssl_certificate_key /etc/ssl/example.key;
    
    # TLS versions
    ssl_protocols TLSv1.2 TLSv1.3;
    
    # Strong ciphers (TLS 1.2)
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305;
    ssl_prefer_server_ciphers off;
    
    # Performance
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 1d;
    ssl_session_tickets off;
    
    # OCSP stapling
    ssl_stapling on;
    ssl_stapling_verify on;
    ssl_trusted_certificate /etc/ssl/chain.pem;
    
    # Security headers
    add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload" always;
}
```

### Mozilla SSL Config Generator

[ssl-config.mozilla.org](https://ssl-config.mozilla.org/) — best-practice configs auto-generated for any web server.

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| **TLS** | Encryption + integrity + auth |
| **Symmetric** | Fast, shared key (AES, ChaCha20) |
| **Asymmetric** | Public/private (RSA, ECDSA) |
| **Hybrid** | Asymmetric to share, symmetric for data |
| **TLS 1.2** | 2-RTT handshake |
| **TLS 1.3** | 1-RTT or 0-RTT, mandatory FS, modern only |
| **Cipher suite** | Algorithms combo |
| **Forward secrecy** | Past sessions safe if key leaks |
| **X.509** | Cert format |
| **SAN** | List of valid domains |
| **Wildcard cert** | `*.example.com` (one level) |
| **CA chain** | Root → Intermediate → Server |
| **OCSP stapling** | Server pre-fetches revocation status |
| **SNI** | Hostname in ClientHello (multi-cert hosting) |
| **ALPN** | Negotiate HTTP version in TLS handshake |
| **mTLS** | Both sides authenticate |
| **HSTS** | Force HTTPS |

---

## Practice

1. Use `openssl s_client` to manually inspect a website's cert.
2. Generate self-signed cert + private key; serve a Node.js HTTPS app.
3. Set up Let's Encrypt with `certbot` for a real domain.
4. Configure Nginx with TLS 1.3 only; test with browser.
5. Implement mTLS between two services using OpenSSL-generated certs.
6. Run [ssllabs.com](https://www.ssllabs.com/ssltest/) on your site, aim for A+.
7. Trace TLS 1.2 vs 1.3 handshake in Wireshark — count RTTs.
