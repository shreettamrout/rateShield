package com.throttling.ratelimiter.converter;

import com.throttling.ratelimiter.pojo.model.ClientRateLimitData;
import com.throttling.ratelimiter.pojo.request.ClientConfigRequest;
import com.throttling.ratelimiter.pojo.request.ClientLimitsConfigRequest;
import com.throttling.ratelimiter.pojo.request.ClientIntervalRequestsLimit;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ClientConfigRequestConverter {

    //converts request data into client rate limit data
    public List<ClientRateLimitData> convert(ClientConfigRequest clientConfigRequest) {
        long currentTimestamp = System.currentTimeMillis();
        List<ClientRateLimitData> clientRateLimits = new ArrayList<>();
        
        if(clientConfigRequest.getLimits()!=null)
        {
        // Process each ClientLimitsConfigRequest in the request
        for (ClientLimitsConfigRequest limitsConfig : clientConfigRequest.getLimits()) {
            clientRateLimits.add(createRateLimitData(clientConfigRequest.getClientId(), limitsConfig, currentTimestamp));
        }
        }

        return clientRateLimits;
    }

    
    private ClientRateLimitData createRateLimitData(String clientId, ClientLimitsConfigRequest limitsConfig, long currentTimestamp) {
        ClientIntervalRequestsLimit timeIntervalLimit = limitsConfig.getTimeIntervalLimit();

        return new ClientRateLimitData(
                clientId,
                limitsConfig.getLimitType(),
                limitsConfig.getLimitName(),
                timeIntervalLimit.getTimeUnit(),
                timeIntervalLimit.getMaxRequests(),
                timeIntervalLimit.getMaxRequests(),
                currentTimestamp
        );
    }
}
