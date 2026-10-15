# Level 4: DevOps, Cloud & Kubernetes (Q262 - Q282)

---

### Q262 & Q263: Multi-Stage Dockerfile for Spring Boot
**Answer (Hinglish):**

Multi-stage build se final image ka size 800MB se ghat kar sirf ~150MB reh jata hai aur build tools (Maven) production image me leak nahi hote:

```dockerfile
# Stage 1: Build the Application
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal Production Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Run as non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

---

### Q265 & Q266: Zero-Downtime Deployment (Blue-Green vs Rolling)
**Answer (Hinglish):**

- **Rolling Deployment:**
  - Kubernetes default strategy.
  - Purane version ke pods ko ek-ek karke terminate karta hai jab naya pod healthy ban kar traffic lene ke liye ready ho jata hai.
- **Blue-Green Deployment:**
  - Blue (Current production version v1) aur Green (Naya version v2) dono environments simultaneously live hote hain.
  - Test pass hone par Router / Load Balancer traffic ko 100% Blue se switch karke Green par point kar deta hai. Rollback instant hota hai.
- **Canary Deployment:** Naye version par pehle sirf 5% traffic bhej kar error rate monitor karte hain, safe hone par dheere-dheere 100% roll out karte hain.

---

### Q273, Q274 & Q279: Kubernetes Core Architecture & Deployment
**Answer (Hinglish):**

- **Pod:** K8s me execution ki sabse smallest deployable unit (1 ya multiple containers).
- **Deployment:** Declarative spec jo replicas, rolling update strategy, aur pod lifecycle manage karta hai.
- **Service:** Pods ke dynamic IPs ke upar stable internal networking aur load balancing abstraction (`ClusterIP`, `NodePort`, `LoadBalancer`).
- **Ingress Controller (NGINX/Traefik):** External world (HTTP/HTTPS) se requests ko internal Services tak domain/path based route karta hai (`api.example.com/users`).

---

### Q280: Kubernetes Health Checks: Liveness vs Readiness Probes
**Answer (Hinglish):**

Spring Boot Actuator K8s ke liye out-of-the-box probe support deta hai:

| Probe Type | Actuator Endpoint | K8s Action on Failure |
| :--- | :--- | :--- |
| **Liveness Probe** | `/actuator/health/liveness` | Container dead ya deadlock ho gaya hai. K8s container ko **turant restart (kill)** karta hai. |
| **Readiness Probe** | `/actuator/health/readiness` | Pod abhi warm-up ho raha hai ya DB load kar raha hai. K8s pod ko restart nahi karta, bas **Service load balancer se temporarily traffic bhejna band** kar deta hai. |

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 20
  periodSeconds: 10
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 15
  periodSeconds: 5
```

---

### Q275 & Q277: Sidecar Pattern & Service Mesh (Istio)
**Answer (Hinglish):**

- **Sidecar Pattern:** Same Pod ke andar main application container ke saath ek secondary helper container run karna (e.g. log shipper, proxy, config sync).
- **Service Mesh (Istio):**
  - Application code me bina code changes kiye har pod ke bagal me **Envoy Proxy Sidecar** inject kar deta hai.
  - *Features provided automatically:* Mutual TLS (mTLS) inter-service encryption, circuit breaking, distributed tracing propagation, fine-grained canary traffic routing.
