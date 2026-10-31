# Module 07: Bash Scripting Fundamentals & Strict Mode 📜

Manual repetitive tasks (deployments, backups, log cleanups, health checks) ko automate karne ke liye Bash scripting sabse powerful tool hai. Har backend developer ko clean, error-tolerant bash scripts likhna aana chahiye.

---

## 1. The Anatomy of a Bash Script

Har script `.sh` extension se end hoti hai aur uski pehli line hamesha **Shebang (`#!`)** hoti hai:

```bash
#!/bin/bash
# Yeh kernel ko batata hai ki is file ko /bin/bash interpreter se run karna hai.

echo "Hello, Backend Developer!"
```

Script ko executable banakar run karna:
```bash
chmod +x script.sh
./script.sh
```

---

## 2. Variables & User Input 🔤

```bash
#!/bin/bash

# ⚠️ RULE: Equal sign '=' ke dono taraf KABHI space mat lagana!
# Correct:
APP_NAME="PaymentService"
PORT=8080

# Wrong (Syntax Error aayega):
# APP_NAME = "PaymentService"

# Variable access karna:
echo "Starting ${APP_NAME} on port ${PORT}..."

# Read-only Constants:
readonly MAX_RETRIES=3

# User se input lena:
read -p "Enter Target Environment (dev/stage/prod): " TARGET_ENV
echo "Deploying to: ${TARGET_ENV}"
```

---

## 3. Command Substitution: `$(command)` ⏱️

Kisi Linux command ka output variable me store karne ke liye `$()` use karte hain:

```bash
# Current date and time nikalna:
CURRENT_TIME=$(date +"%Y-%m-%d_%H-%M-%S")
HOSTNAME=$(hostname)
TOTAL_RAM=$(free -m | awk '/Mem:/ {print $2}')

echo "Backup generated on ${HOSTNAME} at ${CURRENT_TIME} (Total RAM: ${TOTAL_RAM}MB)"
```

---

## 4. Exit Codes & Bash Strict Mode (DevOps Golden Rule! 🛡️)

Har Linux command execute hone ke baad ek **Exit Status Code** return karti hai:
- `0` = **Success** (Sab badhiya raha)
- `1 - 255` = **Error** (Command fail ho gayi)
- `$?` variable me previous command ka exit code hota hai.

```bash
ls /existing-folder
echo $? # Output: 0

ls /non-existent-folder
echo $? # Output: 2 (Error)
```

### 🚨 The Production Danger: Silent Failures
Normal Bash me agar beech me koi command fail ho jaye, toh script bina ruke aage execute hoti rehti hai!  
Sochiye agar `cd /opt/app-build` fail ho gaya, aur agla step `rm -rf *` tha, toh current directory ka sab ud jayega!

### 🌟 Solution: Strict Mode (Script ke top par hamesha lagayein)
```bash
#!/bin/bash
set -euo pipefail
```
- **`-e` (Exit on error):** Jaise hi koi command fail (non-zero) hogi, script immediately wahi terminate ho jayegi!
- **`-u` (Unset variables):** Agar aapne koi variable use kiya jo define nahi tha (e.g. Typo in `$PATH_DIR`), script error throw karke ruk jayegi.
- **`-o pipefail`:** Pipes me agar `command1 | command2` me se `command1` fail hua, toh poora pipeline fail mark hoga.

---

## 5. Script Arguments & Special Variables 🎯

Jab aap script run karte hain: `./deploy.sh api v2.1 prod`

```bash
#!/bin/bash

echo "Script Name: $0"            # ./deploy.sh
echo "First Argument: $1"        # api
echo "Second Argument: $2"       # v2.1
echo "Third Argument: $3"        # prod
echo "Total Arguments Count: $#" # 3
echo "All Arguments List: $@"    # api v2.1 prod
echo "Current Process PID: $$"   # e.g., 34812
```

Arguments validate karne ka standard pattern:
```bash
if [ "$#" -lt 2 ]; then
    echo "Usage: $0 <app_name> <environment>"
    exit 1
fi
```

---

## 6. Conditionals: `if - else` & Tests ⚖️

Bash me modern test operators ke liye double brackets **`[[ ... ]]`** use hote hain:

### String & Numeric Tests:
```bash
ENV="prod"

# String comparison (==, !=, -z for empty, -n for not empty)
if [[ "$ENV" == "prod" ]]; then
    echo "🚨 Production Mode: Enabling strict monitoring!"
elif [[ "$ENV" == "stage" ]]; then
    echo "🟡 Staging Mode"
else
    echo "🟢 Development Mode"
fi

# Numeric comparison:
# -eq (==), -ne (!=), -lt (<), -le (<=), -gt (>), -ge (>=)
HTTP_STATUS=500
if [[ "$HTTP_STATUS" -ge 500 ]]; then
    echo "Server Error detected!"
fi
```

### File & Directory Tests (DevOps Me Bohot Use Hoti Hain):
```bash
FILE="/opt/app/application.properties"
DIR="/var/log/myapp"

if [[ -f "$FILE" ]]; then
    echo "Config file exists!"
fi

if [[ ! -d "$DIR" ]]; then
    echo "Log directory missing! Creating now..."
    mkdir -p "$DIR"
fi

if [[ -s "$FILE" ]]; then
    echo "File is NOT empty."
fi
```

---

## 7. Loops: `for` & `while` 🔁

### 1. For Loop with List or Array
```bash
SERVICES=("nginx" "postgresql" "redis-server" "backend-api")

for service in "${SERVICES[@]}"; do
    echo "Checking status of ${service}..."
    systemctl is-active --quiet "${service}" && echo "✅ OK" || echo "❌ DOWN"
done
```

### 2. Reading a File Line-by-Line (Log Parsing / Bulk Actions)
```bash
# Correct way to read line by line without stripping spaces or backslashes:
while IFS= read -r line; do
    echo "Processing IP: ${line}"
done < "banned_ips.txt"
```

---

## 8. Functions in Bash 🧩

Functions code ko reusable aur modular banate hain:

```bash
#!/bin/bash
set -euo pipefail

# Log helper function with timestamp
log_info() {
    local message="$1"
    echo "[$(date +'%Y-%m-%dT%H:%M:%S')] [INFO] ${message}"
}

log_error() {
    local message="$1"
    echo "[$(date +'%Y-%m-%dT%H:%M:%S')] [ERROR] ${message}" >&2
}

# Function with return code
check_disk_space() {
    local threshold=90
    local usage
    usage=$(df / | awk 'NR==2 {print $5}' | tr -d '%')
    
    if [[ "$usage" -ge "$threshold" ]]; then
        log_error "Disk usage is critical: ${usage}%!"
        return 1
    else
        log_info "Disk usage healthy: ${usage}%"
        return 0
    fi
}

# Invoke function
check_disk_space
```
