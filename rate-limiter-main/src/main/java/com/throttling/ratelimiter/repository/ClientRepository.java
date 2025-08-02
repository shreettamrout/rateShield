package com.throttling.ratelimiter.repository;

import com.throttling.ratelimiter.pojo.model.ClientData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends CrudRepository<ClientData, String> {
	
}
