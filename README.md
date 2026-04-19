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

---

## Flow Explanation Step-by-Step

Because the slider is set to a `WINDOW_SIZE` of **10**, your "current sliding window" is always exactly 10 units long, stretching backward from your current time. The **Window Logical Start** is the absolute earliest mathematical timestamp that is legally allowed to be inside your window. If any old request happens *before* this logical start, it is instantly evicted! *(Mathematically: `Current Time - 10 + 1`)*

**1. Time = 1, 3, 4**
Request ALLOWED for user u1 at 1,3,4 Window Logical Start: 0, Oldest Request in Window: 1
* **Window start:** `0`
* Your queue starts empty. At time `1`, `3`, and `4`, you make 3 requests. 
* All 3 requests are mathematically greater than the window start (`0`), so they are added to your queue: `[1, 3, 4]`.
* **Result: ALLOWED**

**2. Time = 6**
Request DENIED for user u1 at 6 (Rate limited), Window Logical Start: 0, Oldest Request in Window: 1
* **Window start:** `0` (Since `6 - 10 = -4`, it defaults to `0`)
* The oldest request in your list is from time `1`. This is still `>= 0`, so it is NOT evicted. 
* Your queue is `[1, 3, 4]`. You try to make a new request, but you have already hit the limit of 3 requests within the window. 
* **Result: DENIED**

**3. Time = 11**
Request DENIED for user u1 at 6 (Rate limited), Window Logical Start: 0, Oldest Request in Window: 1
* **Window start:** `2` (Because `11 - 10 + 1 = 2`)
* Because the new request is at `11`, the window shifts! *Any request older than time `2` is now expired.*
* The algorithm looks at your queue `[1, 3, 4]` and sees that the request at time `1` is older than `2`. It evicts the `1`.
* Your queue shrinks to `[3, 4]`.
* Because there are only 2 requests in the queue, you have room for a new one! Your new request `11` is added to the queue: `[3, 4, 11]`.
* **Result: ALLOWED**

**4. Time = 12**
Request DENIED for user u1 at 12 (Rate limited), Window Logical Start: 3, Oldest Request in Window: 3
* **Window start:** `3` (Because `12 - 10 + 1 = 3`)
* The algorithm checks the queue `[3, 4, 11]`. Are any requests older than `3`? No. The oldest request is exactly `3`, which is right on the boundary, so it survives!
* But, your queue is full (3 requests). Because no old requests were evicted, there is no room.
* **Result: DENIED**

**5. Time = 16**
Request ALLOWED for user u1 at 16, Window Logical Start: 7, Oldest Request in Window: 11
* **Window start:** `7` (Because `16 - 10 + 1 = 7`)
* The window has shifted forward again. *Any request older than `7` is expired.*
* The algorithm checks your queue `[3, 4, 11]`. It sees that `3` and `4` are older than `7`. It evicts both!
* Your queue shrinks drastically to just `[11]`.
* You have plenty of room, so the request at `16` is added: `[11, 16]`.
* **Result: ALLOWED**

**6. Time = 19**
Request ALLOWED for user u1 at 19, Window Logical Start: 10, Oldest Request in Window: 11
* **Window start:** `10` (Because `19 - 10 + 1 = 10`)
* Your queue is `[11, 16]`. The oldest request is `11`, which is not older than `10`. None are evicted.
* The queue has 2 items, meaning there is exactly enough room for the third one! It is added: `[11, 16, 19]`. 
* **Result: ALLOWED**
