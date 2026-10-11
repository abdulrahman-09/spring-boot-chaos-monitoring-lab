# Shop

![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot 3.5.6](https://img.shields.io/badge/Spring%20Boot-3.5.6-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Docker Compose](https://img.shields.io/badge/Docker%20Compose-2496ED?logo=docker&logoColor=white)
![Prometheus](https://img.shields.io/badge/Prometheus-metrics-E6522C?logo=prometheus&logoColor=white)
![Grafana](https://img.shields.io/badge/Grafana-visualization-F46800?logo=grafana&logoColor=white)
![OpenTelemetry](https://img.shields.io/badge/OpenTelemetry-traces%20%26%20logs-7B61FF?logo=opentelemetry&logoColor=white)
![k6](https://img.shields.io/badge/k6-load%20testing-7D64FF?logo=k6&logoColor=white)

Shop is a small Spring Boot microservices project for learning monitoring, logging, tracing, load testing, and controlled failure diagnosis.

```text
client / k6
    |
    v
order-service :8080 ----HTTP----> payment-service :8082
    |                                  |
    v                                  v
PostgreSQL :5432                 OpenTelemetry Java agent
    |
    +-- Actuator + Micrometer --> Prometheus --> Grafana

OpenTelemetry agents --> Collector --> Tempo (traces) --> Grafana
                                   \-> Loki (logs)   --> Grafana
```

## Services and observability

- `order-service` validates and stores orders in PostgreSQL, then calls payment-service.
- `payment-service` is a stateless fake payment gateway that normally approves payments after a short delay.
- Actuator and Micrometer expose application, JVM, HTTP, and HikariCP metrics. Prometheus scrapes them and Grafana displays them.
- OpenTelemetry Java agents export traces and application logs to the OpenTelemetry Collector. The Collector sends traces to Tempo and logs to Loki.
- Tempo also produces trace-derived metrics for Grafana Traces Drilldown. `OTEL_METRICS_EXPORTER=none`, so Micrometer remains the source of application metrics.
- `k6/load.js` and `k6/stress.js` generate traffic for dashboards and incident drills.

Start the stack:

```bash
docker compose up -d --build
```

Open [Grafana](http://localhost:3000) (`admin` / `admin` for this local demo) or [Prometheus](http://localhost:9090). Use Grafana **Explore** to search Tempo traces and Loki logs.

Run the stress test:

```bash
docker compose --profile load run --rm k6 run /scripts/stress.js
```

## API endpoints

Chaos endpoints exist for learning only. They are active by default in this demo (`CHAOS_ENABLED=true`) and must not be exposed in production.

### order-service — `http://localhost:8080`

| Method and endpoint | Input | Result |
| --- | --- | --- |
| `POST /orders` | JSON: `customerId`, positive `amount` with up to two decimal places. | `201` with the stored order; `400` invalid input; `502` payment failure; `504` payment timeout. |
| `GET /orders/{id}` | Order UUID in the path. | `200` with the order, or `404`. |
| `GET /chaos` | None. | Current leaked-connection and held-memory state. |
| `POST /chaos/db-leak?connections=10&seconds=60` | Optional connection count and duration. | Holds database connections to exhaust the HikariCP pool. |
| `POST /chaos/memory?mb=100` | Optional megabytes to hold. | Allocates and retains heap memory. |
| `POST /chaos/reset` | None. | Releases held connections and memory. |
| `GET /livez` | None. | Liveness probe. |
| `GET /readyz` | None. | Readiness probe; includes the database check. |

### payment-service — `http://localhost:8082`

| Method and endpoint | Input | Result |
| --- | --- | --- |
| `POST /payments` | JSON: `orderId`, positive `amount`. Normally called by order-service. | `200` with `paymentId` and `status`; `400` invalid input; `500` when chaos errors are enabled. |
| `GET /chaos` | None. | Current latency, error-rate, and held-memory state. |
| `POST /chaos/latency?ms=2000` | Required delay in milliseconds. | Adds delay to every payment. |
| `POST /chaos/errors?rate=1.0` | Required failure fraction from `0.0` to `1.0`. | Makes that fraction of payments fail. |
| `POST /chaos/memory?mb=100` | Optional megabytes to hold. | Allocates and retains heap memory. |
| `POST /chaos/reset` | None. | Clears all payment chaos state. |
| `GET /livez` | None. | Liveness probe. |
| `GET /readyz` | None. | Readiness probe. |

### Management endpoints

Actuator is deliberately separated from the public APIs. Prometheus reaches these ports inside the Docker network; Compose does not publish them to the host.

| Service | Management port | Endpoints |
| --- | ---: | --- |
| order-service | `8081` | `GET /actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`, `/actuator/prometheus` |
| payment-service | `8083` | `GET /actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`, `/actuator/prometheus` |

## Failure drills

Use chaos endpoints while running k6, then inspect metrics, traces, and logs in Grafana.

- Payment latency above the order-service read timeout causes `504` responses.
- Payment errors cause payment-service `500` responses and order-service `502` responses.
- A database connection leak starves the order-service HikariCP pool.
- Repeated memory allocations increase heap usage and can restart a container after an out-of-memory error.
- Stopping payment-service tests a dependency outage; existing order reads can still work.
- Stopping PostgreSQL tests database failure and order-service readiness.

This is a learning project. The default credentials, database configuration, automatic schema updates, public API ports, and chaos endpoints are not production-safe defaults.
