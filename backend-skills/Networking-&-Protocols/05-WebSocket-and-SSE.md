# WebSocket & Server-Sent Events (SSE)

## Status: Not Started

---

## Table of Contents

1. [Real-Time Communication Problem](#real-time-communication-problem)
2. [HTTP Polling](#http-polling)
3. [Long Polling](#long-polling)
4. [Server-Sent Events (SSE)](#server-sent-events-sse)
5. [WebSocket](#websocket)
6. [WebSocket Handshake](#websocket-handshake)
7. [Comparison Table](#comparison-table)
8. [Use Cases](#use-cases)
9. [Spring WebSocket Config](#spring-websocket-config)
10. [STOMP Protocol](#stomp-protocol)
11. [Scaling WebSockets](#scaling-websockets)

---

## Real-Time Communication Problem

HTTP **request-response** model — client request kare bina server data nahi bhej sakta. Real-time apps (chat, stock prices, notifications) ke liye solve karna hai:

> "Server kaise client ko data **push** kare?"

### Solutions Evolution

```
1. HTTP Polling             (clunky)
2. Long Polling             (better)
3. Server-Sent Events (SSE) (one-way)
4. WebSocket                (full duplex)
```

---

## HTTP Polling

**Matlab:** Client har few seconds par request bhejta hai — "kuch naya hai?"

```
Client → GET /messages?since=10  → Server
Client ← []                      ← Server   (nothing new)

(wait 5 sec)

Client → GET /messages?since=10  → Server
Client ← []                      ← Server

(wait 5 sec)

Client → GET /messages?since=10  → Server
Client ← [{id: 11, text: "hi"}]  ← Server   (got something!)
```

### Pros / Cons

| Pro | Con |
|-----|-----|
| Simple HTTP | Wasteful (mostly empty responses) |
| Works everywhere | High latency (5s lag minimum) |
| Stateless | High server load |

❌ Avoid in 2026 unless absolutely needed.

---

## Long Polling

**Matlab:** Client request bhejta hai — server **wait** karta hai jab tak data available na ho. Then respond. Client repeat.

```
Client → GET /messages?since=10  → Server
                                    Server: "no data yet, holding..."
                                    (wait 30 sec)
                                    (data arrives!)
Client ← [{id: 11, ...}]         ← Server
Client → GET /messages?since=11  → Server (immediate next request)
                                    (waits again)
```

### Pros / Cons

| Pro | Con |
|-----|-----|
| Lower latency than polling | Connection per client (resource heavy) |
| Easier than WebSocket | Timeouts can cause issues |
| Works through proxies/firewalls | Still HTTP overhead |

Used historically before WebSocket support was widespread (e.g., Facebook chat in 2010).

---

## Server-Sent Events (SSE)

**Matlab:** Server-to-client **one-way push** over HTTP. Built into browsers.

### Protocol

Client opens HTTP connection with `Accept: text/event-stream`. Server keeps connection open, streams data.

```
Client → GET /events HTTP/1.1
         Accept: text/event-stream
         Cache-Control: no-cache

Server ← HTTP/1.1 200 OK
         Content-Type: text/event-stream
         (connection stays open)

Server → data: hello\n\n
         data: another message\n\n
         id: 5\n
         event: notification\n
         data: {"text":"new alert"}\n\n
```

### JavaScript Client (built-in!)

```javascript
const events = new EventSource('/events');

events.onmessage = (e) => {
    console.log('Got:', e.data);
};

events.addEventListener('notification', (e) => {
    const data = JSON.parse(e.data);
    console.log('Notification:', data);
});

events.onerror = (e) => {
    console.error('Connection error');
};
```

### Server Side (Node.js Example)

```javascript
app.get('/events', (req, res) => {
    res.setHeader('Content-Type', 'text/event-stream');
    res.setHeader('Cache-Control', 'no-cache');
    res.setHeader('Connection', 'keep-alive');
    
    const interval = setInterval(() => {
        res.write(`data: ${JSON.stringify({time: Date.now()})}\n\n`);
    }, 1000);
    
    req.on('close', () => clearInterval(interval));
});
```

### Event Format

```
id: 12345           ← optional event ID (used for resumption)
event: update       ← optional event type
data: {"x": 1}      ← actual payload (can be multi-line)
retry: 5000         ← reconnect delay in ms

                    ← blank line ends event
```

### Auto-Reconnect

`EventSource` automatically reconnects on disconnect — sends `Last-Event-ID` header from last seen `id`.

```
GET /events HTTP/1.1
Accept: text/event-stream
Last-Event-ID: 12345
```

### Pros / Cons

| Pro | Con |
|-----|-----|
| Built into browsers | One-way only (server → client) |
| Auto-reconnect | Limited to text data |
| Works over HTTP/1.1, /2, /3 | Browser ~6 connection limit per origin (HTTP/1) |
| Simple to implement | Less common than WebSocket |
| Supports HTTP caching layers |  |

### Use Cases

✅ Live notifications (e.g., new emails, alerts)
✅ Stock tickers, sports scores
✅ Server logs streaming
✅ Progress updates
❌ Chat (need bi-directional → use WebSocket)

---

## WebSocket

**Matlab:** **Full-duplex** communication channel over a single TCP connection. Both client and server can send messages anytime.

### Why WebSocket?

```
HTTP:       Client request → Server response (one shot)
WebSocket:  Bi-directional, persistent, full-duplex

Once connected, both sides send messages without ceremony.
```

### URL Schemes

```
ws://example.com/chat        (unencrypted, like HTTP)
wss://example.com/chat       (encrypted, like HTTPS)
```

### JavaScript Client

```javascript
const ws = new WebSocket('wss://example.com/chat');

ws.onopen = () => {
    console.log('Connected');
    ws.send('Hello server!');
};

ws.onmessage = (event) => {
    console.log('Received:', event.data);
};

ws.onerror = (err) => console.error(err);
ws.onclose = (e) => console.log('Closed', e.code, e.reason);

// Send anytime
ws.send(JSON.stringify({ type: 'msg', text: 'Hi' }));
```

### Server (Node.js with `ws` library)

```javascript
const WebSocket = require('ws');
const wss = new WebSocket.Server({ port: 8080 });

wss.on('connection', (ws) => {
    console.log('New client');
    
    ws.on('message', (data) => {
        console.log('Got:', data.toString());
        ws.send('Echo: ' + data);
        
        // Broadcast to all clients
        wss.clients.forEach(client => {
            if (client.readyState === WebSocket.OPEN) {
                client.send(data);
            }
        });
    });
    
    ws.on('close', () => console.log('Client gone'));
});
```

---

## WebSocket Handshake

WebSocket connection HTTP **Upgrade** request se start hota hai. Phir protocol switch ho jaata hai.

### Handshake Diagram

```
Client                          Server
  │                                │
  │── HTTP GET /chat              │
  │   Upgrade: websocket          │
  │   Connection: Upgrade         │
  │   Sec-WebSocket-Key: dGhl...  │
  │   Sec-WebSocket-Version: 13   │
  │ ─────────────────────────────→│
  │                                │
  │← HTTP/1.1 101 Switching       │
  │   Upgrade: websocket          │
  │   Connection: Upgrade         │
  │   Sec-WebSocket-Accept: ...   │
  │ ──────────────────────────── │
  │                                │
  │ ===== WebSocket Frames =====  │  (now it's WebSocket protocol)
  │                                │
```

### Request

```http
GET /chat HTTP/1.1
Host: example.com
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
Sec-WebSocket-Version: 13
Sec-WebSocket-Protocol: chat, soap        ← optional subprotocols
Origin: https://example.com
```

### Response (Successful)

```http
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
Sec-WebSocket-Protocol: chat
```

### `Sec-WebSocket-Accept` Computation

Server takes client's `Sec-WebSocket-Key`, appends magic string `258EAFA5-E914-47DA-95CA-C5AB0DC85B11`, SHA-1 hashes, base64-encodes.

→ Prevents accidental WebSocket connections.

### Frame Format

After handshake, data sent as **frames**:

```
┌─────────────────────────────────────────────────┐
│ FIN (1) | RSV (3) | Opcode (4) | Mask (1) | ... │
├─────────────────────────────────────────────────┤
│   Payload length, masking key, payload data     │
└─────────────────────────────────────────────────┘
```

### Opcodes

| Opcode | Type |
|--------|------|
| 0x0 | Continuation |
| 0x1 | Text |
| 0x2 | Binary |
| 0x8 | Close |
| 0x9 | Ping |
| 0xA | Pong |

### Ping/Pong (Keep-Alive)

```
Server → Ping
Client → Pong   (auto-handled by browsers/libs)
```

Detects dead connections / keeps NAT entries alive.

### Close Codes

| Code | Meaning |
|------|---------|
| 1000 | Normal closure |
| 1001 | Going away |
| 1002 | Protocol error |
| 1003 | Unsupported data |
| 1006 | Abnormal closure (no close frame) |
| 1011 | Server error |
| 4000-4999 | Application-defined |

---

## Comparison Table

| Feature | HTTP Polling | Long Polling | SSE | WebSocket |
|---------|--------------|--------------|-----|-----------|
| Direction | Client → Server | Client → Server | Server → Client | Bi-directional |
| Connection | Many short | Long-held | Long-held | Persistent |
| Latency | High | Medium | Low | Lowest |
| Server load | High | Medium | Low | Low |
| Browser support | Universal | Universal | Modern | Modern |
| Auto-reconnect | N/A | App | Built-in | Manual |
| Binary data | Yes | Yes | No (text only) | Yes |
| Through proxies | Easy | Easy | Easy | Sometimes tricky |
| Caching layers | OK | OK | OK | Need WS-aware |

---

## Use Cases

### Use WebSocket when:
- Bi-directional needed (chat, collaborative editing)
- Sub-second latency required (gaming, live trading)
- Frequent small messages
- Binary data (audio, video, file transfer)

### Use SSE when:
- Server → client only (notifications, dashboards)
- Want auto-reconnect built in
- Want HTTP caching/routing infrastructure
- Simpler — text-based events

### Use HTTP / Long Poll when:
- Real-time not actually needed
- Simple notifications, updates every minute+
- Working in restricted environments

### Real Examples

| App | Tech |
|-----|------|
| Slack | WebSocket |
| Google Docs | WebSocket (operational transforms) |
| Twitch chat | WebSocket |
| Stripe Dashboard | SSE |
| GitHub notifications | SSE |
| Online games | WebSocket / WebRTC |
| Stock tickers | SSE / WebSocket |

---

## Spring WebSocket Config

### Maven Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### Basic WebSocket Handler

```java
@Component
public class ChatHandler extends TextWebSocketHandler {
    
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) 
            throws IOException {
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(message.getPayload()));
            }
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }
}
```

### Register Handler

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Autowired private ChatHandler chatHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatHandler, "/chat")
                .setAllowedOrigins("*");
    }
}
```

### Connect from JS

```javascript
const ws = new WebSocket('ws://localhost:8080/chat');
ws.onmessage = (e) => console.log(e.data);
ws.send('Hello!');
```

---

## STOMP Protocol

**Matlab:** **Simple Text Oriented Messaging Protocol** — message-broker-style protocol layered over WebSocket. Pub-sub, queues, headers — like JMS but text.

### Why STOMP?

Plain WebSocket = raw bytes/text. STOMP adds:
- ✅ Pub-sub semantics (`SUBSCRIBE`, `UNSUBSCRIBE`)
- ✅ Topics & queues (`/topic/news`, `/queue/orders`)
- ✅ Headers, ack/nack
- ✅ Easy mapping to message brokers (RabbitMQ, ActiveMQ)

### Frame Format (Text)

```
COMMAND
header1:value1
header2:value2

Body content here^@
```

(`^@` = null byte = end of frame)

### Common Commands

| Command | Purpose |
|---------|---------|
| `CONNECT` | Open STOMP session |
| `CONNECTED` | Server ack |
| `SEND` | Send to destination |
| `SUBSCRIBE` | Subscribe to destination |
| `UNSUBSCRIBE` | Stop subscribing |
| `MESSAGE` | Received message (server → client) |
| `ACK` / `NACK` | Acknowledge |
| `DISCONNECT` | Close session |

### Example Flow

```
Client → CONNECT
         accept-version:1.2
         host:example.com

Server ← CONNECTED
         version:1.2

Client → SUBSCRIBE
         id:sub-0
         destination:/topic/news

Client → SEND
         destination:/topic/news
         
         Hello everyone!

(server pushes to all subscribers)

Client ← MESSAGE
         destination:/topic/news
         message-id:abc123
         subscription:sub-0
         
         Hello everyone!
```

### Spring Boot + STOMP

```java
@Configuration
@EnableWebSocketMessageBroker
public class StompConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*");
    }
}

@Controller
public class ChatController {
    
    @MessageMapping("/chat.send")     // matches /app/chat.send
    @SendTo("/topic/messages")        // broadcasts to subscribers
    public ChatMessage send(ChatMessage msg) {
        return msg;
    }
}
```

### JS Client (StompJS)

```javascript
const stomp = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/ws',
    onConnect: () => {
        stomp.subscribe('/topic/messages', (msg) => {
            console.log(JSON.parse(msg.body));
        });
        stomp.publish({
            destination: '/app/chat.send',
            body: JSON.stringify({ user: 'Rahul', text: 'Hi' })
        });
    }
});
stomp.activate();
```

### When STOMP?

✅ Need pub-sub semantics
✅ Want to integrate with RabbitMQ / ActiveMQ
✅ Multiple message types / topics in one connection
❌ Plain raw protocol — STOMP overhead unnecessary

---

## Scaling WebSockets

### Challenges

- **Stateful connections** — sticky to a single server
- **Pub-sub across servers** — message on Server A → user on Server B?
- **Connection limit** per machine (10K-100K range)

### Patterns

#### 1. Sticky Sessions

LB ensures each user always hits same server.

```nginx
upstream ws_backend {
    ip_hash;          # consistent hashing on IP
    server ws1:8080;
    server ws2:8080;
}
```

#### 2. Redis Pub-Sub for Cross-Server Broadcasting

```
User1 ← Server A           Server B → User2
         │                          │
         └──── Redis pub/sub ──────┘
```

```javascript
// Server A receives message
redis.publish('chat', JSON.stringify(msg));

// All servers subscribed to 'chat':
redis.subscribe('chat');
redis.on('message', (channel, payload) => {
    // forward to local connections
    broadcast(JSON.parse(payload));
});
```

#### 3. Dedicated WebSocket Servers

Separate from REST API. Optimized for many concurrent long-lived connections.

#### 4. Use a Service

- **Pusher** / **Ably** / **PubNub** — managed
- **AWS API Gateway WebSocket**
- **Socket.IO** with Redis adapter

### Nginx WebSocket Config

```nginx
upstream ws_backend {
    server localhost:8080;
}

server {
    listen 80;
    
    location /ws {
        proxy_pass http://ws_backend;
        
        # Upgrade headers (CRITICAL)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # Long timeouts (default is 60s — drops your WebSocket!)
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;
        
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

⚠️ **Forgetting `proxy_read_timeout`** is the #1 reason WebSockets drop after 60 seconds in production.

---

## Authentication & Security

### Auth Strategies

#### 1. Token in URL Query (simple but logs leak it)
```
ws://example.com/chat?token=abc123
```

#### 2. Auth via First Message
```
ws.onopen = () => ws.send(JSON.stringify({type: 'auth', token: 'abc'}));
```

#### 3. Cookie-Based (works since browsers send cookies during handshake)
```
ws.onopen = () => {};  // cookie sent automatically
// server reads cookie in handshake
```

### Origin Check

In handshake, browsers send `Origin` header. Server should validate.

```java
.setAllowedOrigins("https://example.com")  // not '*' in production!
```

### Rate Limiting

WebSocket → easy to abuse with rapid messages. Implement per-connection rate limit.

---

## Summary Cheat Sheet

| Tech | Direction | Use For |
|------|-----------|---------|
| **HTTP Polling** | C ↔ S | Avoid in 2026 |
| **Long Polling** | C ↔ S | Legacy compat |
| **SSE** | S → C | Notifications, feeds |
| **WebSocket** | C ↔ S | Chat, gaming, collab |
| **STOMP** | over WS | Pub-sub semantics |

| Concept | Quick Note |
|---------|-----------|
| WebSocket handshake | HTTP Upgrade → 101 Switching |
| Sec-WebSocket-Key | Verify intentional handshake |
| Frame | Binary protocol after handshake |
| Ping/Pong | Keep-alive |
| Close codes | 1000=normal, 1006=abnormal |
| `wss://` | Encrypted (TLS) |
| Sticky sessions | LB pins user to server |
| Redis pub-sub | Cross-server broadcast |

---

## Practice

1. Build a simple chat app using raw WebSocket (Node.js + browser).
2. Implement SSE-based notification stream.
3. Compare bandwidth: polling every 1s vs WebSocket idle vs SSE.
4. Set up Spring Boot with STOMP + STOMP.js client.
5. Configure Nginx in front of WebSocket server with proper Upgrade headers.
6. Scale a WebSocket app across 2 servers using Redis pub-sub.
7. Implement reconnect with exponential backoff in JS client.
