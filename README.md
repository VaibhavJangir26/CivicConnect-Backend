# ⚙️ CivicConnect Backend — Distributed Incident Resolution Engine

[![Java 17](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot 3](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.x-005571?style=for-the-badge&logo=elasticsearch&logoColor=white)](#-1-elasticsearch-8x-search--autosuggest-engine)
[![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?style=for-the-badge&logo=redis&logoColor=white)](#-2-redis-7x-rate-limiting--otp-engine)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?style=for-the-badge&logo=docker&logoColor=white)](#-docker--containerization)
[![Spring Security 6](https://img.shields.io/badge/Security-Spring_Security_6-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](#-4-spring-security-6--rbac)

CivicConnect is an **enterprise-grade, high-throughput REST API backend engine** built with **Java 17**, **Spring Boot 3**, **Spring Security 6**, **Elasticsearch**, **Redis**, and **PostgreSQL**. It powers real-time citizen complaint registration, role-based workflow dispatching, distributed search, rate limiting, and social OAuth2 authentication.

---

## 🌟 Executive Summary

This codebase showcases **production-level backend software engineering patterns**:

* 🔎 **Elasticsearch 8.x Auto-Suggest Engine**: Distributed full-text search index (`complaints_index`) with debounced prefix/fuzzy matching (`/api/v1/complains/suggest`) and multi-field keyword search (`/api/v1/complains/search`).
* 🚀 **Redis 7.x Micro-Architecture & Rate Limiter**:
  - **Sliding Window Rate Limiting**: Redis-backed rate limiting to defend auth and search endpoints against DDoS/brute-force attacks.
  - **Distributed Feature-Flagged OTP Engine (`app.otp.redis-store.enabled`)**: Production feature flag allowing 2-step registration OTP codes to be stored directly in Redis with 5-minute TTL (`EXPIRE`), with fallback to in-memory concurrent maps.
  - **Token Blacklisting**: Instant JWT session revocation and blacklisting cached in Redis.
* 🔑 **OAuth2 & Dual Authentication**: Google and GitHub social logins alongside standard username/password authentication, returning stateless JWT Access & Refresh Tokens.
* 🛡️ **Role-Based Access Control (RBAC)**: Fine-grained Spring Security 6 authorization hierarchy (`CITIZEN`, `OFFICER`, `MANAGER`, `SUPER_ADMIN`) enforced via `@PreAuthorize` method annotations.
* 🐳 **Docker & Containerization**: Fully containerized using Docker and `docker-compose.yml` for single-command deployment of PostgreSQL, Redis, Elasticsearch, and the Spring Boot application.

---

## 🏗️ System Architecture & Technology Stack

```
                              ┌────────────────────────┐
                              │  CivicConnect UI (Web) │
                              └───────────┬────────────┘
                                          │  REST / JSON (JWT)
                                          ▼
                              ┌────────────────────────┐
                              │     Spring Boot 3      │
                              │   REST API Gateway     │
                              └─────┬─────┬─────┬──────┘
                                    │     │     │
         ┌──────────────────────────┘     │     └──────────────────────────┐
         ▼                                ▼                                ▼
┌──────────────────┐             ┌──────────────────┐             ┌──────────────────┐
│  Elasticsearch   │             │   Redis 7.x      │             │  PostgreSQL 15   │
│  Auto-Suggest &  │             │  Rate Limiting,  │             │  Relational DB   │
│ Full-Text Search │             │ OTP (TTL) & Cache│             │ Entities & Audit │
└──────────────────┘             └──────────────────┘             └──────────────────┘
```

---

## ⚡ Technical Capabilities & Engineering Deep Dive

### 1. 🔎 Elasticsearch 8.x Search & Autosuggest Engine
* **Real-time Document Indexing**: Incidents are automatically synchronized with Elasticsearch upon creation or status update.
* **Debounced Auto-Suggestion API**:
  - Endpoint: `GET /api/v1/complains/suggest?query={query}`
  - Performs real-time prefix and fuzzy matching on incident messages, categories, and locations to return top suggestions.
* **Full-Text Keyword Search API**:
  - Endpoint: `GET /api/v1/complains/search?keyword={keyword}`
  - Multi-match query spanning `message`, `category.name`, `address`, and `status`.

### 2. 🚀 Redis 7.x Rate Limiting & OTP Engine
* **Rate Limiting**: Protects sensitive endpoints against high-concurrency brute-force attacks using sliding window counters.
* **Feature-Flagged OTP Storage**:
  ```yaml
  # application.yml
  app:
    otp:
      redis-store:
        enabled: true  # Toggle between Redis TTL store and In-Memory Fallback
        ttl-minutes: 5
  ```
  When enabled, generated registration OTP codes are stored in Redis key `otp:{email}` with automatic 300-second TTL expiration.
* **Token Blacklisting**: Revoked user tokens are stored in Redis set `blacklisted_tokens:{jti}` to invalidate sessions immediately.

### 3. 🔑 Social OAuth2 & JWT Token Management
* Integrates Google & GitHub OAuth2 providers.
* Server-side authorization code exchange provisions the user profile and generates dual JWT Access Tokens (short-lived) and Refresh Tokens (long-lived).

### 4. 🛡️ Spring Security 6 & RBAC
* Role hierarchy:
  - `CITIZEN`: Submit & track tickets.
  - `OFFICER`: Manage & resolve assigned field tickets.
  - `MANAGER`: Regional complaint oversight & staff assignments.
  - `SUPER_ADMIN`: Comprehensive control over directories, role upgrades, account locks, and categories.

---

## 🛠️ Step-by-Step Installation & Setup

### Prerequisites
* **Java**: Version 17 or higher (`java -version`).
* **Maven**: Version 3.8+ (`mvn -version`).
* **Docker & Docker Compose** (Recommended for quick start).

---

### Option 1: Quick Start via Docker Compose (Recommended)

Run all services (PostgreSQL, Redis, Elasticsearch, and Backend) in one command:

```bash
# Clone the repository
git clone https://github.com/VaibhavJangir26/CivicConnectBackend.git
cd finedgebank

# Start all containers in detached mode
docker-compose up -d --build

# Verify running containers
docker-compose ps
```

The REST API will be live at `http://localhost:8000/api/v1`.

---

### Option 2: Manual Local Development Setup

#### 1. Start Infrastructure Services

Make sure PostgreSQL, Redis, and Elasticsearch are running locally:

```bash
# PostgreSQL (Port 5432)
createdb civicconnect_db

# Redis (Port 6379)
redis-server

# Elasticsearch (Port 9200)
elasticsearch
```

#### 2. Configure Environment Variables

Create a `.env` file or export environment variables:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/civicconnect_db
export DB_USERNAME=postgres
export DB_PASSWORD=postgres

export REDIS_HOST=localhost
export REDIS_PORT=6379

export ELASTICSEARCH_URL=http://localhost:9200

export JWT_SECRET=your_jwt_secret
export OTP_REDIS_ENABLED=true
```

#### 3. Build & Run Application

```bash
# Install dependencies & run tests
mvn clean package

# Launch Spring Boot Server
mvn spring-boot:run
```

---

## 📋 API Endpoints Reference

### 🔐 Authentication (`/api/v1/auth`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Initiate registration & trigger email OTP |
| `POST` | `/api/v1/auth/verify-otp` | Verify 6-digit OTP & activate account |
| `POST` | `/api/v1/auth/login` | Authenticate credentials & issue JWT tokens |
| `GET` | `/api/v1/auth/check-username` | Real-time username availability check |
| `GET` | `/api/v1/auth/check-email` | Real-time email availability check |
| `GET` | `/api/v1/auth/oauth2/code/{provider}` | OAuth2 callback code exchange |

### 📝 Incidents & Search (`/api/v1/complains`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/complains` | File a new citizen complaint |
| `GET` | `/api/v1/complains/my` | Retrieve complaints filed by active citizen |
| `GET` | `/api/v1/complains/assigned` | Retrieve tickets assigned to active officer |
| `GET` | `/api/v1/complains/suggest` | Elasticsearch autocomplete suggestions |
| `GET` | `/api/v1/complains/search` | Elasticsearch multi-field full-text search |

### ⚙️ Admin & Staff (`/api/v1/admin`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/admin/users` | Retrieve full registered user directory |
| `PUT` | `/api/v1/admin/users/{id}/role` | Upgrade/assign user role (`SUPER_ADMIN` only) |
| `PUT` | `/api/v1/admin/users/{id}/status` | Update account status (`ACTIVE`, `SUSPENDED`) |
