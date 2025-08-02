package com.throttling.ratelimiter.pojo.model;




import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value="ClientData")
public class ClientData {

  @Id
  @Indexed
  private String clientId;
}
