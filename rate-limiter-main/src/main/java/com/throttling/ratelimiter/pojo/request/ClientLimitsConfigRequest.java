package com.throttling.ratelimiter.pojo.request;

import com.throttling.ratelimiter.enums.LimitType;
import lombok.Data;

@Data
public class ClientLimitsConfigRequest {

  private LimitType limitType;

  private String limitName;

  private ClientIntervalRequestsLimit timeIntervalLimit;

}
