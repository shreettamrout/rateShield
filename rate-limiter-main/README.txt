Rate Limiter Project Design Document 

1. Project Overview

The Rate Limiter project implements a robust and scalable mechanism to enforce API rate-limiting based on client-specific configurations. The system supports rate-limiting using the Token Bucket Algorithm and is designed to handle multiple concurrent requests efficiently.

Key Features:
- Rate-limiting based on client identifiers such as Client ID, API Key, or IP Address.
- Token Bucket Algorithm for precise and fair rate control.
- Flexible configuration of rate limits (time unit, maximum permits).
- Concurrency control using a Lock Service for client-specific operations.
- Support for dynamically adding clients with default rate-limiting rules.
- RESTful APIs for managing rate-limiting configurations and status.
- Redis integration for distributed caching and persistence, enabling high availability and scalability.
- Distributed locking using RedisLock (RLock) for ensuring safe concurrent access.


2. High-Level Architecture 

2.1 Components 
1. Filter Layer: 
   - Extracts client information (Client ID or API Key or IP Address) from incoming requests.
   - Passes the extracted data to the Rate-Limiting Service.

2. Rate-Limiting Service: 
   - Implements the Token Bucket Algorithm.
   - Tracks request counts and enforces limits.

3. Persistence Layer: 
   - Stores rate-limiting configurations and request metadata.
   - Uses Redis for distributed and fast access to data.

4. Lock Service: 
   - Ensures thread-safe operations for client-specific rate-limiting data using RedisLock (RLock).

2.2 Workflow 
1. The request enters the filter, which extracts the client-specific data.
2. The filter passes the request to the Rate-Limiting Service for validation.
3. The Rate-Limiting Service checks and updates the token count for the client.
4. If the client exceeds the rate limit, the service returns a BaseResponse(Status, Message) response.
5. Valid requests proceed to the application’s business logic.


3. Token Bucket Algorithm Implementation 

The Token Bucket Algorithm allows requests up to a fixed rate while permitting bursts of traffic within a defined capacity.

Steps in the Code: 
1. Fetch Limits: Retrieve applicable rate-limiting configurations for the client.
2. Calculate Tokens: 
   - Calculate tokens to add based on elapsed time since the last request.
   - Ensure tokens do not exceed the bucket’s maximum capacity.
3. Enforce Limits: 
   - Reject requests if insufficient tokens are available.
   - Deduct one token for each valid request.
4. Update State: 
   - Save the updated token count and timestamp to Redis.

Example Code: 
  
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


4. Lock Service 

The Lock Service ensures thread safety when multiple concurrent requests are processed for the same client.

Implementation Details: 
1. Lock Storage: 
   - Uses RedisLock (RLock) for distributed locking.

2. Add Lock: 
   - Automatically creates a distributed lock for a new client.

3. Acquire Lock: 
   - Ensures only one thread modifies the rate-limiting data for a client at a time, distributed across multiple instances.

4. Release Lock: 
   - Frees the distributed lock after the operation is complete.

Concurrency Handling: 
- For multiple requests to the same client:
  - The first thread acquires the lock and processes the request.
  - Other threads block until the lock is released.
- Guarantees atomic updates to client-specific rate-limiting data using RedisLock.


5. Supported APIs 

1. Client Rate-Limit Verification: 
   - Endpoint: `POST /ratelimiter/verify-api-limit`
   - Description: Verifies whether the client’s request complies with the rate limit.
   - Input: 
     {
       "clientId": "client123",
       "apiName": "getUsers",
       "methodName": "GET"
     }
   - Output: 
     - Success or FAILURE

2. Add/Update Client Configuration: 
   - Endpoint: `POST /ratelimiter/configure-client`
   - Description: Adds or updates rate-limiting configurations for a client.
   - Input: 
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

3. Get Client Rate-Limiting Status: 
   - Endpoint: `GET /ratelimiter/client-limits`
   - Description: Retrieves the current rate-limiting status for a client.

4. Get All Configured Rate Limits: 
   - Endpoint: `GET /ratelimiter/configured-limits`
   - Description: Retrieves all the rate-limiting configurations.

5. Delete Specific Rate Limits: 
   - Endpoint: `DELETE /ratelimiter/delete-limits`
   - Description: Deletes specific rate limits for a client.

6. Delete Client and Its Configuration: 
   - Endpoint: `DELETE /ratelimiter/delete-client`
   - Description: Deletes a client and all its configured rate limits.


6. Tech Stack 

- Java: Core language for implementation.
- Spring Boot: Framework for building RESTful APIs.
- Redis: Distributed in-memory data store for persistence and caching.
- RedisLock (RLock): Distributed locking for concurrency control.
- Maven: Build automation and dependency management.


8. Conclusion 

This project delivers a rate-limiting solution leveraging the Token Bucket Algorithm. The integration of Redis ensures distributed, fast, and scalable data access. The use of RedisLock guarantees safe and distributed thread management. The flexible configuration mechanism enables diverse use cases. The architecture supports future enhancements and deployment in distributed environments.

