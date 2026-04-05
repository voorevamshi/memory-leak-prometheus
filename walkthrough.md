# Spring Boot Observability & Memory Leak Walkthrough

This document summarizes the entire flow we implemented to set up an Observability Stack, artificially simulate a memory leak, and track it live using Prometheus and Grafana.

---

## 1. The Application: Simulating the Memory Leak
We started by creating a scenario where the JVM runs out of memory due to **Unintentional Object Retention**.

### `LeakController.java`
We built a standard REST controller containing a static array:
```java
private static final List<String> customCache = new ArrayList<>();
```
Because the `customCache` list is `static` and never cleared, every time you call `http://localhost:8080/api/leak`, it injects 10,000 UUID strings into the list. The Garbage Collector cannot prune these strings because the application retains a constant active reference to them.

---

## 2. Spring Boot Actuator Configuration
To expose our application's metrics to Prometheus, we had to fix an override bug in `application.properties`. We merged the settings into a single, comprehensive command:

```properties
management.endpoints.web.exposure.include=heapdump,health,info,metrics,prometheus
```
* **`/actuator/prometheus`**: Exposes all JVM metrics in the exact text format Prometheus requires.
* **`/actuator/heapdump`**: Allows you to download an `.hprof` file right before the application crashes, so you can trace the exact static reference holding the memory hostage.

---

## 3. Infrastructure: Dockerizing the Observability Stack
Instead of manually downloading and running Prometheus and Grafana binaries, we automated the infrastructure using Docker Compose.

1. **`prometheus.yml`**: By configuring the scrape target to `host.docker.internal:8080`, we instructed the Dockerized Prometheus container on how to securely connect backward into our Windows host machine where Spring Boot was running.
2. **`docker-compose.yml`**: This file launches both Prometheus (`9090`) and Grafana (`3000`).
3. **The `localhost` Network Trap**: We solved a common issue where Grafana threw a `Connection Refused` error. Because Grafana is isolated inside a container, `localhost` means *the Grafana container itself*. We solved this by securely pointing Grafana's Data Source directly to the internal Docker DNS name: `http://prometheus:9090`.

---

## 4. PromQL Deep Dive (Prometheus Query Language)
Understanding how to query the raw metrics is the most critical part of setting up dashboards.

### Query 1: The Raw JVM Metric
```promql
jvm_memory_used_bytes{area="heap"}
```
If you run this in Prometheus's **Table** view, you noticed it returns *three separate rows*:
1. `G1 Eden Space`
2. `G1 Old Gen`
3. `G1 Survivor Space`

This is highly accurate, but it makes creating a simple line-graph difficult because it draws three disconnected lines instead of your total application memory.

### Query 2: The Aggregated Graph Query (The Best Choice)
To properly watch the memory leak grow, we aggregated those separate JVM spaces together:
```promql
sum(jvm_memory_used_bytes{area="heap"}) / 1024 / 1024
```
**Breakdown of the syntax:**
* **`jvm_memory_used_bytes{area="heap"}`**: Grabs the raw byte count of the heap.
* **`sum(...)`**: Forces Prometheus to add Eden Space + Old Gen + Survivor Space together perfectly into one combined number.
* **`/ 1024 / 1024`**: Converts the number from raw bytes into Megabytes (MB), moving the Y-Axis into human-readable numbers like "25MB" instead of "26214400".

> [!WARNING]
> **Reporting `{}` / Empty Data Pitfalls**
> If your query ever returns `{}` (No Results), check the following:
> 1. Always verify `http://localhost:9090/targets` says **UP**. If it's DOWN, Spring Boot isn't running or the connection is blocked.
> 2. Ensure your time window (top right corner of Grafana/Prometheus) is set to a short interval like **"Last 15 minutes"**. If you select "Last 7 Days" but only started the app 2 minutes ago, the graph lines will be invisible.
> 3. If you use Grafana manually, you must select the **Code** toggle rather than the "Builder" UI to safely paste strings like `sum(...)`.

---

## 5. Grafana Auto-Provisioning (Configuration as Code)
To remove the burden of figuring out the Grafana UI, we injected the setup automatically using volume mounts:
* **`datasource.yml`**: Automatically logs Grafana into `http://prometheus:9090` without you needing to test the connection.
* **`memory-leak.json`**: An architectural design file that created the "JVM Memory Leak Dashboard" inside Grafana immediately on boot!
