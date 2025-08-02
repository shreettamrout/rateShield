# 🚦 Rate Limiter Service (Token Bucket + RedisLock)

A scalable and client-specific API rate limiting service built using the **Token Bucket Algorithm**, **Redis** for distributed state management, and **RedisLock (RLock)** for concurrency safety. This service supports dynamic configuration of rate limits and is production-ready for distributed applications.

---

## 📘 Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [Token Bucket Algorithm](#-token-bucket-algorithm)
- [Concurrency Handling](#-concurrency-handling)
- [API Documentation](#-api-documentation)
- [Data Model in Redis](#-data-model-in-redis)
- [Tech Stack](#-tech-stack)
- [Setup & Installation](#-setup--installation)
- [Configuration](#-configuration)
- [Testing the API](#-testing-the-api)
- [Future Improvements](#-future-improvements)

---

## 🔍 Overview

This service enforces rate limits per client using their:
- Client ID
- API Key
- or IP Address

It supports three types of limits:
1. **DEFAULT (Global)**
2. **API-based**
3. **HTTP Method-based**

Each request is evaluated using the **Token Bucket Algorithm**, and concurrent requests are managed using **Redis distributed locking**.

---

## 🏗️ Architecture

### 🧱 Components:

| Component       | Role |
|----------------|------|
| Filter Layer    | Extracts client metadata (e.g., IP, Client ID) from incoming requests |
| Rate Limiting Service | Applies rate limits using Token Bucket |
| Persistence Layer | Uses Redis to store client metadata, token states, and configs |
| Lock Service    | Manages distributed locks to prevent race conditions |

### 🔄 Request Flow:

1. Request hits the application.
2. Filter extracts clientId / API key / IP address.
3. RateLimitingService acquires a RedisLock for that client.
4. Token Bucket logic is applied:
   - If tokens available → proceed.
   - If not → reject.
5. Token state updated in Redis.
6. Lock is released.

---

## 🪣 Token Bucket Algorithm

The Token Bucket Algorithm allows requests at a fixed rate while allowing short bursts.

### Parameters:
- `maxPermits`: max burst size.
- `timeUnit`: refill interval (sec/min/hour).
- `availablePermits`: current tokens.

### Logic (Pseudocode):
```java
long elapsedMillis = now - lastRequestTime;
double unitsPassed = convertToTimeUnits(elapsedMillis);
long newTokens = unitsPassed * maxPermits;

availableTokens = min(availableTokens + newTokens, maxPermits);

if (availableTokens < 1) {
    return RATE_LIMIT_EXCEEDED;
}

availableTokens -= 1;
updateRedis(clientId, availableTokens, now);
