# Shop

![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot 3.5.6](https://img.shields.io/badge/Spring%20Boot-3.5.6-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Docker Compose](https://img.shields.io/badge/Docker%20Compose-2496ED?logo=docker&logoColor=white)
![Prometheus](https://img.shields.io/badge/Prometheus-monitoring-E6522C?logo=prometheus&logoColor=white)
![Grafana](https://img.shields.io/badge/Grafana-dashboards-F46800?logo=grafana&logoColor=white)
![k6](https://img.shields.io/badge/k6-load%20testing-7D64FF?logo=k6&logoColor=white)
![OpenTelemetry](https://img.shields.io/badge/OpenTelemetry-planned-7B61FF?logo=opentelemetry&logoColor=white)

Shop is a small Spring Boot microservices project for learning monitoring and observability.

```text
curl / k6
    |
    v
order-service :8080 ----HTTP----> payment-service :8082
    |
    v
PostgreSQL :5432

Prometheus scrapes application metrics
Grafana displays dashboards and trends
```

## Application

The project contains two independent Maven services:

- `order-service` is the public API. It validates and stores orders in PostgreSQL, calls payment-service, and marks orders `PAID` or `PAYMENT_FAILED`.
- `payment-service` is a stateless fake payment gateway. It simulates payment work and normally approves valid payments.

The project uses Java 21, Spring Boot 3.5.6, PostgreSQL 16, Maven, and Docker Compose.

## Monitoring purpose

The project is designed to show how to observe a distributed application while it is working and while it is failing.

Prometheus collects application metrics such as:

- HTTP request rate and status codes
- Request duration and percentiles
- JVM heap and garbage collection
- HikariCP database connection-pool usage
- Business outcomes such as paid and failed orders

Grafana is used to visualize these metrics in dashboards. The dashboards help answer questions such as:

- Are requests failing or becoming slower?
- Which service or endpoint is affected?
- Is payment-service slower than order-service expects?
- Is the database connection pool saturated?
- Is JVM memory increasing?
- Are failed orders caused by payment errors, timeouts, or infrastructure problems?

The `k6` scripts generate normal traffic and ramping stress so that changes in latency, errors, resource usage, and throughput can be observed over time.

## Chaos testing

The services include local-only failure switches. They deliberately create conditions that monitoring should reveal:

- Payment latency can make order-service return `504` timeouts.
- Injected payment errors make payment-service return `500` and order-service return `502`.
- A database connection leak can exhaust the order-service HikariCP pool.
- Repeated memory allocation can increase heap and garbage-collection activity and eventually restart a container.
- Stopping payment-service tests dependency failure while existing order reads may still work.
- Stopping PostgreSQL tests database failure and order-service readiness.

The purpose is not to make the application reliable by itself. The purpose is to practice the complete monitoring cycle:

```text
generate traffic
    -> create a controlled failure
    -> observe metrics in Grafana
    -> identify the affected service and resource
    -> confirm the behavior in application logs
    -> reset the failure and verify recovery
```

This is a learning project. The demo credentials, automatic database schema updates, and chaos endpoints must not be used as-is in production.
