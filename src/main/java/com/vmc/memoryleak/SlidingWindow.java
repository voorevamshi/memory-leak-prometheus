package com.vmc.memoryleak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class SlidingWindow {

	private static final Logger logger = LoggerFactory.getLogger(SlidingWindow.class);

	private static final int WINDOW_SIZE = 10;
	private static final int MAX_REQUESTS = 3;
	
	// Stores the history of request timestamps for each user
	private static final Map<String, List<Integer>> userRequestHistory = new LinkedHashMap<>();

	public static void main(String[] args) {
		simulateRequests();
	}

	/**
	 * Simulates incoming user requests to test the sliding window rate limiter.
	 */
	public static void simulateRequests() {
		List<User> mockRequests = new ArrayList<>();
		mockRequests.add(new User("user1", 1));
		mockRequests.add(new User("user1", 3));
		mockRequests.add(new User("user1", 4));
		mockRequests.add(new User("user1", 6));
		mockRequests.add(new User("user1", 11));
		mockRequests.add(new User("user1", 12));
		mockRequests.add(new User("user1", 16));
		mockRequests.add(new User("user1", 19));
		
		logger.info("Starting simulation with requests: {}", mockRequests);
		
		for (User request : mockRequests) {
			logger.info("Processing Request: {}", request);
			processRequest(request);
		}
	}

	/**
	 * Rate limits incoming requests using a sliding window algorithm.
	 */
	public static void processRequest(User request) {
		String userId = request.getUserId();
		int requestTime = request.getTime();

		userRequestHistory.putIfAbsent(userId, new LinkedList<>());
		List<Integer> requestTimestamps = userRequestHistory.get(userId);

		// 1. Remove outdated request timestamps that fall outside the current sliding window
		while (!requestTimestamps.isEmpty() && requestTime - requestTimestamps.get(0) >= WINDOW_SIZE) {
			requestTimestamps.remove(0);
		}

		// Calculate the logical starting boundary of the current sliding window
		int windowLogicalStart = Math.max(0, requestTime - WINDOW_SIZE + 1);
		
		// Determine the oldest request currently present in the window
		int oldestRequestInWindow = requestTimestamps.isEmpty() ? requestTime : requestTimestamps.get(0);

		// 2. Allow the request if the max limit within the window has not been reached
		if (requestTimestamps.size() < MAX_REQUESTS) {
			requestTimestamps.add(requestTime);
			logger.info("Request ALLOWED for user {} at {}, Window Logical Start: {}, Oldest Valid Request: {}",
					userId, requestTime, windowLogicalStart, oldestRequestInWindow);
		} else {
			logger.warn("Request DENIED for user {} at {} (Rate limited), Window Logical Start: {}, Oldest Valid Request: {}",
					userId, requestTime, windowLogicalStart, oldestRequestInWindow);
		}

		userRequestHistory.put(userId, requestTimestamps);
	}

}