package com.throttling.ratelimiter.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.throttling.ratelimiter.constant.RateLimitData;
import com.throttling.ratelimiter.converter.ClientConfigRequestConverter;
import com.throttling.ratelimiter.enums.LimitType;
import com.throttling.ratelimiter.enums.Status;
import com.throttling.ratelimiter.pojo.model.ClientData;
import com.throttling.ratelimiter.pojo.model.ClientRateLimitData;
import com.throttling.ratelimiter.pojo.request.ClientConfigRequest;
import com.throttling.ratelimiter.pojo.request.ClientLimitsConfigRequest;
import com.throttling.ratelimiter.pojo.response.BaseResponse;
import com.throttling.ratelimiter.repository.ClientRateLimitsRepository;
import com.throttling.ratelimiter.repository.ClientRepository;
import com.throttling.ratelimiter.service.ClientConfigService;
import com.throttling.ratelimiter.util.ValidationUtil;

@Service
public class ClientConfigServiceImpl implements ClientConfigService {
	
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	@Value("${rate.limit.cache.prefix:rate_limit:}")
	private String cachePrefix;


	private static final Logger logger = LoggerFactory.getLogger(ClientConfigServiceImpl.class);

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private ClientRateLimitsRepository clientRateLimitsRepository;

	@Autowired
	private ClientConfigRequestConverter clientConfigRequestConverter;
	
	@PostConstruct
	public void initializeCache() {
	    List<ClientRateLimitData> rateLimits = getAllRateLimits();
	    rateLimits.forEach(rateLimit -> {
	        String key = buildRateLimitKey(rateLimit.getClientId(), rateLimit.getLimitType(), rateLimit.getLimitName());
	        redisTemplate.opsForValue().set(cachePrefix + key, rateLimit);
	    });
	    logger.info("Rate limit cache initialized.");
	}


	public BaseResponse addOrUpdateRateLimits(ClientConfigRequest clientConfigRequest) {
	    validateAndAddClient(clientConfigRequest);
	    List<ClientRateLimitData> rateLimits = convertRateLimits(clientConfigRequest);
	    saveOrUpdateRateLimits(rateLimits);
	    return new BaseResponse(Status.SUCCESS, "Rate limits processed successfully");
	}

	private void validateAndAddClient(ClientConfigRequest clientConfigRequest) {
	    ValidationUtil.validateClientId(clientConfigRequest.getClientId());
	    if (!isClientPresent(clientConfigRequest.getClientId())) {
	        addClient(clientConfigRequest.getClientId());
	    }
	}

	private List<ClientRateLimitData> convertRateLimits(ClientConfigRequest clientConfigRequest) {
	    return clientConfigRequestConverter.convert(clientConfigRequest);
	}

	public void saveOrUpdateRateLimits(List<ClientRateLimitData> rateLimits) {
	    if (rateLimits != null && !rateLimits.isEmpty()) {
	        rateLimits.forEach(this::saveOrUpdateRateLimit);
	    }
	}


	@Override
	@Transactional
	public BaseResponse deleteRateLimits(ClientConfigRequest clientConfigRequest) {
	    String clientId = clientConfigRequest.getClientId();
	    List<ClientLimitsConfigRequest> limitsToDelete = clientConfigRequest.getLimits();

	    if (CollectionUtils.isEmpty(limitsToDelete)) {
	        return new BaseResponse(Status.FAILURE, "Limits to delete cannot be null or empty.");
	    }

	    AtomicBoolean deletedAtLeastOne = new AtomicBoolean(false);
	    limitsToDelete.forEach(limitConfig -> {
	        ClientRateLimitData rateLimit = getRateLimitData(clientId, limitConfig.getLimitType(), limitConfig.getLimitName());
	        if (rateLimit != null) {
	            String key = buildRateLimitKey(clientId, limitConfig.getLimitType(), limitConfig.getLimitName());
	            clientRateLimitsRepository.delete(rateLimit);
	            redisTemplate.delete(cachePrefix + key);
	            deletedAtLeastOne.set(true);
	            logger.debug("Deleted cache for key: {}", key);
	        }
	    });

	    return deletedAtLeastOne.get() ? new BaseResponse(Status.SUCCESS, "Rate limits deleted successfully.")
	            : new BaseResponse(Status.FAILURE, "No matching rate limits found.");
	}


	@Override
	@Transactional
	public BaseResponse removeClient(String clientId) {
	    ValidationUtil.validateClientId(clientId);

	    if (!isClientPresent(clientId)) {
	        return new BaseResponse(Status.FAILURE, "Client not found.");
	    }

	    List<ClientRateLimitData> rateLimits = clientRateLimitsRepository.findByClientId(clientId);
	    rateLimits.forEach(rateLimit -> {
	        String key = cachePrefix + buildRateLimitKey(rateLimit.getClientId(), rateLimit.getLimitType(), rateLimit.getLimitName());
	        redisTemplate.delete(key);
	    });

	    clientRateLimitsRepository.deleteAll(rateLimits);
	    clientRepository.deleteById(clientId);
	    logger.debug("Deleted all rate limits and cache for client: {}", clientId);

	    return new BaseResponse(Status.SUCCESS, "Client removed successfully.");
	}


	public boolean isClientPresent(String clientId) {
		return clientRepository.existsById(clientId);
	}

	@Override
	public BaseResponse addClient(String clientId) {
		if (isClientPresent(clientId)) {
			return new BaseResponse(Status.FAILURE, "Client already exists.");
		}
		clientRepository.save(new ClientData(clientId));
		logger.info("Client Successfully Added");
		return new BaseResponse(Status.SUCCESS, "Client added successfully.");
	}

	@Override
	public List<ClientRateLimitData> getAllRateLimits() {
		return (List<ClientRateLimitData>) clientRateLimitsRepository.findAll();
	}

	@Override
	public List<ClientRateLimitData> getClientLimits(String clientId) {
		ValidationUtil.validateClientId(clientId);
		return clientRateLimitsRepository.findByClientId(clientId);
	}

	
	//Updating ratelimit value in cache and db
	private void saveOrUpdateRateLimit(ClientRateLimitData rateLimit) {
	    String key = cachePrefix + buildRateLimitKey(rateLimit.getClientId(),rateLimit.getLimitType(),rateLimit.getLimitName());
	    clientRateLimitsRepository.save(rateLimit);
	    redisTemplate.opsForValue().set(key, rateLimit);
	    logger.debug("Updated cache for key: {}", key);
	}


	//Fetching all rate limits for client
	public List<ClientRateLimitData> fetchApplicableRateLimits(String clientId, String methodName, String apiName) {
	    List<ClientRateLimitData> rateLimits = new ArrayList<>();
	    
	    // Fetch and add limits
	    addIfNotNull(rateLimits, fetchDefaultRateLimit(clientId));
	    addIfNotNull(rateLimits, fetchMethodRateLimit(clientId, methodName));
	    addIfNotNull(rateLimits, fetchAPIRateLimit(clientId, apiName));

	    return rateLimits;
	}

	private void addIfNotNull(List<ClientRateLimitData> rateLimits, ClientRateLimitData limitData) {
	    if (limitData != null) {
	        rateLimits.add(limitData);
	    }
	}
    
	//Fetching default rate limit for client
	public ClientRateLimitData fetchDefaultRateLimit(String clientId) {
	    return getRateLimitData(clientId, LimitType.DEFAULT, RateLimitData.DEFAULT_LIMIT_NAME);
	}
    
	//Fetching method rate limit for client
	public ClientRateLimitData fetchMethodRateLimit(String clientId, String methodName) {
	    return getRateLimitData(clientId, LimitType.METHOD, methodName);
	}

	//Fetching API rate limit for client
	public ClientRateLimitData fetchAPIRateLimit(String clientId, String apiName) {
	    return getRateLimitData(clientId, LimitType.API, apiName);
	}

	//Getting rate limit data from cache if exist if not get it from database
	public ClientRateLimitData getRateLimitData(String clientId, LimitType limitType, String limitName) {
	    String key = buildRateLimitKey(clientId, limitType, limitName);
	    String redisKey = cachePrefix + key;

	    // Try to fetch from Redis
	    ClientRateLimitData cachedRateLimit = (ClientRateLimitData) redisTemplate.opsForValue().get(redisKey);
	    if (cachedRateLimit != null) {
	        logger.debug("Cache hit for key: {}", redisKey);
	        return cachedRateLimit;
	    }

	    // Cache miss, fetch from DB
	    ClientRateLimitData dbRateLimit = clientRateLimitsRepository.findById(key).orElse(null);
	    if (dbRateLimit != null) {
	        redisTemplate.opsForValue().set(redisKey, dbRateLimit);
	    }
	    return dbRateLimit;
	}

	
	//building ratelimit key for redis cache
	private String buildRateLimitKey(String clientId, LimitType limitType, String limitName) {
	    return clientId + ":" + limitType.name() + ":" + limitName;
	}
	

}
