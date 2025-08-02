package com.throttling.ratelimiter.dataconstructor;

import com.throttling.ratelimiter.enums.LimitType;
import com.throttling.ratelimiter.enums.TimeUnit;
import com.throttling.ratelimiter.pojo.request.ClientConfigRequest;
import com.throttling.ratelimiter.pojo.request.ClientIntervalRequestsLimit;
import com.throttling.ratelimiter.pojo.request.ClientLimitsConfigRequest;

import java.util.Collections;

public class ClientDataConstructor {

    //create default rate limit for client (Default type , per second 10 calls allowed)
    public static ClientConfigRequest constructDefaultClientConfig(String clientId) {
        // Create a time interval limit
        ClientIntervalRequestsLimit clientIntervalRequestLimit = createDefaultIntervalLimit();

        // Create a limits configuration
        ClientLimitsConfigRequest clientLimitsConfigRequest = createDefaultLimitsConfig(clientIntervalRequestLimit);

        // Build the client configuration request
        ClientConfigRequest clientConfigRequest = new ClientConfigRequest();
        clientConfigRequest.setClientId(clientId);
        clientConfigRequest.setLimits(Collections.singletonList(clientLimitsConfigRequest));

        return clientConfigRequest;
    }

    private static ClientIntervalRequestsLimit createDefaultIntervalLimit() {
        ClientIntervalRequestsLimit clientIntervalRequestLimit = new ClientIntervalRequestsLimit();
        clientIntervalRequestLimit.setMaxRequests(10);
        clientIntervalRequestLimit.setTimeUnit(TimeUnit.SEC);
        return clientIntervalRequestLimit;
    }

    
    private static ClientLimitsConfigRequest createDefaultLimitsConfig(ClientIntervalRequestsLimit clientIntervalRequestLimit) {
        ClientLimitsConfigRequest clientLimitsConfigRequest = new ClientLimitsConfigRequest();
        clientLimitsConfigRequest.setLimitType(LimitType.DEFAULT);
        clientLimitsConfigRequest.setLimitName("GLOBAL");
        clientLimitsConfigRequest.setTimeIntervalLimit(clientIntervalRequestLimit);
        return clientLimitsConfigRequest;
    }
}
