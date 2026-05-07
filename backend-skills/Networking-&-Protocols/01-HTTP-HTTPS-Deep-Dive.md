# HTTP / HTTPS Deep Dive

## Status: Not Started

---

## Table of Contents

1. [HTTP Kya Hai?](#http-kya-hai)
2. [Request / Response Structure](#request--response-structure)
3. [HTTP Methods](#http-methods)
4. [Status Codes](#status-codes)
5. [Headers](#headers)
6. [Cookies](#cookies)
7. [Caching Headers](#caching-headers)
8. [HTTP Keep-Alive](#http-keep-alive)
9. [HTTP/1.1](#http11)
10. [HTTP/2](#http2)
11. [HTTP/3 (QUIC)](#http3-quic)
12. [HTTPS](#https)

---

## HTTP Kya Hai?

**Matlab:** **HyperText Transfer Protocol** — client (browser) aur server ke beech mein communication ka rule. Stateless, text-based (HTTP/1.1) ya binary (HTTP/2+).

```
Client (Browser)              Server
   │                              │
   │── GET /index.html ────────→ │
   │                              │
   │←── HTTP/1.1 200 OK ──────── │
   │     <html>...</html>        │
```

**Key properties:**
- **Stateless** — har request independent (state cookies/sessions se maintain hota hai)
- **Request-response** model
- **Application layer** protocol (TCP/IP ke upar)
- Default port: **80** (HTTP), **443** (HTTPS)

---

## Request / Response Structure

### HTTP Request

```
GET /api/users/42 HTTP/1.1                ← Request line
Host: api.example.com                     ← Headers
User-Agent: Mozilla/5.0
Accept: application/json
Cookie: session_id=abc123
                                          ← Empty line
{ "key": "value" }                        ← Body (optional)
```

**Components:**
1. **Request line** — `METHOD path HTTP-VERSION`
2. **Headers** — metadata (`key: value` pairs)
3. **Empty line** — separator
4. **Body** — optional payload

### HTTP Response

```
HTTP/1.1 200 OK                           ← Status line
Content-Type: application/json            ← Headers
Content-Length: 38
Cache-Control: max-age=3600
Set-Cookie: session_id=xyz789

{ "id": 42, "name": "Rahul" }             ← Body
```

**Components:**
1. **Status line** — `HTTP-VERSION status-code reason`
2. **Headers**
3. **Empty line**
4. **Body**

---

## HTTP Methods

| Method | Purpose | Idempotent | Safe | Body |
|--------|---------|------------|------|------|
| `GET` | Retrieve resource | ✅ | ✅ | ❌ |
| `HEAD` | GET without body | ✅ | ✅ | ❌ |
| `POST` | Create resource | ❌ | ❌ | ✅ |
| `PUT` | Replace resource | ✅ | ❌ | ✅ |
| `PATCH` | Partial update | ⚠️ | ❌ | ✅ |
| `DELETE` | Remove resource | ✅ | ❌ | ⚠️ |
| `OPTIONS` | Discover allowed methods | ✅ | ✅ | ❌ |
| `CONNECT` | Tunnel (HTTPS proxy) | ❌ | ❌ | ❌ |
| `TRACE` | Diagnostic loop-back | ✅ | ✅ | ❌ |

- **Safe** = no side effects on server
- **Idempotent** = repeating gives same result

---

## Status Codes

| Range | Class | Meaning |
|-------|-------|---------|
| **1xx** | Informational | Request received, processing |
| **2xx** | Success | Request successfully processed |
| **3xx** | Redirection | Further action needed |
| **4xx** | Client Error | Request has problem |
| **5xx** | Server Error | Server failed |

### Common Codes

| Code | Name | Use |
|------|------|-----|
| `200` | OK | Standard success |
| `201` | Created | POST created resource |
| `204` | No Content | Success, no body |
| `301` | Moved Permanently | Permanent redirect |
| `302` | Found | Temporary redirect |
| `304` | Not Modified | Cached version still valid |
| `400` | Bad Request | Malformed input |
| `401` | Unauthorized | Auth required |
| `403` | Forbidden | Authenticated but no permission |
| `404` | Not Found | Resource doesn't exist |
| `409` | Conflict | State conflict (e.g., duplicate) |
| `422` | Unprocessable Entity | Validation failure |
| `429` | Too Many Requests | Rate limited |
| `500` | Internal Server Error | Generic server failure |
| `502` | Bad Gateway | Upstream server bad response |
| `503` | Service Unavailable | Server overloaded / down |
| `504` | Gateway Timeout | Upstream took too long |

---

## Headers

### Request Headers

| Header | Purpose |
|--------|---------|
| `Host` | Target hostname (mandatory in HTTP/1.1) |
| `User-Agent` | Client info |
| `Accept` | Acceptable response content types |
| `Accept-Encoding` | Acceptable compression (gzip, br) |
| `Accept-Language` | Preferred languages |
| `Authorization` | Auth credentials (`Bearer token`, `Basic ...`) |
| `Cookie` | Cookies for the server |
| `Content-Type` | Request body media type |
| `Content-Length` | Body size in bytes |
| `Referer` | URL of referring page |
| `If-None-Match` | Conditional: send only if ETag changed |
| `If-Modified-Since` | Conditional: send only if modified |

### Response Headers

| Header | Purpose |
|--------|---------|
| `Content-Type` | Response body media type |
| `Content-Length` | Body size |
| `Content-Encoding` | Compression used |
| `Cache-Control` | Caching directives |
| `ETag` | Resource version identifier |
| `Last-Modified` | Last change time |
| `Set-Cookie` | Set a cookie on client |
| `Location` | Used in redirects (3xx) |
| `WWW-Authenticate` | Auth challenge (with 401) |
| `Server` | Server software info |
| `Access-Control-Allow-Origin` | CORS |
| `Strict-Transport-Security` | HSTS (force HTTPS) |
| `X-Frame-Options` | Clickjacking protection |

### Custom Headers

`X-` prefix deprecated (RFC 6648). Use meaningful names:
```
Request-Id: abc123
Idempotency-Key: xyz456
Correlation-Id: trace789
```

---

## Cookies

**Matlab:** Server-set, client-stored small data — har subsequent request mein automatically bhejti hai.

### Setting a Cookie

```http
HTTP/1.1 200 OK
Set-Cookie: session_id=abc123; Max-Age=3600; HttpOnly; Secure; SameSite=Strict
```

### Cookie Attributes

| Attribute | Meaning |
|-----------|---------|
| `Max-Age=N` | Lifetime in seconds |
| `Expires=date` | Absolute expiry |
| `Domain=example.com` | Which domains can use |
| `Path=/api` | Which paths apply |
| `Secure` | Only over HTTPS |
| `HttpOnly` | JS can't access (XSS protection) |
| `SameSite=Strict/Lax/None` | CSRF protection |

### `SameSite` Explained

| Value | Behavior |
|-------|----------|
| `Strict` | Sirf same-site requests par bheja jaata hai (max security) |
| `Lax` | Top-level navigation par bhi (default in modern browsers) |
| `None` | Har request par (requires `Secure`) |

### Sending Cookie

```http
GET /api HTTP/1.1
Host: example.com
Cookie: session_id=abc123; theme=dark
```

### Session vs Persistent

- **Session cookie** — `Max-Age` / `Expires` nahi → browser close pe delete
- **Persistent cookie** — explicit lifetime → file pe save

---

## Caching Headers

Bandwidth save aur latency kam karne ke liye HTTP caching critical hai.

### `Cache-Control`

Sabse modern caching directive.

```http
Cache-Control: public, max-age=3600
Cache-Control: private, no-cache
Cache-Control: no-store
Cache-Control: max-age=86400, stale-while-revalidate=60
```

| Directive | Meaning |
|-----------|---------|
| `public` | Anyone can cache (CDN, browser) |
| `private` | Only end-user can cache (no shared cache) |
| `no-cache` | Cache stored, but **always revalidate** before use |
| `no-store` | Don't cache at all |
| `max-age=N` | Fresh for N seconds |
| `s-maxage=N` | Shared cache (CDN) max-age |
| `must-revalidate` | After expiry, must check with server |
| `immutable` | Won't change (e.g., versioned assets) |
| `stale-while-revalidate=N` | Use stale up to N sec while fetching new |

### `ETag` (Entity Tag)

**Matlab:** Resource ka unique fingerprint (hash). Conditional requests ke liye.

```http
HTTP/1.1 200 OK
ETag: "33a64df551"
Content-Length: 1024
[body]
```

Next request:
```http
GET /resource HTTP/1.1
If-None-Match: "33a64df551"
```

Response if not changed:
```http
HTTP/1.1 304 Not Modified
ETag: "33a64df551"
[no body — saves bandwidth!]
```

### `Last-Modified` + `If-Modified-Since`

Time-based version (less precise than ETag).

```http
HTTP/1.1 200 OK
Last-Modified: Wed, 21 Oct 2024 07:28:00 GMT

GET /resource HTTP/1.1
If-Modified-Since: Wed, 21 Oct 2024 07:28:00 GMT

HTTP/1.1 304 Not Modified
```

### Caching Strategy

```
Static assets (JS, CSS, images with hash in filename):
  Cache-Control: public, max-age=31536000, immutable

HTML pages:
  Cache-Control: no-cache  (or short max-age + ETag)

API responses (sensitive):
  Cache-Control: private, no-store
```

---

## HTTP Keep-Alive

**Matlab:** Single TCP connection par multiple HTTP requests bhejna — connection re-use.

### Without Keep-Alive (HTTP/1.0)

```
Request 1: open TCP → request → response → close TCP
Request 2: open TCP → request → response → close TCP
Request 3: open TCP → request → response → close TCP

Each request: TCP handshake + (TLS handshake) overhead
```

### With Keep-Alive

```
Request 1: open TCP → request → response
Request 2:           → request → response
Request 3:           → request → response  → close TCP (after timeout)
```

### Headers

```http
Connection: keep-alive
Keep-Alive: timeout=5, max=100
```

- `timeout` — idle time before close (seconds)
- `max` — max requests per connection

In **HTTP/1.1, keep-alive is default**. Use `Connection: close` to opt out.

---

## HTTP/1.1

### Features

- Persistent connections (keep-alive default)
- Chunked transfer encoding
- Pipelining (rarely used due to head-of-line blocking)
- Host header (mandatory) → multiple sites per IP
- Caching headers

### Limitations

- **Head-of-line (HOL) blocking** — request 1 slow → request 2 waits
- **One request per connection** at a time → browsers open 6+ parallel connections per host
- **Plain text headers** — every request sends repeated headers (cookies, user-agent)
- **No priority** for resources

### Pipelining

Theoretically allowed, but rarely worked in practice (HOL blocking, broken proxies).

```
Connection 1: req1 → req2 → req3
              ↓
              resp1 → resp2 → resp3   (must be in order!)
```

---

## HTTP/2

**Matlab:** HTTP/1.1 ke major issues solve karta hai — 2015 mein release. **Binary protocol**.

### Key Features

#### 1. Binary Framing

Text ki bajaye binary frames. Smaller, easier to parse.

```
HTTP/1.1: "GET / HTTP/1.1\r\nHost: ..."  (text)
HTTP/2:   binary frames (HEADERS, DATA, SETTINGS, ...)
```

#### 2. Multiplexing

**Single TCP connection par multiple parallel streams.** No HOL blocking at HTTP layer.

```
Connection 1:
  Stream 1: req for /index.html
  Stream 2: req for /style.css       ← parallel!
  Stream 3: req for /app.js
  Stream 4: req for /logo.png

  All interleaved on same TCP
```

#### 3. Header Compression (HPACK)

Repeated headers (Cookie, User-Agent) compress hote hain.

```
Request 1: full headers (60 bytes)
Request 2: only diff (5 bytes!)
```

Static dictionary + dynamic table = huge savings.

#### 4. Server Push

Server proactively client ko related resources bhej deta hai.

```
Client: GET /index.html
Server: 
  PUSH /style.css  (sent before client asks)
  PUSH /app.js
  RESPONSE /index.html
```

⚠️ Server push largely deprecated — Chrome removed it (2022). Use `103 Early Hints` instead.

#### 5. Stream Priorities

Browser tells server: "JS is more important than image, send first".

### Limitations

- Still uses TCP → **TCP HOL blocking** still possible (lost packet blocks all streams on connection)
- Encryption typically required (browsers only support h2 over TLS)

---

## HTTP/3 (QUIC)

**Matlab:** TCP ki bajaye **UDP** par based! QUIC protocol use karta hai. 2022 mein RFC 9114.

### Why HTTP/3?

HTTP/2 ka biggest issue: **TCP HOL blocking**. Ek packet lost → pure connection ka throughput rukk jaata hai.

### QUIC Solves It

- **UDP-based** — application-layer protocol implements reliability
- **Independent streams** at transport layer → ek stream block hone se baaki affected nahi
- **Built-in encryption** (TLS 1.3 baked in — handshake combined with transport)
- **Connection migration** — IP change (WiFi to mobile) without reconnect
- **0-RTT** resumption (super fast reconnection)

### Connection Setup Comparison

```
HTTP/2 over TLS:
  TCP handshake     1 RTT
  TLS handshake     2 RTT (TLS 1.2) / 1 RTT (TLS 1.3)
  HTTP request      1 RTT
  Total: ~3-4 RTT for first byte

HTTP/3 (QUIC):
  QUIC + TLS combined  1 RTT (or 0-RTT if resumed)
  HTTP request         within same RTT
  Total: 1 RTT (or 0!)
```

### Diagram

```
HTTP/1.1:    HTTP → TCP → IP
HTTP/2:      HTTP/2 → TLS → TCP → IP
HTTP/3:      HTTP/3 → QUIC (incl. TLS) → UDP → IP
```

### Adoption

- Cloudflare, Google, Meta deploy at scale
- Browsers (Chrome, Firefox, Safari) support
- Most apps still use HTTP/2 — HTTP/3 deployment growing

---

## HTTPS

**Matlab:** HTTP **+ TLS encryption**. Confidentiality, integrity, authentication.

### Why HTTPS?

- 🔐 **Confidentiality** — encrypt traffic (no eavesdropping)
- ✅ **Integrity** — detect tampering
- 🪪 **Authentication** — verify server identity (via certificate)
- 🚀 **Modern features** — HTTP/2, brotli, service workers all need HTTPS

### TLS Handshake (Simplified)

```
Client                              Server
  │                                   │
  │── ClientHello (supported algos)→ │
  │                                   │
  │←── ServerHello + Certificate ─── │
  │                                   │
  │── Key exchange ────────────────→ │
  │                                   │
  │← Encrypted application data →   │
```

(Full details in `06-SSL-TLS.md`.)

### HSTS — Strict Transport Security

```http
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```

- Browser ko force karta hai HTTPS use karne ke liye
- Eliminates HTTP→HTTPS redirect MITM window

### Mixed Content

HTTPS page jab HTTP resources load karta hai → browser block karta hai.

```html
<!-- Bad on HTTPS page -->
<script src="http://cdn.example.com/lib.js"></script>

<!-- Good -->
<script src="https://cdn.example.com/lib.js"></script>
<!-- Or -->
<script src="//cdn.example.com/lib.js"></script>  (relative)
```

---

## Quick Comparison: HTTP/1.1 vs /2 vs /3

| Feature | HTTP/1.1 | HTTP/2 | HTTP/3 |
|---------|----------|--------|--------|
| **Released** | 1997 | 2015 | 2022 |
| **Format** | Text | Binary | Binary |
| **Transport** | TCP | TCP | UDP (QUIC) |
| **Multiplexing** | ❌ | ✅ | ✅ |
| **Header compression** | ❌ | HPACK | QPACK |
| **Server push** | ❌ | ✅ (deprecated) | ✅ (rare) |
| **Stream priority** | ❌ | ✅ | ✅ |
| **HOL blocking (TCP)** | ✅ | ✅ | ❌ |
| **Connection migration** | ❌ | ❌ | ✅ |
| **0-RTT resumption** | ❌ | ❌ | ✅ |
| **TLS** | Optional | Practically required | Built-in |

---

## Real-World Tips

### 1. Use HTTP/2 (or /3) on Production

Modern Nginx / Cloudflare / load balancers support easily.

```nginx
listen 443 ssl http2;
```

### 2. Always HTTPS

Free certs via Let's Encrypt. No excuse not to.

### 3. Long Cache for Static, Short for HTML

```
Static (with hash): max-age=31536000, immutable
HTML:               max-age=300 + ETag
```

### 4. Compress Responses

```nginx
gzip on;
gzip_types text/plain application/json text/css application/javascript;
brotli on;  # better than gzip for text
```

### 5. Reduce Headers

Cookie bloat, repeated user-agent → bandwidth waste.

### 6. Use ETags for API

GET response with stable data → ETag → 304 saves bandwidth.

### 7. Avoid Redirects in Hot Path

Each redirect = extra RTT.

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| **HTTP/1.1** | Text, keep-alive, HOL blocking |
| **HTTP/2** | Binary, multiplexing, HPACK |
| **HTTP/3** | QUIC over UDP, no TCP HOL |
| **GET** | Safe + idempotent |
| **POST** | Not idempotent — careful with retries |
| **Cookie** | Server sets, client sends back |
| **HttpOnly** | JS can't read (XSS protection) |
| **SameSite** | CSRF protection |
| **Cache-Control** | Modern caching directive |
| **ETag** | Resource fingerprint for revalidation |
| **304 Not Modified** | Cached version still valid |
| **Keep-Alive** | Re-use TCP connection |
| **HSTS** | Force HTTPS via header |

---

## Practice

1. Inspect HTTP headers in DevTools — identify cache, security headers.
2. Set up Nginx with HTTP/2 and gzip; measure load time vs HTTP/1.1.
3. Build a Cache-Control + ETag based API endpoint; verify 304 with `curl -H "If-None-Match: ..."`.
4. Set HSTS on your site; verify in browser DevTools.
5. Configure secure cookies (`HttpOnly`, `Secure`, `SameSite=Strict`); test from JS.
6. Compare TLS handshake time: HTTP/2 vs HTTP/3 (use `curl --http3` if supported).
