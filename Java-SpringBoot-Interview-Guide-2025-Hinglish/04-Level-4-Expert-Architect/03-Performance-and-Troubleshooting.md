# Level 4: Performance & Troubleshooting (Q283 - Q292)

---

### Q283 & Q289: Java Application Profiling & APM Tools
**Answer (Hinglish):**

- **Profiling:** Application ke runtime behavior (CPU usage, object allocation rates, lock contention, latency bottlenecks) ko measure karna.
- **Profiling Tools:**
  1. **Async-Profiler / JFR (Java Flight Recorder):** Low overhead (<1% CPU cost) profiler jo direct production me flame graphs generate karne ke liye best hai.
  2. **JProfiler / YourKit:** Local deep-dive analysis ke liye GUI tools.
  3. **Production APM (Application Performance Monitoring):** Datadog, Dynatrace, New Relic, Prometheus + Grafana.

---

### Q286: 504 Gateway Timeout vs 503 Service Unavailable Troubleshooting
**Answer (Hinglish):**

- **504 Gateway Timeout:**
  - **Meaning:** NGINX / Cloud Gateway ne upstream microservice ko request forward ki, lekin upstream service ne configured timeout (e.g. 30 seconds) ke andar response nahi diya.
  - **Causes:** Heavy unindexed DB query freeze, external payment API slow response, downstream thread pool exhaustion.
  - **Action:** Check upstream service logs and DB slow query logs.
- **503 Service Unavailable:**
  - **Meaning:** Upstream service completely down hai, crash ho gayi hai, ya over-capacity hone ki wajah se new connections reject kar rahi hai.
  - **Causes:** Pod crashed (OOMKilled), connection pool limit reach, autoscaling delay under sudden traffic spike.

---

### Q287: Database Query Optimization Strategies
**Answer (Hinglish):**

1. **`EXPLAIN ANALYZE` Run Karein:** Query execution plan inspect karein. Check karein ki *Index Scan* ho raha hai ya expensive *Seq Scan / Table Scan*.
2. **Proper Indexing:**
   - WHERE clause, JOIN columns, aur ORDER BY columns par **Composite B-Tree Indexes** banayein.
   - High selectivity columns ko index me pehle rakhein.
3. **Avoid `SELECT *`:** Sirf zaroori columns fetch karein (DTO Projections use karein).
4. **Solve N+1 JPA Queries:** `JOIN FETCH` ya `@EntityGraph` enforce karein.
5. **Read Replicas:** Heavy reporting / read traffic ko MySQL/Postgres Read Replica par divert karein.

---

### Q288: High CPU Usage in Production (Step-by-Step Incident Response)
**Answer (Hinglish):**

Jab production me CPU 100% chala jaye, to 4 critical steps follow karein:

1. **Find Top Thread ID (OS level):**
   ```bash
   top -H -p <PID>
   ```
   Top CPU consuming thread ka PID (Linux LWP thread ID) note karein (e.g. `14250`).
2. **Convert Thread ID to Hexadecimal:**
   ```bash
   printf "%x\n" 14250
   # Result: 37aa
   ```
3. **Capture Thread Dump:**
   ```bash
   jcmd <PID> Thread.print > threaddump.txt
   ```
4. **Search Hex ID in Thread Dump:**
   Grep `nid=0x37aa` in `threaddump.txt`. Aapko exactly method name aur line number mil jayega jahan code infinite loop me phas gaya hai ya heavy JSON parsing / regex backtracking kar raha hai!

---

### Q292: Remote Debugging in Java
**Answer (Hinglish):**

Agar staging ya dev server par code debug karna ho:
1. **JVM start command me JDWP agent enable karein:**
   ```bash
   java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar app.jar
   ```
2. **IntelliJ IDEA Setup:**
   - Go to `Run` -> `Edit Configurations` -> `+` -> `Remote JVM Debug`.
   - Host: Remote server IP, Port: `5005`.
   - Breakpoint lagayein aur real server traffic ko live inspect karein!
