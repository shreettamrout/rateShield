package com.throttling.ratelimiter.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.throttling.ratelimiter.constant.Message;
import com.throttling.ratelimiter.dataconstructor.ClientDataConstructor;
import com.throttling.ratelimiter.enums.Status;
import com.throttling.ratelimiter.pojo.model.ClientRateLimitData;
import com.throttling.ratelimiter.pojo.request.ClientApiRequest;
import com.throttling.ratelimiter.pojo.request.ClientConfigRequest;
import com.throttling.ratelimiter.pojo.response.BaseResponse;
import com.throttling.ratelimiter.repository.ClientRateLimitsRepository;
import com.throttling.ratelimiter.service.ClientLockService;
import com.throttling.ratelimiter.service.ClientRateLimitingService;
import com.throttling.ratelimiter.util.TimeUnitConversionUtil;
import com.throttling.ratelimiter.util.ValidationUtil;

@Service
public class ClientRateLimitingServiceImpl implements ClientRateLimitingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientRateLimitingServiceImpl.class);

    @Autowired
    private ClientLockService clientLockService;

    @Autowired
    private ClientConfigServiceImpl clientConfigServiceImpl;

    @Value("${ratelimiter.default.config.enabled:false}")
    private boolean isDefaultConfigEnabled;

    @Override
    public BaseResponse verifyApiLimit(ClientApiRequest clientApiRequest) {

        String clientId = clientApiRequest.getClientId();
        String methodName = clientApiRequest.getMethodName().name();
        String apiName = clientApiRequest.getApiName();

        // Acquire the lock for atomic operations
        clientLockService.acquireLock(clientId);

        try {
            // Validate the client ID
            ValidationUtil.validateClientId(clientId);

            // Check if the client exists
            if (!clientConfigServiceImpl.isClientPresent(clientId)) {
                LOGGER.info("Client ID {} not found.", clientId);

                if (isDefaultConfigEnabled) {
                    LOGGER.info("Default configuration is enabled. Adding client ID {} to DB with default configuration.", clientId);

                    // Add client to DB with default limits
                    ClientConfigRequest defaultConfig = ClientDataConstructor.constructDefaultClientConfig(clientId);
                    clientConfigServiceImpl.addOrUpdateRateLimits(defaultConfig);

                } else {
                    LOGGER.warn("Default configuration is disabled. Rejecting request for client ID {}.", clientId);
                    return new BaseResponse(Status.FAILURE, "Client not configured, and default configuration is disabled.");
                }
            }

            long currentTimestamp = System.currentTimeMillis();

            // Fetch and validate applicable limits
            List<ClientRateLimitData> applicableApiLimits = clientConfigServiceImpl.fetchApplicableRateLimits(clientId, methodName,apiName);

            // Handle empty rate limits case
            if (applicableApiLimits.isEmpty()) {
                LOGGER.error("No applicable rate limits found for client ID {} and API {}. Request denied.", clientId, apiName);
                return new BaseResponse(Status.FAILURE, "Rate limit configuration missing. Request denied.");
            }

            // Process rate limits
            for (ClientRateLimitData clientApiLimit : applicableApiLimits) {
                long elapsedTimeMillis = currentTimestamp - clientApiLimit.getLastRequestTimeStamp();
                double elapsedTimeUnits = TimeUnitConversionUtil.convert(elapsedTimeMillis, clientApiLimit.getTimeUnit());

                long updatedAvailablePermits = (long) Math.min(
                        clientApiLimit.getAvailablePermits() + elapsedTimeUnits * clientApiLimit.getMaxPermits(),
                        clientApiLimit.getMaxPermits());

                if (updatedAvailablePermits < 1) {
                    LOGGER.warn("Rate limit breached for client {} on limit {}", clientId, clientApiLimit.getLimitName());
                    return new BaseResponse(Status.FAILURE, "Rate Limit reached for " + clientId);
                }

                // Update the limit usage
                updatedAvailablePermits--;
                clientApiLimit.setAvailablePermits(updatedAvailablePermits);
                clientApiLimit.setLastRequestTimeStamp(currentTimestamp);
            }

            // Save updated limits using the service method
            clientConfigServiceImpl.saveOrUpdateRateLimits(applicableApiLimits);

            return new BaseResponse(Status.SUCCESS, Message.SUCCESS);

        } finally {
            // Release the lock only if it was acquired
            clientLockService.releaseLock(clientId);
        }
    }
}
