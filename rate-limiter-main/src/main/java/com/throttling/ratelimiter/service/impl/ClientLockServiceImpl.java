package com.throttling.ratelimiter.service.impl;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.throttling.ratelimiter.service.ClientLockService;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;

@Service
public class ClientLockServiceImpl implements ClientLockService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientLockServiceImpl.class);


    @Autowired
    private RedissonClient redissonClient;

    @PostConstruct
    public void init() {
        // Optionally, you can log existing clients if needed
        LOGGER.info("ClientLockService initialized with Redis distributed locking.");
    }

    @Override
    public void acquireLock(String clientId) {
        RLock lock = redissonClient.getLock(clientId);
        lock.lock();
        LOGGER.info("Lock acquired for client: {}", clientId);
    }

    @Override
    public void releaseLock(String clientId) {
        RLock lock = redissonClient.getLock(clientId);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            LOGGER.info("Lock released for client: {}", clientId);
        } else {
            LOGGER.warn("Attempt to release a lock not held by the current thread for client: {}", clientId);
        }
    }

}
