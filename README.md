# Cloud Gateway

[**← Back to Main Architecture**](https://github.com/oleh-prukhnytskyi/macro-tracker)

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-Gateway-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Redis](https://img.shields.io/badge/redis-%23DD0031.svg?style=for-the-badge&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)

---

[![License](https://img.shields.io/badge/license-Apache%202.0-blue?style=for-the-badge)](LICENSE)
[![Docker Hub](https://img.shields.io/badge/Docker%20Hub-Image-blue?style=for-the-badge&logo=docker)](https://hub.docker.com/repository/docker/olehprukhnytskyi/macro-tracker-cloud-gateway/general)

**The Edge Server and Entry Point for the Macro Tracker Platform.**

This service handles all incoming traffic, providing dynamic routing, security (OAuth2 Resource Server), central API documentation, and sophisticated rate limiting strategies.

## :zap: Service Specifics

* **OAuth2 Resource Server**: Validates JWT signatures using the Public Key exposed by the **User Service** (JWKS). It enforces authentication for all endpoints except public ones (auth, swagger).
* **Header Propagation**: Extracts the `sub` (User ID) from the JWT claims and injects it as an `X-User-Id` header into downstream requests, simplifying user identification for internal services.
* **Smart Rate Limiting**: Implements a **Hybrid Key Resolver**:
    * *Authenticated Users*: Limits are applied per **User ID** (ensuring fair usage).
    * *Anonymous Users*: Limits are applied per **IP Address** (preventing abuse on public endpoints).
* **Swagger Aggregation**: Acts as a central documentation hub, pulling OpenAPI definitions from all downstream microservices into a single UI.
* **Distributed Tracing**: Automatically injects Trace IDs and Span IDs into logs and headers for full observability across the cluster.

---

## :electric_plug: API & Routing

The Gateway manages routing for the following domains:

* `/api/auth/**`, `/api/users/**` -> **User Service**
* `/api/goals/**` -> **Goal Service**
* `/api/foods/**` -> **Food Service**
* `/api/intake/**` -> **Intake Service**
* `/api/dashboard/**` -> **BFF Service**

**Note**: All routes are protected by a global **CORS** configuration allowing requests only from the trusted client domain.

---

## :hammer_and_wrench: Tech Details

| Component          | Implementation                                              |
|:-------------------|:------------------------------------------------------------|
| **Core Framework** | Spring Cloud Gateway (Reactive)                             |
| **Security**       | Spring Security OAuth2 Resource Server (Nimbus JWT)         |
| **Rate Limiting**  | Spring Cloud Gateway RequestRateLimiter (Redis Lua Scripts) |
| **Observability**  | OpenTelemetry, Logstash Logback Encoder (JSON logs)         |
| **Documentation**  | SpringDoc OpenAPI (Aggregator)                              |

---

## :gear: Environment Variables

Required variables for `local` or `k8s` deployment:

| Variable                  | Purpose                                                             |
|:--------------------------|:--------------------------------------------------------------------|
| **Services URLs**         |                                                                     |
| `USER_SERVICE_URL`        | URL for User Service (Auth & JWKS).                                 |
| `GOAL_SERVICE_URL`        | URL for Goal Service.                                               |
| `FOOD_SERVICE_URL`        | URL for Food Service.                                               |
| `INTAKE_SERVICE_URL`      | URL for Intake Service.                                             |
| `BFF_SERVICE_URL`         | URL for BFF Service.                                                |
| **Infrastructure**        |                                                                     |
| `CLOUD_GATEWAY_REDIS_URL` | Redis connection URL for Rate Limiter (e.g., `redis://redis:6379`). |
| `MACRO_TRACKER_URL`       | Allowed Origin URL for CORS (e.g., `https://macrotracker.uk`).      |
| `CLOUD_GATEWAY_URL`       | The external URL of the gateway (used for Swagger config).          |

---

## :whale: Quick Start

```bash
# Pull from Docker Hub
docker pull olehprukhnytskyi/macro-tracker-cloud-gateway:latest

# Run (Ensure your .env file contains all required variables listed above)
docker run -p 8080:8080 --env-file .env olehprukhnytskyi/macro-tracker-cloud-gateway:latest
```

---

## :balance_scale: License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.