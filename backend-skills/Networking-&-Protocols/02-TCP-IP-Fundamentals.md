# TCP / IP Fundamentals

## Status: Not Started

---

## Table of Contents

1. [TCP/IP Stack](#tcpip-stack)
2. [TCP vs UDP](#tcp-vs-udp)
3. [TCP 3-Way Handshake](#tcp-3-way-handshake)
4. [TCP 4-Way Termination](#tcp-4-way-termination)
5. [TCP Flow Control](#tcp-flow-control)
6. [TCP Congestion Control](#tcp-congestion-control)
7. [Socket Programming Basics](#socket-programming-basics)
8. [TIME_WAIT State](#time_wait-state)
9. [Common Issues & Tuning](#common-issues--tuning)

---

## TCP/IP Stack

**Matlab:** Internet ka layered model — har layer ek specific job karta hai.

```
┌────────────────────────┐
│ Application Layer      │  HTTP, FTP, SMTP, DNS
├────────────────────────┤
│ Transport Layer        │  TCP, UDP                  ← yahan focus
├────────────────────────┤
│ Internet Layer         │  IP (IPv4, IPv6), ICMP
├────────────────────────┤
│ Link Layer             │  Ethernet, WiFi
└────────────────────────┘
```

### Encapsulation

```
HTTP request "GET /"
   ↓ wrap with TCP header (port, seq#)
TCP segment
   ↓ wrap with IP header (src/dst IP)
IP packet
   ↓ wrap with Ethernet header (MAC)
Frame on wire
```

---

## TCP vs UDP

### TCP — Transmission Control Protocol

**Matlab:** Reliable, ordered, connection-oriented.

```
TCP guarantees:
  ✅ Delivery (lost packets retransmitted)
  ✅ Order (segments delivered in sequence)
  ✅ No duplicates
  ✅ Flow + congestion control

Cost: more overhead, higher latency
```

### UDP — User Datagram Protocol

**Matlab:** Fast, simple, no guarantees. "Send and forget".

```
UDP gives:
  ✅ Speed (no handshake)
  ✅ Low overhead (8-byte header)
  ❌ No reliability
  ❌ No ordering
  ❌ Duplicates possible
```

### Comparison

| Feature | TCP | UDP |
|---------|-----|-----|
| Connection | Connection-oriented | Connectionless |
| Reliable | ✅ | ❌ |
| Ordered | ✅ | ❌ |
| Duplicates prevented | ✅ | ❌ |
| Flow control | ✅ | ❌ |
| Congestion control | ✅ | ❌ |
| Header size | 20+ bytes | 8 bytes |
| Speed | Slower | Faster |
| Use cases | HTTP, SSH, email | DNS, video stream, gaming, VoIP |

### When to Use What?

**TCP:**
- Web (HTTP/1, HTTP/2)
- File transfer (FTP, SCP)
- Email (SMTP, IMAP)
- Database connections
- Anything where data loss = corruption

**UDP:**
- DNS lookups (small, fast)
- Video / audio streaming (drop a frame > delay)
- Online gaming (latency > completeness)
- VoIP
- DNS-over-UDP, NTP, DHCP
- HTTP/3 (uses QUIC over UDP)

---

## TCP 3-Way Handshake

**Matlab:** Connection establish karne ke liye 3 messages exchange — `SYN → SYN-ACK → ACK`.

### Diagram

```
Client                            Server
  │                                  │
  │── SYN, seq=x ──────────────────→ │   "Connect karna hai"
  │                                  │
  │←── SYN, seq=y, ACK=x+1 ──────── │   "OK, ready"
  │                                  │
  │── ACK, seq=x+1, ACK=y+1 ──────→ │   "Confirmed"
  │                                  │
  │       Connection established     │
  │                                  │
```

### Step-by-Step

**Step 1: Client → Server (SYN)**
```
TCP flags: SYN
Seq#: x (random initial sequence)
"I want to connect, my starting seq is x"
```

**Step 2: Server → Client (SYN + ACK)**
```
TCP flags: SYN, ACK
Seq#: y (server's initial seq)
ACK#: x+1
"OK, I accept. My starting seq is y. I expect your next byte to be x+1"
```

**Step 3: Client → Server (ACK)**
```
TCP flags: ACK
Seq#: x+1
ACK#: y+1
"Got it. I'll start sending from x+1. Send your data starting y+1"
```

### Why 3 Steps?

- 1 message: client doesn't know if server received
- 2 messages: server doesn't know if client received
- 3 messages: **both sides confirmed** seq numbers + reachability

### TCP State Transitions

```
Client:  CLOSED → SYN_SENT → ESTABLISHED
Server:  CLOSED → LISTEN → SYN_RECEIVED → ESTABLISHED
```

### Cost of Handshake

```
Each TCP connection: 1 RTT just for handshake
+ TLS handshake on top (if HTTPS): another 1-2 RTT

That's why HTTP keep-alive matters.
```

### SYN Flood Attack

Attacker bahut saare SYNs bhejta hai bina ACK ke → server ka backlog full → DoS.

**Defense:** SYN cookies, rate limiting.

---

## TCP 4-Way Termination

**Matlab:** Connection close karne ke liye 4 messages — `FIN → ACK → FIN → ACK`.

### Diagram

```
Client                            Server
  │                                  │
  │── FIN ────────────────────────→ │   "I'm done sending"
  │                                  │
  │←── ACK ───────────────────────── │   "OK, got your FIN"
  │                                  │
  │  ... server can still send data │
  │                                  │
  │←── FIN ───────────────────────── │   "I'm done too"
  │                                  │
  │── ACK ────────────────────────→ │   "Acknowledged"
  │                                  │
  │     [client enters TIME_WAIT]    │
  │     [server: CLOSED]             │
```

### Why 4 Steps (vs 3)?

TCP is full-duplex — each side closes independently.

```
Step 1+2: Client closes its sending side (server still sending)
Step 3+4: Server closes its sending side
```

(Sometimes ACK + FIN combined → 3 packets in practice.)

### State Transitions

```
Active close (initiator):
  ESTABLISHED → FIN_WAIT_1 → FIN_WAIT_2 → TIME_WAIT → CLOSED

Passive close (other side):
  ESTABLISHED → CLOSE_WAIT → LAST_ACK → CLOSED
```

---

## TCP Flow Control

**Matlab:** Sender ko slow karna agar receiver overwhelmed ho raha hai. Receiver-driven.

### How? — Sliding Window

Receiver advertises **window size** (free buffer space) in every ACK.

```
Receiver: "I have 8KB free, send up to 8KB"
Sender:   sends 8KB

Receiver: "I have 4KB free now (4KB still being processed)"
Sender:   sends only 4KB

Receiver: "Window 0 — STOP sending"
Sender:   pauses
```

### Window Header Field

```
TCP header has:
  Window Size: 16 bits → max 64KB (without scaling)
  Window Scale option: shifts left → up to 1 GB
```

### Zero Window

```
Receiver: window=0
Sender: stops, periodically sends "window probes" to check
Receiver: window=8192 (when ready)
Sender: resumes
```

---

## TCP Congestion Control

**Matlab:** Network congestion (router queue full, packets dropped) detect karke sender ko slow karna.

### Why?

If everyone sends at max speed → router buffers fill → packets dropped → retransmits → more congestion → collapse.

### Slow Start

Connection start → small congestion window (`cwnd`) → exponentially grow.

```
RTT 1:  cwnd = 1 (send 1 segment)
        ACK received → cwnd = 2
RTT 2:  send 2
        ACKs received → cwnd = 4
RTT 3:  send 4
        ACKs received → cwnd = 8
...exponentially until threshold (ssthresh)
```

### Congestion Avoidance

After `ssthresh`, grow **linearly** (additive increase).

```
cwnd = 16, 17, 18, 19, ... (one per RTT)
```

### Packet Loss → Backoff

When loss detected:
- **Timeout (severe)**: `cwnd = 1`, restart slow start
- **Triple duplicate ACK (mild)**: `cwnd = cwnd / 2` (multiplicative decrease) → fast retransmit

### Algorithms

| Algorithm | Notes |
|-----------|-------|
| **Reno** | Classic — loss-based |
| **CUBIC** (Linux default) | Better high-bandwidth scaling |
| **BBR** (Google, 2016) | Bandwidth-based, not loss-based; great for video, modern WANs |

### AIMD

**Additive Increase, Multiplicative Decrease** — basis of TCP's stability.

```
No loss: cwnd += 1 per RTT
Loss:    cwnd /= 2
```

---

## Socket Programming Basics

**Socket:** Network communication ka endpoint — IP + port.

### Server (TCP) — Pseudocode

```python
import socket

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)  # TCP
server.bind(('0.0.0.0', 8080))
server.listen(5)  # backlog = 5 pending connections

while True:
    conn, addr = server.accept()  # blocks until client
    data = conn.recv(1024)
    conn.sendall(b"HTTP/1.1 200 OK\r\n\r\nHello")
    conn.close()
```

### Client (TCP)

```python
client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client.connect(('example.com', 80))
client.sendall(b"GET / HTTP/1.1\r\nHost: example.com\r\n\r\n")
data = client.recv(4096)
client.close()
```

### UDP

```python
# Server
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)  # UDP
sock.bind(('0.0.0.0', 5353))
data, addr = sock.recvfrom(1024)
sock.sendto(b"response", addr)

# Client (no connect)
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.sendto(b"hello", ('server.com', 5353))
data, addr = sock.recvfrom(1024)
```

### Socket Types

| Type | Constant | Protocol |
|------|----------|----------|
| Stream | `SOCK_STREAM` | TCP |
| Datagram | `SOCK_DGRAM` | UDP |
| Raw | `SOCK_RAW` | Custom (privileged) |

### Socket Options

```python
sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
# Allow rebinding to port quickly (avoid TIME_WAIT block)

sock.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
# Detect dead connections

sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
# Disable Nagle's algorithm — small writes sent immediately
```

---

## TIME_WAIT State

**Matlab:** Connection close hone ke baad client (initiator of close) thoda time tak `TIME_WAIT` mein rehta hai — typically **2 × MSL (Maximum Segment Lifetime)** = 60-120s.

### Why TIME_WAIT?

1. **Late packets** se confusion na ho — agar same 4-tuple (src IP, src port, dst IP, dst port) reuse hua, purane packets new connection mein ghus jaate
2. **Last ACK lost** → server retransmits FIN → client should still respond

### Diagram

```
Client                          Server
  │── FIN ──────────────────→  │
  │←── ACK ────────────────── │
  │←── FIN ────────────────── │
  │── ACK ──────────────────→ │
  │                            │
  │  [TIME_WAIT for 2*MSL]     │
  │                            │
  │  [CLOSED]                  │
```

### Problem at Scale

High-traffic server with many short connections → thousands of TIME_WAIT entries → port exhaustion.

### Detect

```bash
# Linux
ss -tan | grep TIME-WAIT | wc -l
netstat -an | grep TIME_WAIT | wc -l
```

### Solutions

#### 1. Use Keep-Alive

Long-lived connections instead of many short ones.

#### 2. `SO_REUSEADDR`

Allow rebinding the local port even if TIME_WAIT exists.

#### 3. Tune `tcp_tw_reuse` (Linux)

```bash
sysctl -w net.ipv4.tcp_tw_reuse=1
```

Allows reusing TIME_WAIT sockets for new outgoing connections (safe).

#### 4. Reduce `tcp_fin_timeout`

```bash
sysctl -w net.ipv4.tcp_fin_timeout=15
```

⚠️ Don't reduce too much — RFC defaults exist for good reason.

#### 5. Increase Port Range

```bash
sysctl -w net.ipv4.ip_local_port_range="10000 65535"
```

---

## Common Issues & Tuning

### Issue 1: Slow First Byte

**Cause:** TCP + TLS handshakes.

**Fix:**
- Keep-alive
- HTTP/2
- TLS 1.3 (1-RTT) or HTTP/3 (0/1-RTT)
- Edge / CDN

### Issue 2: Connection Refused

```
ECONNREFUSED
```

- Server not listening on that port
- Firewall blocking
- Backlog full — `accept()` not called fast enough

### Issue 3: Connection Reset

```
ECONNRESET
```

- Server crashed mid-connection
- Application closed without proper shutdown
- Network device sent RST

### Issue 4: Broken Pipe

```
EPIPE (write to closed socket)
```

- Other end closed → write attempt → SIGPIPE / error
- Use `MSG_NOSIGNAL` flag in Linux

### Issue 5: Nagle's Algorithm + Delayed ACK

Combination causes 200ms delays for small writes.

**Fix:**
```python
sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
```

### Linux TCP Tuning Cheat Sheet

```bash
# Increase backlog
sysctl -w net.core.somaxconn=4096
sysctl -w net.ipv4.tcp_max_syn_backlog=4096

# More ephemeral ports
sysctl -w net.ipv4.ip_local_port_range="10000 65535"

# TIME_WAIT recycling
sysctl -w net.ipv4.tcp_tw_reuse=1
sysctl -w net.ipv4.tcp_fin_timeout=15

# Larger buffers (for high BDP networks)
sysctl -w net.core.rmem_max=16777216
sysctl -w net.core.wmem_max=16777216

# BBR congestion control (if kernel supports)
sysctl -w net.ipv4.tcp_congestion_control=bbr
```

---

## Useful Tools

| Tool | Use |
|------|-----|
| `ss -tan` | Connection states (modern netstat) |
| `netstat -an` | Connections + ports |
| `tcpdump -i any port 80` | Capture packets |
| `wireshark` | GUI packet analysis |
| `curl -v` | HTTP debugging |
| `telnet host port` | Manual TCP probe |
| `nc -l 8080` | Listen as netcat |
| `mtr host` | Traceroute + ping |
| `iperf3` | Bandwidth testing |
| `ping host` | ICMP reachability |

### Wireshark Filter Examples

```
tcp.port == 80
http.request
tcp.flags.syn == 1
ip.addr == 192.168.1.1
```

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| **TCP** | Reliable, ordered, slow |
| **UDP** | Fast, unreliable, no ordering |
| **3-way handshake** | SYN → SYN-ACK → ACK |
| **4-way termination** | FIN → ACK → FIN → ACK |
| **Flow control** | Sliding window (receiver-driven) |
| **Congestion control** | Slow start + AIMD |
| **CUBIC** | Linux default congestion algo |
| **BBR** | Modern, bandwidth-based |
| **Socket** | IP + port endpoint |
| **TIME_WAIT** | 2×MSL after close (~60-120s) |
| **Keep-Alive** | Avoid handshake overhead |
| **TCP_NODELAY** | Disable Nagle (low-latency) |
| **SYN flood** | DoS via half-open conns |

---

## Practice

1. Use Wireshark to capture and identify a 3-way handshake.
2. Write a simple TCP echo server in Python; test with `nc`.
3. Compare `time curl http://...` first call vs second (keep-alive benefit).
4. Cause a SYN flood with `hping3`; observe with `ss`.
5. Tune Linux TCP params on a busy server; measure connection rate.
6. Compare TCP vs UDP latency for small packets in a script.
