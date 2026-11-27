# payment-service

Microservice for managing payments.

## Tech Stack

- Java 21, Spring Boot 4
- MongoDB, Liquibase (liquibase-mongodb extension)
- MapStruct, Lombok
- Spring Data MongoDB (Criteria-based custom repository)
- OpenFeign, Resilience4j (CircuitBreaker)
- Spring Security (OAuth2 Resource Server, RS256 + JWKS)
- Spring Kafka (producer)
- JUnit 5, Mockito, Testcontainers (MongoDB), WireMock
- Docker, GitHub Actions

## Features

- CRUD for Payment with pagination and filtering by userId/orderId/status
- Total sum of payments for a date range (current user and all users for ADMIN)
- Random number API decides the payment status (SUCCESS/FAILED)
- Publishes a `CREATE_PAYMENT` event to Kafka for `order-service`
- JWT authorization: USER - own payments, ADMIN - all
- Global exception handling
- Unit + integration tests
- CI with build, test, optional Sonar analysis and Docker image

## Run with Docker

docker compose up --build
