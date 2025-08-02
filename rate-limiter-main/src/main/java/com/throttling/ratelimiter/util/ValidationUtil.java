package com.throttling.ratelimiter.util;

public class ValidationUtil {
	
	public static void validateClientId(String clientId) {
		if (clientId == null || clientId.trim().isEmpty()) {
			throw new IllegalArgumentException("Client ID must not be null or empty");
		}
	}
}
