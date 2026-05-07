# Load Balancing & Proxies

## Status: Not Started

---

## Table of Contents

1. [Proxy Kya Hota Hai?](#proxy-kya-hota-hai)
2. [Forward Proxy vs Reverse Proxy](#forward-proxy-vs-reverse-proxy)
3. [Load Balancing Basics](#load-balancing-basics)
4. [L4 vs L7 Load Balancing](#l4-vs-l7-load-balancing)
5. [Load Balancing Algorithms](#load-balancing-algorithms)
6. [Nginx](#nginx)
7. [HAProxy](#haproxy)
8. [Envoy Proxy](#envoy-proxy)
9. [X-Forwarded-For & Real IP](#x-forwarded-for--real-ip)
10. [Health Checks](#health-checks)

---

## Proxy Kya Hota Hai?

**Matlab:** Ek intermediary server jo client aur destination ke beech mein baith ke traffic forward karta hai. Use cases: caching, security, anonymity, load balancing.

```
Client →→ Proxy →→ Server
       ←←       ←←
```

---

## Forward Proxy vs Reverse Proxy

### Forward Proxy

**Matlab:** **Client side** ka proxy. Client request proxy ke through bhejta hai jo destination tak forward karta hai. Server ko client ka pata nahi chalta.

```
Office computers → Forward Proxy → Internet
                     │
                     ↓ filtering, caching, blocking
```

**Use cases:**
- Corporate firewalls (block social media)
- Anonymity (Tor, VPN)
- Caching (squid in offices)
- Bypass geo-restrictions

**Examples:** Squid, Polipo, corporate VPN.

### Reverse Proxy

**Matlab:** **Server side** ka proxy. Client ko sirf proxy ka pata hota hai, real backend hidden. Multiple backends ke saamne ek face.

```
Client → Reverse Proxy →┬→ Server 1
                        ├→ Server 2
                        └→ Server 3
```

**Use cases:**
- Load balancing
- SSL termination
- Caching
- Compression
- Hide internal architecture
- Protect backend from direct exposure

**Examples:** Nginx, HAProxy, Envoy, AWS ALB.

### Comparison

| | Forward Proxy | Reverse Proxy |
|--|---------------|---------------|
| Whose proxy? | Client's | Server's |
| Who knows about it? | Client configured | Hidden from client |
| Sees | Client → multiple destinations | Multiple clients → backends |
| Common use | Privacy, filtering | LB, SSL, cache |

---

## Load Balancing Basics

**Matlab:** Multiple backend servers ke beech traffic distribute karna — performance + availability ke liye.

### Why?

- ✅ **Scale horizontally** — add more servers
- ✅ **High availability** — one fails, others continue
- ✅ **Performance** — distribute load
- ✅ **Maintenance** — drain a server, update, re-add
- ✅ **Geographic distribution** — route to nearest

### Architecture

```
                  ┌─────────┐
                  │  Load   │
        Clients →→│Balancer │
                  └─────────┘
                       │
            ┌──────────┼──────────┐
            ↓          ↓          ↓
        ┌──────┐  ┌──────┐  ┌──────┐
        │ App1 │  │ App2 │  │ App3 │
        └──────┘  └──────┘  └──────┘
```

---

## L4 vs L7 Load Balancing

### L4 — Transport Layer (TCP/UDP)

**Matlab:** Sirf IP + port dekhta hai. Connection-level. Application data nahi parse karta.

**Pros:**
- ⚡ Very fast (low overhead)
- 🔒 Can handle any TCP/UDP traffic (not just HTTP)
- 💪 High throughput

**Cons:**
- ❌ Can't make routing decisions on URL/headers
- ❌ Can't handle HTTPS without termination

**Examples:** AWS NLB, HAProxy in TCP mode, IPVS.

### L7 — Application Layer (HTTP, HTTPS)

**Matlab:** Application data parse karta hai — URL, headers, cookies, body. Smart routing.

**Pros:**
- ✅ Route by path, host, header
- ✅ SSL termination
- ✅ HTTP/2 multiplexing
- ✅ Caching, compression
- ✅ Web Application Firewall (WAF)

**Cons:**
- 🐢 Slower than L4 (parsing overhead)
- 🧠 More complex config

**Examples:** AWS ALB, Nginx, HAProxy in HTTP mode, Envoy.

### Comparison

| | L4 | L7 |
|--|----|----|
| Layer | Transport | Application |
| Sees | IP, port | URL, headers, body |
| Speed | Faster | Slower |
| Routing options | Basic | Rich |
| Protocols | Any TCP/UDP | HTTP, HTTPS, gRPC, WebSocket |
| Use cases | High-speed TCP, gaming | Web apps, APIs |

### Modern Reality

Most production setups: **L7 LB at edge** for HTTP, **L4 LB internally** for service-to-service.

---

## Load Balancing Algorithms

### 1. Round Robin

**Matlab:** Servers ko bari-bari traffic mil ti hai.

```
Request 1 → Server A
Request 2 → Server B
Request 3 → Server C
Request 4 → Server A
...
```

✅ Simple, fair if servers equal
❌ Doesn't account for load / different capacities

### 2. Weighted Round Robin

```
Server A weight=3, Server B weight=1
A, A, A, B, A, A, A, B, ...
```

Use when servers have different capacities.

### 3. Least Connections

**Matlab:** Server with fewest active connections gets next request.

```
Server A: 50 conns
Server B: 30 conns   ← gets next
Server C: 70 conns
```

✅ Good for long-lived requests (DB, WebSockets)
✅ Adapts to actual load

### 4. Least Response Time

Combines connection count + recent response times.

### 5. IP Hash

**Matlab:** Client IP ka hash → consistently same server.

```
hash(client_ip) % num_servers → server
```

✅ Sticky sessions (no DB-backed sessions needed)
❌ Uneven distribution if IPs cluster (NAT, proxies)

### 6. Consistent Hashing

For caches — when servers added/removed, only K/N keys redistribute.

```
ring of hash slots
each server takes a range
key → hash → maps to nearest server clockwise
```

Used in Redis Cluster, Memcached, CDNs.

### 7. Random / Power of Two Choices

Pick 2 servers randomly, route to less-loaded one. Surprisingly effective + simple.

### Algorithm Selection

| Scenario | Algorithm |
|----------|-----------|
| Equal servers, short requests | Round Robin |
| Different capacities | Weighted RR |
| Long-lived connections (WebSocket, DB) | Least Connections |
| Need sticky sessions | IP Hash |
| Distributed cache | Consistent Hash |

---

## Nginx

**Matlab:** High-performance reverse proxy + web server. Most popular L7 LB.

### Key Features

- ✅ Reverse proxy + load balancing
- ✅ Static file serving
- ✅ SSL/TLS termination
- ✅ Caching
- ✅ Rate limiting
- ✅ Compression (gzip, brotli)
- ✅ HTTP/2, HTTP/3 (with module)

### Basic Reverse Proxy

```nginx
http {
    upstream backend {
        server 10.0.0.1:8080;
        server 10.0.0.2:8080;
        server 10.0.0.3:8080;
    }
    
    server {
        listen 80;
        server_name example.com;
        
        location / {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
}
```

### Load Balancing Methods

```nginx
upstream backend {
    # Default: Round Robin
    server 10.0.0.1;
    server 10.0.0.2;
    
    # Weighted
    # server 10.0.0.1 weight=3;
    
    # Least connections
    # least_conn;
    
    # IP hash
    # ip_hash;
    
    # Health check (commercial Nginx Plus, or use passive)
    # max_fails=3 fail_timeout=30s
    server 10.0.0.3 max_fails=3 fail_timeout=30s;
    
    # Mark as backup
    server 10.0.0.4 backup;
}
```

### Static File Serving

```nginx
server {
    listen 80;
    
    root /var/www/html;
    
    location /static/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

### SSL Termination

```nginx
server {
    listen 443 ssl http2;
    server_name example.com;
    
    ssl_certificate /etc/ssl/certs/example.crt;
    ssl_certificate_key /etc/ssl/private/example.key;
    
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    
    # HSTS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    
    location / {
        proxy_pass http://backend;
    }
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name example.com;
    return 301 https://$host$request_uri;
}
```

### Rate Limiting

```nginx
http {
    # Define a zone: 10MB shared memory, 10 req/s per IP
    limit_req_zone $binary_remote_addr zone=mylimit:10m rate=10r/s;
    
    server {
        location /api/ {
            limit_req zone=mylimit burst=20 nodelay;
            proxy_pass http://backend;
        }
    }
}
```

### Caching

```nginx
http {
    proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=mycache:10m max_size=1g 
                     inactive=60m use_temp_path=off;
    
    server {
        location /api/products {
            proxy_cache mycache;
            proxy_cache_valid 200 5m;
            proxy_cache_use_stale error timeout updating;
            add_header X-Cache-Status $upstream_cache_status;
            
            proxy_pass http://backend;
        }
    }
}
```

### Useful Directives

```nginx
client_max_body_size 10M;            # max upload
proxy_connect_timeout 5s;
proxy_read_timeout 60s;
keepalive_timeout 65s;
gzip on;
gzip_types text/plain application/json text/css;
```

### Reload Without Downtime

```bash
nginx -t                # test config
nginx -s reload         # graceful reload
```

---

## HAProxy

**Matlab:** Battle-tested L4/L7 load balancer. Excellent for TCP, robust health checks, observability.

### Modes

- **TCP mode** (L4) — generic TCP traffic
- **HTTP mode** (L7) — HTTP-aware

### Basic HTTP Config

```
global
    daemon
    maxconn 4096
    log stdout local0

defaults
    mode http
    timeout connect 5s
    timeout client 30s
    timeout server 30s
    log global

frontend http_front
    bind *:80
    default_backend app_servers

backend app_servers
    balance roundrobin
    option httpchk GET /health
    server app1 10.0.0.1:8080 check
    server app2 10.0.0.2:8080 check
    server app3 10.0.0.3:8080 check backup
```

### Health Checks

```
backend app_servers
    option httpchk GET /health
    http-check expect status 200
    server app1 10.0.0.1:8080 check inter 2s fall 3 rise 2
    # check       = enable health checks
    # inter 2s    = check every 2 seconds
    # fall 3      = mark down after 3 failures
    # rise 2      = mark up after 2 successes
```

### ACLs (Access Control Lists)

Powerful conditional routing.

```
frontend http_front
    bind *:80
    
    # Define ACLs
    acl is_api      path_beg /api/
    acl is_admin    path_beg /admin/
    acl is_mobile   hdr_sub(User-Agent) Mobile
    
    # Route based on ACLs
    use_backend api_servers   if is_api
    use_backend admin_servers if is_admin
    use_backend mobile_servers if is_mobile
    default_backend web_servers
```

### Stats Page

```
listen stats
    bind *:9000
    mode http
    stats enable
    stats uri /stats
    stats refresh 5s
    stats auth admin:secretpass
```

Visit `http://lb:9000/stats` for real-time stats.

### TCP Load Balancing

```
frontend mysql_front
    bind *:3306
    mode tcp
    default_backend mysql_servers

backend mysql_servers
    mode tcp
    balance leastconn
    server db1 10.0.0.1:3306 check
    server db2 10.0.0.2:3306 check
```

### SSL Termination

```
frontend https_front
    bind *:443 ssl crt /etc/ssl/certs/example.pem
    default_backend app_servers
```

---

## Envoy Proxy

**Matlab:** Modern, cloud-native proxy by Lyft. Heart of service mesh (Istio, Consul Connect).

### Why Envoy?

- 🌐 **Native HTTP/2 + gRPC** support
- 🔧 **Dynamic config** via xDS API (hot reloads, no restarts)
- 📊 **Rich observability** (metrics, tracing, access logs)
- 🔐 **mTLS** for service-to-service
- 🎯 **Circuit breakers, retries, outlier detection**
- 🌍 **Service mesh** building block

### Sidecar Pattern (Service Mesh)

Each microservice has an Envoy sidecar.

```
Service A      Service B
   ↓              ↑
 Envoy ←──→  Envoy
 (sidecar)   (sidecar)
```

All inter-service traffic goes through Envoy → centralized observability + policy.

### Basic Config (YAML)

```yaml
static_resources:
  listeners:
  - name: listener_0
    address:
      socket_address: { address: 0.0.0.0, port_value: 80 }
    filter_chains:
    - filters:
      - name: envoy.filters.network.http_connection_manager
        typed_config:
          "@type": type.googleapis.com/envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager
          stat_prefix: ingress_http
          route_config:
            name: local_route
            virtual_hosts:
            - name: local_service
              domains: ["*"]
              routes:
              - match: { prefix: "/" }
                route: { cluster: backend_service }
          http_filters:
          - name: envoy.filters.http.router
  
  clusters:
  - name: backend_service
    connect_timeout: 5s
    type: STRICT_DNS
    lb_policy: ROUND_ROBIN
    load_assignment:
      cluster_name: backend_service
      endpoints:
      - lb_endpoints:
        - endpoint:
            address:
              socket_address: { address: backend1, port_value: 8080 }
        - endpoint:
            address:
              socket_address: { address: backend2, port_value: 8080 }
```

### Advanced Features

**Circuit Breaker:**
```yaml
clusters:
- name: backend
  circuit_breakers:
    thresholds:
    - max_connections: 100
      max_pending_requests: 100
      max_requests: 1000
      max_retries: 3
```

**Retry Policy:**
```yaml
routes:
- match: { prefix: "/api" }
  route:
    cluster: backend
    retry_policy:
      retry_on: 5xx,reset,connect-failure
      num_retries: 3
      per_try_timeout: 1s
```

**Outlier Detection** (auto-eject misbehaving hosts):
```yaml
clusters:
- name: backend
  outlier_detection:
    consecutive_5xx: 5
    interval: 10s
    base_ejection_time: 30s
```

### Comparison

| | Nginx | HAProxy | Envoy |
|--|-------|---------|-------|
| Origin | Russia/2004 | France/2001 | Lyft/2016 |
| L4 + L7 | ✅ | ✅ | ✅ |
| Dynamic config | Limited | Limited | Native (xDS) |
| HTTP/2 | ✅ | ✅ | Native |
| gRPC | Some | Some | Native |
| Observability | Basic | Stats page | Rich (Prometheus, OpenTelemetry) |
| Best for | Web servers, simple LB | TCP LB, traditional | Service mesh, microservices |

---

## X-Forwarded-For & Real IP

**Problem:** Behind a proxy, server sees proxy's IP, not the real client's.

```
Client (1.2.3.4) → Proxy (10.0.0.1) → App Server
                                       sees: 10.0.0.1 ❌
```

### `X-Forwarded-For` (XFF)

Standard header set by proxies.

```http
X-Forwarded-For: 1.2.3.4
```

If multiple proxies in chain:
```http
X-Forwarded-For: 1.2.3.4, 5.6.7.8, 10.0.0.1
                 ↑          ↑          ↑
              client    intermediate  closest proxy
```

### Other Headers

| Header | Purpose |
|--------|---------|
| `X-Forwarded-For` | Original client IP |
| `X-Forwarded-Proto` | Original scheme (http/https) |
| `X-Forwarded-Host` | Original host header |
| `X-Real-IP` | Single most-recent client IP (Nginx) |
| `Forwarded` | RFC 7239 standard combining all |

### Nginx Setting

```nginx
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
```

### App Side — Extract Real IP

```python
def get_client_ip(request):
    # Trust XFF only from known proxies
    xff = request.headers.get('X-Forwarded-For')
    if xff:
        # First IP is the original client
        return xff.split(',')[0].strip()
    return request.remote_addr
```

### ⚠️ Security: Don't Trust Blindly

XFF can be **forged** by malicious clients! 

```
Attacker sends:
  GET / HTTP/1.1
  X-Forwarded-For: 192.168.1.1
```

→ Server thinks request from internal IP. **Spoofing!**

**Solution:** Trust XFF only from known proxy IPs.

```nginx
set_real_ip_from 10.0.0.0/8;        # only trust these
set_real_ip_from 127.0.0.1;
real_ip_header X-Forwarded-For;
real_ip_recursive on;
```

### `Forwarded` (RFC 7239)

Modern alternative combining XFF + proto + host:

```http
Forwarded: for=1.2.3.4;proto=https;host=example.com
```

Less common in practice (XFF still dominant).

---

## Health Checks

### Active Health Check

LB periodically checks each backend.

```
Every 5 seconds:
  GET /health from LB to each backend
  Expect 200 OK
  Mark unhealthy after 3 failures
```

### Passive Health Check

LB tracks request outcomes; detects failures via real traffic.

```
If 3 consecutive 5xx → mark unhealthy
Re-check after timeout
```

### Health Endpoint Best Practices

```python
# Liveness — is the process alive?
@app.route('/health/live')
def liveness():
    return 'OK', 200

# Readiness — can it serve traffic?
@app.route('/health/ready')
def readiness():
    if not db.is_connected() or not cache.is_connected():
        return 'NOT READY', 503
    return 'OK', 200
```

- **Liveness** failed → restart container (Kubernetes)
- **Readiness** failed → remove from LB rotation

### What to Avoid

❌ Just returning 200 for everything (defeats purpose)
❌ Heavy queries in health check (kills DB)
❌ External API calls in liveness (cascading failures)

---

## Real-World Architecture

```
                    ┌────────────────┐
Internet ─────→     │   Cloudflare   │  (DDoS, WAF, edge cache)
                    └────────────────┘
                            ↓
                    ┌────────────────┐
                    │    AWS ALB     │  (L7, SSL termination)
                    └────────────────┘
                            ↓
            ┌──────────┬────┴────┬──────────┐
            ↓          ↓         ↓          ↓
        ┌──────┐   ┌──────┐  ┌──────┐  ┌──────┐
        │ App1 │   │ App2 │  │ App3 │  │ App4 │
        └──────┘   └──────┘  └──────┘  └──────┘
                            ↓
                    ┌────────────────┐
                    │  Envoy mesh    │  (mTLS, retries)
                    └────────────────┘
                            ↓
                    ┌────────────────┐
                    │ Internal svcs  │
                    └────────────────┘
```

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| **Forward proxy** | Client-side; corp filtering, VPN |
| **Reverse proxy** | Server-side; LB, SSL, hide backends |
| **L4 LB** | TCP/UDP, fast, no app awareness |
| **L7 LB** | HTTP-aware, slower, smart routing |
| **Round Robin** | Equal distribution, simple |
| **Least Conn** | Best for long-lived requests |
| **IP Hash** | Sticky sessions |
| **Consistent Hash** | Distributed caches |
| **Nginx** | Web server + reverse proxy + LB |
| **HAProxy** | TCP/HTTP LB, great health checks |
| **Envoy** | Service mesh, dynamic config |
| **X-Forwarded-For** | Original client IP (verify trust) |
| **Liveness probe** | Process alive? |
| **Readiness probe** | Can serve traffic? |

---

## Practice

1. Set up Nginx as reverse proxy in front of 2 Node.js / Spring Boot apps.
2. Configure SSL termination with Let's Encrypt cert in Nginx.
3. Set up rate limiting (10 req/s) and test with `ab` / `wrk`.
4. Configure HAProxy with health checks; bring down a backend, observe.
5. Deploy Envoy as sidecar to a service; enable mTLS between two services.
6. Implement proper liveness + readiness endpoints in your app.
7. Test X-Forwarded-For spoofing — write code that handles it safely.
