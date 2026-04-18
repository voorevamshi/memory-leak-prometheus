# Memory Leak & Prometheus Monitoring + Sliding Window Rate Limiter

This project demonstrates two main components:
1. **Memory Leak Simulation & Monitoring**: Tools and classes to simulate a memory leak and monitor heap utilization using a Prometheus & Grafana stack.
2. **Sliding Window Rate Limiter**: A simple, log-driven Java implementation of the Sliding Window Log rate-limiting algorithm.

---

## Sliding Window Rate Limiter

The file `SlidingWindow.java` contains a basic simulation of a Sliding Window Log rate limiter. It strictly limits the number of permitted requests a specific user can make within a defined chronological time window.

### How it Works
- Each user possesses their own simulated timeline stream of requests.
- The algorithm defines a `WINDOW_SIZE` (e.g., 10 time units) and `MAX_REQUESTS` threshold (e.g., 3 requests per window).
- When a new request arrives at `currentTime`, the algorithm:
  1. Checks the user's request history.
  2. **Evicts outdated requests**: It removes any older request timestamps that fall outside the bounds of `[currentTime - WINDOW_SIZE + 1, currentTime]`.
  3. **Checks Threshold**: If the remaining request list is below `MAX_REQUESTS`, the request is **ALLOWED** and added to the user's history map.
  4. If the threshold has already been met by existing valid window timestamps, the incoming request is **DENIED**.

### Simulation Components

**1. `User.java`**
A plain old Java object (POJO) representing an incoming user request. It holds:
- `userId`: Identifier to map requests to specific users.
- `time`: A logical chronological timestamp representing when the request was made.

**2. `SlidingWindow.java`**
Contains the core execution and rate limiter logic:
- `userRequestHistory`: A global `LinkedHashMap` simulating an in-memory datastore where keys are User IDs and values are `List<Integer>` representing successful request timestamps.
- **`simulateRequests()`**: Initializes mock requests and pushes them through to be evaluated.
- **`processRequest(User request)`**: Triggers the sliding window logic and securely adds requests if permitted. Logging exposes the logic behavior in real-time.

### Logging Explanations
When you execute the `SlidingWindow` class, SLF4J logger outputs provide insight:

- `Window Logical Start`: Representing the exact theoretical baseline of the active window (`requestTime - WINDOW_SIZE + 1`).
- `Oldest Valid Request`: Represents the earliest actual request timestamp remaining that blocks further incoming allowances until it is evicted.