package com.vmc.memoryleak.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Queue;

@Aspect
@Component
public class RateLimitAspect {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);

    // Conditional flag for rate limiter, defaults to true
    @Value("${feature.rate-limiter.enabled:true}")
    private boolean isRateLimitingEnabled;

    private static final int MAX_REQUESTS = 3;
    private static final long WINDOW_SIZE_MS = 60000; // 1 minute in milliseconds
    private final Queue<Long> requestTimestamps = new LinkedList<>();

    @Around("execution(* com.vmc.memoryleak.controller.LeakController.triggerLeak(..))")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!isRateLimitingEnabled) {
            logger.info("Rate limiter is disabled via flag. Proceeding with request.");
            return joinPoint.proceed();
        }

        long currentTime = System.currentTimeMillis();

        synchronized (requestTimestamps) {
            // 1. Remove timestamps that are older than 1 minute
            while (!requestTimestamps.isEmpty() && currentTime - requestTimestamps.peek() >= WINDOW_SIZE_MS) {
                requestTimestamps.poll();
            }

            // 2. Check if we have hit the maximum allowed requests within the 1-minute window
            if (requestTimestamps.size() >= MAX_REQUESTS) {
                logger.warn("Rate limit exceeded for triggerLeak API. Denying request.");
                return "Rate limit exceeded! Please try again later. Maximum 3 requests allowed per minute.";
            }

            // 3. Allowance: Add current timestamp and proceed with the API call
            requestTimestamps.add(currentTime);
        }

        return joinPoint.proceed();
    }
}
