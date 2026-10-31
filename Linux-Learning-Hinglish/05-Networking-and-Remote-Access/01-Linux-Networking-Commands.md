# Module 05: Linux Networking Commands & Firewall Security 🌐

Backend applications akele isolated island par nahi chalti. Microservices REST APIs ke through aapas me baat karti hain, databases se connect hoti hain aur internet se traffic receive karti hain. Networking commands sikhna backend developer ke liye absolute superpower hai.

---

## 1. Network Interfaces & IP Configuration

### 1. `ip addr` (Modern Alternative to `ifconfig`)
`ifconfig` ab Linux me deprecated ho chuka hai. Standard tool hai `ip`:

```bash
ip addr
# ya shortcut:
ip a
```
**Output breakdown:**
- `lo` (Loopback Interface, IP `127.0.0.1`): Localhost communications ke liye.
- `eth0` ya `ens33` ya `enp0s3`: Server ka physical/virtual network card jisme Public ya Private LAN IP assigned hota hai.
- `docker0` (IP `172.17.0.1`): Docker container bridge network.

### 2. Default Gateway & Routing Table
```bash
ip route
# default via 192.168.1.1 dev eth0 proto dhcp metric 100
# '192.168.1.1' aapka Router / NAT Gateway hai jahan se internet bahar jata hai.
```

---

## 2. Port Listening & Sockets (`ss` vs `netstat`) 🔌

Jab aap Spring Boot (`8080`), Nginx (`80/443`) ya MySQL (`3306`) start karte hain, toh process port par "Listen" karti hai.

### The Modern Champion: `ss` (Socket Statistics)
`netstat` pura `/proc/net` scan karta tha jo high-traffic servers par bohot slow tha. `ss` direct kernel netlink interface se data leta hai aur instant output deta hai.

```bash
ss -tulnp
```
**Flags breakdown:**
- `-t`: TCP sockets
- `-u`: UDP sockets
- `-l`: Sirf **Listening** ports dikhao (established client connections mat dikhao)
- `-n`: Numeric ports dikhao (e.g. `8080`, instead of service names)
- `-p`: Process name aur PID bhi print karo

Sample Output:
```
Netid  State   Recv-Q  Send-Q   Local Address:Port   Peer Address:Port  Process
tcp    LISTEN  0       128      0.0.0.0:22           0.0.0.0:*          users:(("sshd",pid=540,fd=3))
tcp    LISTEN  0       511      0.0.0.0:80           0.0.0.0:*          users:(("nginx",pid=820,fd=6))
tcp    LISTEN  0       100      127.0.0.1:3306       0.0.0.0:*          users:(("mysqld",pid=1102,fd=21))
tcp    LISTEN  0       100      *:8080               *:*                users:(("java",pid=2490,fd=14))
```

### 🧠 Understanding `0.0.0.0` vs `127.0.0.1` (Crucial Security Concept!)
- `127.0.0.1:3306` (Localhost only): MySQL sirf usi server ke andar se access ho sakta hai. Internet ya doosri machines se connect nahi ho sakta. **Very Secure!**
- `0.0.0.0:8080` (All Interfaces): App server ke sabhi network cards par bind hai. Localhost, Private LAN, aur Public Internet sabhi jagah se traffic accept karega.

---

## 3. The Classic "Port Already in Use" Error Fix 🛠️

Jab aap application start karte hain aur error aata hai:  
`Web server failed to start. Port 8080 was already in use.`

### Step-by-step Solution with `lsof`:
```bash
# 1. Port 8080 ko kis process ne capture kiya hai:
sudo lsof -i :8080

# Output:
# COMMAND   PID   USER   FD   TYPE DEVICE SIZE/OFF NODE NAME
# java     2490 sachin   14u  IPv6  34509      0t0  TCP *:http-alt (LISTEN)

# 2. Culprit PID mil gaya: 2490. Use terminate karein:
kill -15 2490

# 3. Or a one-liner to kill port holder automatically:
sudo kill -9 $(sudo lsof -t -i :8080)
```

---

## 4. Connectivity, DNS & API Testing Tools

### 1. `ping` (ICMP Ping)
Server physically zinda hai ya nahi aur latency kitni hai:
```bash
ping -c 4 8.8.8.8
# -c 4 flag: Sirf 4 packets bhej kar ruk jao (Mac/Linux me bina flag ke infinite ping karta rehta hai)
```

### 2. `curl` (The HTTP API Tester) 🚀
Backend APIs ko terminal se call karne ka gold standard tool:
```bash
# 1. Simple GET request:
curl https://api.github.com

# 2. Verbose mode (-v): HTTP Headers, TLS Handshake aur exact request/response dekhne ke liye:
curl -v https://api.myproject.com/health

# 3. Sirf Response Headers dekhna (-I flag):
curl -I https://google.com

# 4. POST Request with JSON Body and Headers:
curl -X POST https://api.myapp.com/v1/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer my-jwt-token" \
  -d '{"name": "Sachin", "role": "Backend Developer"}'

# 5. File download karna (-O flag):
curl -O https://download.oracle.com/java/17/latest/jdk-17_linux-x64_bin.tar.gz
```

### 3. DNS Troubleshooting: `nslookup` vs `dig`
Jab domain name resolve na ho raha ho:
```bash
# Quick lookup:
nslookup api.stripe.com

# Detailed DNS trace (dig = Domain Information Groper):
dig api.stripe.com +short
# Output: Direct IP addresses list!

# Specific DNS Server se query karna (e.g. Google 8.8.8.8):
dig @8.8.8.8 api.stripe.com
```

### 4. `nc` (Netcat - The Network Swiss Knife) 🗡️
Remote server ka port open hai ya firewall ne block kiya hai:
```bash
# Check if remote database port 5432 is open:
nc -zv 10.0.0.15 5432
# -z = Zero-I/O mode (sirf connect karke check karega, data send nahi karega)
# -v = Verbose
# Output: Connection to 10.0.0.15 5432 port [tcp/postgresql] succeeded!
```

---

## 5. Linux Firewalls: UFW & Iptables 🛡️

### UFW (Uncomplicated Firewall - Ubuntu Standard)
UFW `iptables` ke upar user-friendly wrapper hai.

```bash
# 1. Status check:
sudo ufw status verbose

# 2. 🚨 CRITICAL FIRST STEP: Default rules set karein aur SSH allow karein
# (Warna enable karte hi aapka SSH connection disconnect ho jayega aur lockout ho jayenge!)
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp                # SSH allow

# 3. HTTP aur HTTPS traffic allow karna:
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# 4. Specific Private IP ko Database access dena:
# (Backend server IP 10.0.0.50 ko Postgres port 5432 par access do):
sudo ufw allow from 10.0.0.50 to any port 5432 proto tcp

# 5. Firewall turn on / off karna:
sudo ufw enable
sudo ufw reload
sudo ufw disable
```
