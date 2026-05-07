# Networking & Protocols

Backend developer ke liye **networking ka deep-dive** — HTTP/HTTPS, TCP/IP, DNS, load balancing & proxies, WebSocket/SSE, aur SSL/TLS. Sab Hinglish mein, code + config examples ke saath.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | HTTP / HTTPS Deep Dive | [01-HTTP-HTTPS-Deep-Dive.md](./01-HTTP-HTTPS-Deep-Dive.md) | Not Started |
| 2 | TCP / IP Fundamentals | [02-TCP-IP-Fundamentals.md](./02-TCP-IP-Fundamentals.md) | Not Started |
| 3 | DNS | [03-DNS.md](./03-DNS.md) | Not Started |
| 4 | Load Balancing & Proxies | [04-Load-Balancing-and-Proxies.md](./04-Load-Balancing-and-Proxies.md) | Not Started |
| 5 | WebSocket & SSE | [05-WebSocket-and-SSE.md](./05-WebSocket-and-SSE.md) | Not Started |
| 6 | SSL / TLS | [06-SSL-TLS.md](./06-SSL-TLS.md) | Not Started |

---

## What's Inside Each File?

### [01 — HTTP / HTTPS Deep Dive](./01-HTTP-HTTPS-Deep-Dive.md)
- Request/response structure, methods, status codes
- Headers (request, response, security)
- Cookies (`HttpOnly`, `Secure`, `SameSite`)
- Caching headers (`Cache-Control`, `ETag`, `Last-Modified`, `If-None-Match`)
- HTTP keep-alive
- HTTP/1.1 vs HTTP/2 (multiplexing, HPACK, server push, binary framing)
- HTTP/3 (QUIC, UDP-based, no TCP HOL blocking)
- HTTPS, HSTS, mixed content

### [02 — TCP / IP Fundamentals](./02-TCP-IP-Fundamentals.md)
- TCP/IP stack layers
- TCP vs UDP (reliability vs speed)
- TCP 3-way handshake (`SYN → SYN-ACK → ACK`)
- TCP 4-way termination (`FIN → ACK → FIN → ACK`)
- Flow control (sliding window)
- Congestion control (slow start, AIMD, CUBIC, BBR)
- Socket programming basics (TCP & UDP)
- TIME_WAIT state and tuning
- Linux TCP kernel tuning, troubleshooting tools

### [03 — DNS](./03-DNS.md)
- DNS hierarchy (root → TLD → authoritative)
- Resolution flow (recursive resolver path)
- Record types (A, AAAA, CNAME, MX, TXT, NS, SOA, PTR, SRV, CAA)
- TTL & migration strategy
- Multi-level caching
- Negative caching
- DNSSEC (signing, chain of trust)
- DNS-based load balancing (round-robin, GeoDNS, weighted, failover)
- Split-horizon DNS
- DNS over HTTPS / TLS (DoH, DoT)
- `dig`, `nslookup`, debugging

### [04 — Load Balancing & Proxies](./04-Load-Balancing-and-Proxies.md)
- Forward vs reverse proxy
- L4 vs L7 load balancing
- Algorithms (round robin, least conn, IP hash, consistent hash)
- Nginx (reverse proxy, SSL termination, rate limiting, caching)
- HAProxy (L4/L7, ACLs, health checks, stats page)
- Envoy proxy (service mesh, sidecar, circuit breakers)
- X-Forwarded-For, X-Real-IP, security pitfalls
- Health checks (active vs passive, liveness vs readiness)

### [05 — WebSocket & SSE](./05-WebSocket-and-SSE.md)
- HTTP polling vs long polling vs SSE vs WebSocket
- SSE — server push, EventSource API, auto-reconnect
- WebSocket handshake (HTTP Upgrade → 101 Switching)
- Frames, opcodes, ping/pong, close codes
- Use cases (chat, live feeds, collab)
- Spring WebSocket configuration
- STOMP protocol (pub-sub over WebSocket)
- Scaling (sticky sessions, Redis pub-sub, Nginx config gotchas)

### [06 — SSL / TLS](./06-SSL-TLS.md)
- Symmetric vs asymmetric encryption
- TLS 1.2 vs TLS 1.3 handshake (2-RTT vs 1-RTT)
- Forward secrecy
- X.509 certificates, SAN, wildcards
- Certificate Authority (CA) chain of trust
- OCSP stapling, revocation
- SNI — Server Name Indication
- ALPN — protocol negotiation in handshake
- mTLS — mutual TLS for service-to-service
- Recommended Nginx config, debugging tools

---

## Recommended Learning Order

```
1. TCP/IP Fundamentals (02)   ← Foundation — packets, ports, sockets
2. HTTP / HTTPS (01)          ← App-layer protocol
3. SSL/TLS (06)               ← How HTTPS actually works
4. DNS (03)                   ← How URLs become IPs
5. Load Balancing (04)        ← Production scaling
6. WebSocket & SSE (05)       ← Real-time apps
```

---

## Quick Reference Index

### "Mujhe X karna hai" → kahan dekhun?

| Task | File | Section |
|------|------|---------|
| Cache responses correctly | 01 | Caching Headers |
| Choose HTTP version | 01 | HTTP/1.1 vs /2 vs /3 |
| Set secure cookie | 01 | Cookies (SameSite, HttpOnly) |
| Debug slow connection | 02 | TCP handshake + Keep-Alive |
| Tune Linux for high concurrency | 02 | Linux TCP Tuning |
| Reduce TIME_WAIT pile-up | 02 | TIME_WAIT State |
| Migrate domain to new IP | 03 | TTL Reduction Strategy |
| Set up email SPF/DKIM | 03 | TXT Records |
| Configure GeoDNS | 03 | DNS-Based Load Balancing |
| Set up Nginx reverse proxy | 04 | Nginx |
| Rate limit per IP | 04 | Nginx Rate Limiting |
| Health check backend | 04 | Health Checks |
| Get real client IP | 04 | X-Forwarded-For |
| Build chat app | 05 | WebSocket |
| Send live notifications | 05 | SSE |
| Scale WebSockets | 05 | Scaling WebSockets |
| Issue free TLS cert | 06 | Let's Encrypt (CA section) |
| Configure modern HTTPS | 06 | Recommended Configuration |
| Service mesh auth | 06 | mTLS |
| Host multiple HTTPS sites on one IP | 06 | SNI |

---

## Tools Reference

### HTTP / HTTPS
- `curl -v https://...` — manual request, verbose
- Browser DevTools → Network tab
- `httpie` — friendlier curl
- `wrk`, `ab` — load testing
- [ssllabs.com/ssltest](https://www.ssllabs.com/ssltest/) — TLS audit

### TCP / IP
- `ss -tan`, `netstat -an` — connection states
- `tcpdump`, `wireshark` — packet capture
- `nc` (netcat) — manual TCP/UDP
- `telnet host port` — TCP probe
- `mtr`, `traceroute` — path analysis
- `iperf3` — bandwidth testing

### DNS
- `dig`, `nslookup`, `host` — query records
- `dig +trace` — full resolution chain
- `whois` — domain registration
- [dnschecker.org](https://dnschecker.org) — propagation
- [crt.sh](https://crt.sh) — certificate transparency

### TLS
- `openssl s_client -connect host:443` — manual TLS
- `openssl x509 -in cert.pem -text -noout` — view cert
- [ssl-config.mozilla.org](https://ssl-config.mozilla.org) — best-practice configs
- `certbot` — Let's Encrypt automation

---

## Companion Folders

- [API Design & Architecture](../API-Design-&-Architecture/) — REST, GraphQL, pagination, rate limiting, idempotency
- [Database Mastery](../Database-Mastery/) — SQL, indexing, transactions, NoSQL, pooling, modeling

---

## Status Tracker

```
[ ] 01 — HTTP / HTTPS Deep Dive
[ ] 02 — TCP / IP Fundamentals
[ ] 03 — DNS
[ ] 04 — Load Balancing & Proxies
[ ] 05 — WebSocket & SSE
[ ] 06 — SSL / TLS
```

Happy learning! 🌐
