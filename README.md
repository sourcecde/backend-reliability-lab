# Backend Reliability Lab

A hands-on Kotlin and Spring Boot project for exploring backend reliability through practical experiments with PostgreSQL.

## Goals

- Understand idempotency and duplicate request processing.
- Reproduce race conditions under concurrent requests.
- Explore transaction boundaries and database constraints.
- Verify behavior with integration and concurrency tests.

## Tech Stack

- Kotlin 2.2.21, Java 21, Spring Boot 4.0.8
- Spring Data JPA / Hibernate
- PostgreSQL 17 and Docker Compose
- Flyway for schema migrations
- Springdoc OpenAPI / Swagger UI
- Gradle, JUnit 5, Testcontainers

## Current Implementation

The first milestone is a baseline Order CRUD API. The database uses a numeric primary key (`id`) and a separate unique order UUID (`uuid`). Products are identified by UUID.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/order` | List orders |
| GET | `/api/order/{orderId}` | Get an order by numeric ID |
| GET | `/api/order/product/{productId}` | List orders for a product UUID |
| POST | `/api/order/create` | Create an order |
| PUT | `/api/order/update/{orderId}` | Update an order |
| DELETE | `/api/order/delete/{orderId}` | Delete an order |

**Implementation note:** Path parameter binding and not-found HTTP responses still need to be finalized; the table describes the declared routes, not a fully verified API contract.

### Example create request

```http
POST /api/order/create
Content-Type: application/json
```

```json
{
  "productId": "550e8400-e29b-41d4-a716-446655440000",
  "quantity": 2
}
```

Example response shape (UUID values are illustrative):

```json
{
  "uuid": "7b6f74ac-1bb1-4b8d-8b8b-ec34c8ef7280",
  "productId": "550e8400-e29b-41d4-a716-446655440000",
  "quantity": 2
}
```

## Getting Started

### Prerequisites

- JDK 21
- Docker and Docker Compose
- Git

### Clone

```bash
git clone https://github.com/sourcecde/backend-reliability-lab.git
cd backend-reliability-lab
```

To work with the CRUD feature branch before it is merged:

```bash
git checkout feature/order-crud-api
```

### Start PostgreSQL

```bash
docker compose up -d
```

The local development database is available at `localhost:5432`, with database name `reliability_lab` and username `reliability`. Development credentials are defined in `compose.yaml`; do not use them in production.

### Start the application

```bash
./gradlew bootRun
```

Flyway applies the SQL migration from `src/main/resources/db/migration/` when the application starts.

### API documentation

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

### Run tests

```bash
./gradlew test
```

Test dependencies are configured; comprehensive integration and concurrency coverage is planned.

## Experiment 1: Idempotency Under Concurrency

**Problem:** A client retries an order-creation request after a timeout. If the original request succeeded, the retry may create a duplicate order. Concurrent requests can also race when they check for an existing operation before inserting.

**Planned experiment:**

1. Establish and verify the baseline Order API.
2. Add an `Idempotency-Key` header to order creation.
3. Demonstrate why a naive check-then-insert implementation is unsafe.
4. Reproduce the race with concurrent requests.
5. Add database-backed concurrency protection.
6. Verify that retries return the same logical result without creating duplicates.

Idempotency is **not yet implemented**.

## Roadmap

- [x] Initialize Kotlin / Spring Boot project
- [x] Configure PostgreSQL via Docker Compose
- [x] Add Flyway migration for orders
- [x] Implement baseline CRUD methods and DTOs
- [x] Add Swagger/OpenAPI dependency
- [ ] Finalize path variable binding, validation, and HTTP error handling
- [ ] Remove random UUID fallbacks from persisted-entity response mapping
- [ ] Add PostgreSQL integration tests with Testcontainers
- [ ] Implement naive idempotency and reproduce its race condition
- [ ] Implement concurrency-safe idempotency and verify it under load
- [ ] Document findings from the experiments

## License

No license has been specified yet.
