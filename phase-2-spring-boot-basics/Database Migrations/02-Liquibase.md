# Liquibase

## Status: Not Started

---

## Table of Contents

1. [Liquibase Overview](#liquibase-overview)
2. [Liquibase Dependency](#liquibase-dependency)
3. [Changelog Formats](#changelog-formats)
4. [changeSet (id + author)](#changeset--id--author)
5. [Preconditions](#preconditions)
6. [Rollback](#rollback)
7. [runOnChange](#runonchange)
8. [Contexts](#contexts)
9. [Labels](#labels)
10. [liquibase.changeLogFile Property](#liquibasechangelogfile-property)

---

## Liquibase Overview

**Matlab:** Database schema change management tool — XML/YAML/JSON/SQL format mein migrations likhte hain. Flyway ka alternative hai.

### Flyway vs Liquibase

| Feature | Flyway | Liquibase |
|---------|--------|-----------|
| **Format** | SQL (mostly) | XML, YAML, JSON, SQL |
| **Database Independent** | ❌ SQL database-specific | ✅ Abstract change types |
| **Rollback Support** | Paid (Teams) | ✅ Open source mein |
| **Preconditions** | Limited | ✅ Rich |
| **Contexts** | Limited | ✅ Environment-specific |
| **Learning Curve** | Easy | Medium |

### How Liquibase Works

```
Application Start
    ↓
Liquibase auto-runs (Spring Boot integration)
    ↓
DATABASECHANGELOG table check karta hai
    ↓
Changelog file read karta hai
    ↓
Pending changeSets identify karta hai
    ↓
Har changeSet run karta hai (order mein)
    ↓
DATABASECHANGELOG update karta hai
    ↓
Application starts
```

---

## Liquibase Dependency

### Maven

```xml
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>

<!-- Database driver -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Gradle

```groovy
implementation 'org.liquibase:liquibase-core'
runtimeOnly 'com.mysql:mysql-connector-j'
```

### Liquibase Maven Plugin

```xml
<plugin>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-maven-plugin</artifactId>
    <configuration>
        <changeLogFile>src/main/resources/db/changelog/db.changelog-master.xml</changeLogFile>
        <url>jdbc:mysql://localhost:3306/mydb</url>
        <username>root</username>
        <password>pass</password>
    </configuration>
</plugin>
```

---

## Changelog Formats

Liquibase multiple formats support karta hai — XML, YAML, JSON, SQL.

### XML Format (Most Common)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="1" author="sachin">
        <createTable tableName="users">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="name" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="email" type="VARCHAR(100)">
                <constraints unique="true" nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP"/>
        </createTable>
    </changeSet>

    <changeSet id="2" author="sachin">
        <addColumn tableName="users">
            <column name="phone" type="VARCHAR(20)"/>
        </addColumn>
    </changeSet>
</databaseChangeLog>
```

### YAML Format

```yaml
databaseChangeLog:
  - changeSet:
      id: 1
      author: sachin
      changes:
        - createTable:
            tableName: users
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: name
                  type: VARCHAR(100)
                  constraints:
                    nullable: false
              - column:
                  name: email
                  type: VARCHAR(100)
                  constraints:
                    unique: true
                    nullable: false
              - column:
                  name: created_at
                  type: TIMESTAMP
                  defaultValueComputed: CURRENT_TIMESTAMP

  - changeSet:
      id: 2
      author: sachin
      changes:
        - addColumn:
            tableName: users
            columns:
              - column:
                  name: phone
                  type: VARCHAR(20)
```

### JSON Format

```json
{
  "databaseChangeLog": [
    {
      "changeSet": {
        "id": "1",
        "author": "sachin",
        "changes": [
          {
            "createTable": {
              "tableName": "users",
              "columns": [
                {
                  "column": {
                    "name": "id",
                    "type": "BIGINT",
                    "autoIncrement": true,
                    "constraints": {
                      "primaryKey": true,
                      "nullable": false
                    }
                  }
                },
                {
                  "column": {
                    "name": "name",
                    "type": "VARCHAR(100)",
                    "constraints": {
                      "nullable": false
                    }
                  }
                }
              ]
            }
          }
        ]
      }
    }
  ]
}
```

### SQL Format

```sql
--liquibase formatted sql

--changeset sachin:1
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--changeset sachin:2
ALTER TABLE users ADD COLUMN phone VARCHAR(20);

--changeset sachin:3
CREATE INDEX idx_users_email ON users(email);
```

### Format Comparison

| Format | Readability | Database Independent | Recommended? |
|--------|-------------|---------------------|--------------|
| **XML** | Medium | ✅ Yes | ✅ Most common |
| **YAML** | High | ✅ Yes | ✅ Clean syntax |
| **JSON** | Medium | ✅ Yes | ⚠️ Verbose |
| **SQL** | High | ❌ No | ✅ For DB-specific changes |

---

## changeSet (id + author)

**Matlab:** Har database change ek `changeSet` mein define hota hai — unique `id` + `author` combination se identify hota hai.

### Basic changeSet

```xml
<changeSet id="1" author="sachin">
    <createTable tableName="users">
        <column name="id" type="BIGINT" autoIncrement="true">
            <constraints primaryKey="true"/>
        </column>
        <column name="name" type="VARCHAR(100)"/>
    </createTable>
</changeSet>
```

### id + author Uniqueness

```xml
<!-- ✅ Unique combinations -->
<changeSet id="1" author="sachin">...</changeSet>
<changeSet id="2" author="sachin">...</changeSet>
<changeSet id="1" author="rahul">...</changeSet>

<!-- ❌ Duplicate — error! -->
<changeSet id="1" author="sachin">...</changeSet>
<changeSet id="1" author="sachin">...</changeSet>
```

### changeSet Attributes

```xml
<changeSet
    id="3"
    author="sachin"
    runAlways="false"
    runOnChange="false"
    failOnError="true"
    context="dev,test"
    labels="users,core"
    dbms="mysql,postgresql">

    <createTable tableName="orders">
        <!-- ... -->
    </createTable>
</changeSet>
```

| Attribute | Description | Default |
|-----------|-------------|---------|
| **id** | Unique identifier | Required |
| **author** | Developer name | Required |
| **runAlways** | Har baar run karo | false |
| **runOnChange** | File change pe run karo | false |
| **failOnError** | Error pe fail karo | true |
| **context** | Environments | All |
| **labels** | Categorization | None |
| **dbms** | Database types | All |

---

## Preconditions

**Matlab:** ChangeSet run karne se pehle conditions check karna — agar condition fail hui toh changeSet skip ya error.

### Basic Precondition

```xml
<changeSet id="4" author="sachin">
    <preConditions onFail="HALT">
        <not>
            <tableExists tableName="users"/>
        </not>
    </preConditions>

    <createTable tableName="users">
        <!-- ... -->
    </createTable>
</changeSet>
```

### onFail Options

| Value | Behavior |
|-------|----------|
| **HALT** (default) | Stop all migrations — error throw karo |
| **WARN** | Warning log karo, lekin continue karo |
| **SKIP** | Is changeSet ko skip karo, aage badho |
| **MARK_RAN** | ChangeSet ko "ran" mark karo, aage badho |

### Multiple PreConditions

```xml
<changeSet id="5" author="sachin">
    <preConditions onFail="HALT">
        <and>
            <tableExists tableName="users"/>
            <not>
                <columnExists tableName="users" columnName="phone"/>
            </not>
        </and>
    </preConditions>

    <addColumn tableName="users">
        <column name="phone" type="VARCHAR(20)"/>
    </addColumn>
</changeSet>
```

### Available Preconditions

```xml
<!-- Table checks -->
<tableExists tableName="users"/>
<tableDoesNotExist tableName="temp_table"/>

<!-- Column checks -->
<columnExists tableName="users" columnName="email"/>
<columnDoesNotExist tableName="users" columnName="phone"/>

<!-- Row count -->
<rowCount tableName="users" expectedRows="0"/>

<!-- SQL check -->
<sqlCheck expectedResult="1">SELECT COUNT(*) FROM users WHERE email IS NOT NULL</sqlCheck>

<!-- DBMS check -->
<dbms type="mysql"/>
<dbms type="postgresql"/>

<!-- Custom SQL -->
<customPrecondition className="com.example.MyPrecondition"/>
```

### Nested Logic

```xml
<preConditions onFail="HALT">
    <or>
        <and>
            <tableExists tableName="users"/>
            <columnExists tableName="users" columnName="email"/>
        </and>
        <dbms type="mysql"/>
    </or>
</preConditions>
```

---

## Rollback

**Matlab:** Migration ko undo karne ka tarika — Liquibase open source mein rollback support karta hai.

### Automatic Rollback

Liquibase kuch changes ka rollback automatically jaanta hai:

```xml
<changeSet id="6" author="sachin">
    <createTable tableName="temp_table">
        <column name="id" type="BIGINT" autoIncrement="true">
            <constraints primaryKey="true"/>
        </column>
    </createTable>

    <!-- Liquibase automatically knows: DROP TABLE temp_table -->
</changeSet>
```

### Manual Rollback

```xml
<changeSet id="7" author="sachin">
    <addColumn tableName="users">
        <column name="phone" type="VARCHAR(20)"/>
    </addColumn>

    <rollback>
        <dropColumn tableName="users" columnName="phone"/>
    </rollback>
</changeSet>
```

### Multiple Rollback Steps

```xml
<changeSet id="8" author="sachin">
    <createTable tableName="user_roles">
        <column name="user_id" type="BIGINT"/>
        <column name="role" type="VARCHAR(50)"/>
    </createTable>

    <insert tableName="user_roles">
        <column name="user_id" valueNumeric="1"/>
        <column name="role" value="ADMIN"/>
    </insert>

    <rollback>
        <delete tableName="user_roles">
            <where>user_id = 1</where>
        </delete>
        <dropTable tableName="user_roles"/>
    </rollback>
</changeSet>
```

### Rollback Commands

```bash
# Rollback last N changeSets
mvn liquibase:rollback -Dliquibase.rollbackCount=1

# Rollback to specific tag
mvn liquibase:rollback -Dliquibase.rollbackTag=v1.0

# Rollback to specific date
mvn liquibase:rollback -Dliquibase.rollbackDate=2024-01-15

# Rollback SQL generate karo (review ke liye)
mvn liquibase:rollbackSQL
```

### Tagging for Rollback

```xml
<changeSet id="9" author="sachin">
    <tagDatabase tag="v2.0"/>
</changeSet>
```

```bash
# Is tag tak rollback karo
mvn liquibase:rollback -Dliquibase.rollbackTag=v2.0
```

---

## runOnChange

**Matlab:** Jab changeSet ki definition change hogi — tabhi run hoga. Views aur stored procedures ke liye useful.

### Usage

```xml
<changeSet id="10" author="sachin" runOnChange="true">
    <createView viewName="active_users_view">
        SELECT id, name, email FROM users WHERE active = true
    </createView>
</changeSet>
```

**Behavior:**
1. Pehli baar — view create hoga
2. Agar file change hui — view drop + recreate hoga
3. Agar file same rahi — skip hoga (already applied mark hai)

### Without runOnChange

```xml
<changeSet id="11" author="sachin">
    <!-- Yeh sirf ek baar run hoga — file change hone pe bhi nahi -->
    <createTable tableName="logs">
        <!-- ... -->
    </createTable>
</changeSet>
```

### runOnChange vs runAlways

| Attribute | Behavior |
|-----------|----------|
| **runOnChange** | File change hone pe run hoga |
| **runAlways** | Har baar run hoga (chahe file same ho) |

```xml
<!-- Har baar run hoga — seed data insert -->
<changeSet id="12" author="sachin" runAlways="true">
    <insert tableName="settings">
        <column name="key" value="app.version"/>
        <column name="value" value="1.0.0"/>
    </insert>
</changeSet>
```

---

## Contexts

**Matlab:** ChangeSets ko environments ke basis pe filter karna — dev mein seed data, prod mein nahi.

### Defining Contexts

```xml
<!-- Sirf dev environment mein run hoga -->
<changeSet id="13" author="sachin" context="dev">
    <insert tableName="users">
        <column name="name" value="Test User"/>
        <column name="email" value="test@example.com"/>
    </insert>
</changeSet>

<!-- Dev aur test dono mein run hoga -->
<changeSet id="14" author="sachin" context="dev, test">
    <insert tableName="users">
        <column name="name" value="QA User"/>
        <column name="email" value="qa@example.com"/>
    </insert>
</changeSet>

<!-- Sab environments mein run hoga (no context = all) -->
<changeSet id="15" author="sachin">
    <createTable tableName="users">
        <!-- ... -->
    </createTable>
</changeSet>
```

### Setting Context

```properties
# application-dev.properties
spring.liquibase.contexts=dev

# application-test.properties
spring.liquibase.contexts=test

# application-prod.properties
spring.liquibase.contexts=prod

# Command line
mvn liquibase:update -Dliquibase.contexts=dev
```

### Context Expressions

```xml
<!-- dev AND not staging -->
<changeSet id="16" author="sachin" context="dev AND NOT staging">
    <!-- ... -->
</changeSet>

<!-- dev OR test -->
<changeSet id="17" author="sachin" context="dev, test">
    <!-- ... -->
</changeSet>
```

---

## Labels

**Matlab:** ChangeSets ko categorize karna — reporting aur filtering ke liye.

### Usage

```xml
<changeSet id="18" author="sachin" labels="users,core">
    <createTable tableName="users">
        <!-- ... -->
    </createTable>
</changeSet>

<changeSet id="19" author="sachin" labels="orders,core">
    <createTable tableName="orders">
        <!-- ... -->
    </createTable>
</changeSet>

<changeSet id="20" author="sachin" labels="users,performance">
    <createIndex indexName="idx_users_email" tableName="users">
        <column name="email"/>
    </createIndex>
</changeSet>
```

### Label Filtering

```properties
# Sirf "users" labelled changes run karo
spring.liquibase.labels=users

# Multiple labels
spring.liquibase.labels=users,core
```

### Labels vs Contexts

| Feature | Contexts | Labels |
|---------|----------|--------|
| **Purpose** | Environment filtering | Categorization |
| **Execution** | Affects which changeSets run | Affects reporting/filtering |
| **Syntax** | `context="dev"` | `labels="users,core"` |

---

## liquibase.changeLogFile Property

**Matlab:** Master changelog file ka path — yahan se Liquibase sab migrations read karta hai.

### Single Changelog

```properties
# application.properties
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml
```

```xml
<!-- db/changelog/db.changelog-master.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog>
    <include file="db/changelog/changes/001-create-users.xml" relativeToChangelogFile="true"/>
    <include file="db/changelog/changes/002-create-orders.xml" relativeToChangelogFile="true"/>
    <include file="db/changelog/changes/003-add-indexes.xml" relativeToChangelogFile="true"/>
</databaseChangeLog>
```

### Organized Structure

```
src/main/resources/
└── db/
    └── changelog/
        ├── db.changelog-master.xml          # Master file
        └── changes/
            ├── 001-create-users.xml
            ├── 002-create-orders.xml
            ├── 003-add-indexes.xml
            └── 004-seed-data.xml
```

### Multiple Includes with Contexts

```xml
<databaseChangeLog>
    <!-- Core schema — sab environments -->
    <include file="db/changelog/changes/001-schema.xml" relativeToChangelogFile="true"/>

    <!-- Seed data — sirf dev/test -->
    <include file="db/changelog/changes/002-seed-data.xml"
             relativeToChangelogFile="true"
             context="dev, test"/>

    <!-- Performance indexes — sirf prod -->
    <include file="db/changelog/changes/003-indexes.xml"
             relativeToChangelogFile="true"
             context="prod"/>
</databaseChangeLog>
```

### YAML Changelog

```properties
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml
```

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/changes/001-create-users.yaml
      relativeToChangelogFile: true
  - include:
      file: db/changelog/changes/002-create-orders.yaml
      relativeToChangelogFile: true
```

### SQL Changelog

```properties
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.sql
```

```sql
--liquibase formatted sql

--changeset sachin:1
CREATE TABLE users (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100));

--changeset sachin:2
ALTER TABLE users ADD COLUMN email VARCHAR(100);
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Liquibase** | Database change management — XML/YAML/JSON/SQL formats |
| **changeSet** | Unique `id` + `author` combination se identify hota hai |
| **Preconditions** | ChangeSet run karne se pehle conditions check |
| **Rollback** | Open source mein supported — manual rollback define karo |
| **runOnChange** | File change hone pe re-run karo (views, stored procs) |
| **Contexts** | Environment-specific changeSets (dev, test, prod) |
| **Labels** | Categorization — reporting aur filtering ke liye |
| **changeLogFile** | Master changelog file — includes se organized structure |
| **DATABASECHANGELOG** | Liquibase ki tracking table — applied changeSets track karti hai |
