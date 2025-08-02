package com.throttling.ratelimiter.pojo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import com.throttling.ratelimiter.enums.LimitType;
import com.throttling.ratelimiter.enums.TimeUnit;

import lombok.Data;

@Data
@RedisHash("ClientRateLimitData")
public class ClientRateLimitData {

  @Id
  private String id; // Composite key: clientId + limitType + limitName

  @Indexed
  private String clientId;

  @Indexed
  private LimitType limitType;

  @Indexed
  private String limitName;

  private TimeUnit timeUnit;

  private long maxPermits;

  private long availablePermits;

  private long lastRequestTimeStamp;

  public ClientRateLimitData() {}

  public ClientRateLimitData(String clientId, LimitType limitType, String limitName,
      TimeUnit timeUnit, long maxPermits, long availablePermits, long lastRequestTimeStamp) {
    this.clientId = clientId;
    this.limitType = limitType;
    this.limitName = limitName;
    this.timeUnit = timeUnit;
    this.maxPermits = maxPermits;
    this.availablePermits = availablePermits;
    this.lastRequestTimeStamp = lastRequestTimeStamp;

    // Generate the composite key
    this.id = generateId(clientId, limitType, limitName);
  }

  private String generateId(String clientId, LimitType limitType, String limitName) {
    return clientId + ":" + limitType.name() + ":" + limitName;
  }

}
