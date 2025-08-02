package com.throttling.ratelimiter.controller;

import com.throttling.ratelimiter.pojo.request.ClientApiRequest;
import com.throttling.ratelimiter.pojo.request.ClientConfigRequest;
import com.throttling.ratelimiter.pojo.response.BaseResponse;
import com.throttling.ratelimiter.service.ClientConfigService;
import com.throttling.ratelimiter.service.ClientRateLimitingService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ratelimiter")
public class ClientLimitConfigController {

  @Autowired
  private ClientConfigService clientConfigService;

  @Autowired
  private ClientRateLimitingService clientRateLimitingService;

  @PostMapping("/configure-client")
  public BaseResponse configureClient(@RequestBody ClientConfigRequest clientConfigRequest) {
    return clientConfigService.addOrUpdateRateLimits(clientConfigRequest);
  }

  @PostMapping("/verify-api-limit")
  public BaseResponse verifyApiLimit(@RequestBody ClientApiRequest clientApiRequest) {
    return clientRateLimitingService.verifyApiLimit(clientApiRequest);
  }

  @GetMapping("/configured-limits")
  public List<?> getConfiguredLimits() {
      return clientConfigService.getAllRateLimits();
  }
  
  @GetMapping("/client-limits")
  public List<?> getConfiguredLimits(@RequestBody ClientConfigRequest clientConfigRequest) {
      return  clientConfigService.getClientLimits(clientConfigRequest.getClientId());
  }
  
  @DeleteMapping("/delete-limits")
  public BaseResponse deleteLimit(@RequestBody ClientConfigRequest clientConfigRequest) {
      return clientConfigService.deleteRateLimits(clientConfigRequest);
  }
  
  @DeleteMapping("/delete-client")
  public BaseResponse deleteClientLimits(@RequestBody ClientConfigRequest clientConfigRequest) {
      return clientConfigService.removeClient(clientConfigRequest.getClientId());
  }

}
