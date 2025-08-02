package com.throttling.ratelimiter.service;

import java.util.List;

import com.throttling.ratelimiter.pojo.request.ClientConfigRequest;
import com.throttling.ratelimiter.pojo.response.BaseResponse;

public interface ClientConfigService {

  BaseResponse addOrUpdateRateLimits(ClientConfigRequest clientConfigRequest);

  BaseResponse deleteRateLimits(ClientConfigRequest clientConfigRequest);

  BaseResponse removeClient(String clientId);
  
  BaseResponse addClient(String clienId);

  List<?> getAllRateLimits();

  List<?> getClientLimits(String clientId);
  
}
