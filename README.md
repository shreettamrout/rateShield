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
long elapsedTimeMillis = currentTimestamp - clientApiLimit.getLastRequestTimeStamp();
double elapsedTimeUnits = TimeUnitConversionUtil.convert(elapsedTimeMillis, clientApiLimit.getTimeUnit());
long updatedAvailablePermits = (long) Math.min(
    clientApiLimit.getAvailablePermits() + elapsedTimeUnits * clientApiLimit.getMaxPermits(),
    clientApiLimit.getMaxPermits()
);

if (updatedAvailablePermits < 1) {
    return new BaseResponse(Status.FAILURE, "Rate limit exceeded");
}

updatedAvailablePermits--;
clientApiLimit.setAvailablePermits(updatedAvailablePermits);
clientApiLimit.setLastRequestTimeStamp(currentTimestamp);
```
## 📡 Supported APIs

### 1. Verify API Request Limit

- **POST** `/ratelimiter/verify-api-limit`
- **Request**
```json
{
  "clientId": "client123",
  "apiName": "getUsers",
  "methodName": "GET"
}
```
Response

```json
Copy
Edit
{
  "status": "SUCCESS"
}
```
2. Add / Update Client Configuration
POST /ratelimiter/configure-client

Request

```json
Copy
Edit
{
  "clientId": "client123",
  "limits": [
    {
      "limitType": "DEFAULT",
      "limitName": "GLOBAL",
      "timeIntervalLimit": {
        "timeUnit": "SEC",
        "maxRequests": 2
      }
    },
    {
      "limitType": "API",
      "limitName": "/web/test",
      "timeIntervalLimit": {
        "timeUnit": "HOUR",
        "maxRequests": 60
      }
    },
    {
      "limitType": "METHOD",
      "limitName": "PUT",
      "timeIntervalLimit": {
        "timeUnit": "MIN",
        "maxRequests": 10
      }
    }
  ]
}
```
3. Get Client Rate-Limiting Status
GET /ratelimiter/client-limits

Query Parameter: clientId=client123

4. Get All Configured Rate Limits
GET /ratelimiter/configured-limits

5. Delete Specific Rate Limits
DELETE /ratelimiter/delete-limits

Request

```json
Copy
Edit
{
  "clientId": "client123",
  "limitType": "API",
  "limitName": "/web/test"
}
```
6. Delete Client and Its Configuration
DELETE /ratelimiter/delete-client

Request

```json
Copy
Edit
{
  "clientId": "client123"
}
```
🧱 Redis Structure
🔑 Rate Limit Keys
Format:
rate-limit:{clientId}:{limitType}:{limitName}

📦 Value Structure
```json
Copy
Edit
{
  "availablePermits": 4,
  "maxPermits": 10,
  "lastRequestTimeStamp": 1690977600000,
  "timeUnit": "MIN"
}
```
🔐 Lock Key
Format:
lock:{clientId}


