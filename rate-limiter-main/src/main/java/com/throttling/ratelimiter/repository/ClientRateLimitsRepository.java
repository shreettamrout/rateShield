package com.throttling.ratelimiter.repository;

import com.throttling.ratelimiter.pojo.model.ClientRateLimitData;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ClientRateLimitsRepository extends CrudRepository<ClientRateLimitData, String> {

   
     //Fetches applicable rate limits for a given client, prioritizing custom and method-specific limits, and orders results by priority.
		/*
		 * @Query(""" FROM ClientRateLimitData crld WHERE crld.clientId = :clientId AND
		 * ( (crld.limitType = com.throttling.ratelimiter.enums.LimitType.API AND
		 * crld.limitName = :apiName) OR (crld.limitType =
		 * com.throttling.ratelimiter.enums.LimitType.METHOD AND crld.limitName =
		 * :methodName) OR (crld.limitType =
		 * com.throttling.ratelimiter.enums.LimitType.DEFAULT) ) ORDER BY crld.limitType
		 * ASC, crld.timeUnit """)
		 */
		/*
		 * List<ClientRateLimitData> fetchApplicableApiLimitsInOrder(String
		 * clientId,String methodName,String apiName) { ClientRateLimitData }
		 */

    //Finds a rate limit for a specific client, limit type, and limit name
	//ClientRateLimitData findByClientIdAndLimitTypeAndLimitName(String clientId,String limitType, String limitName);

    
    //ClientRateLimitData findByClientIdAndLimitTypeAndLimitName(String clientId, LimitType limitType, ApiMethod limitName);

    //Checks if a rate limit exists for the given client ID
    boolean existsByClientId(String clientId);

    //Retrieves all rate limits for a given client
    List<ClientRateLimitData> findByClientId(String clientId);
}
