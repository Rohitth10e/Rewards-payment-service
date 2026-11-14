# 01 — Spring Boot and HTTP foundations

This note explains the Spring Boot and HTTP concepts used by the rewards payment service. It assumes basic Java knowledge and builds from application startup to request validation and tests.

## 1. What Spring Boot provides

Spring Boot is an opinionated way to build Spring applications. It provides:

- automatic configuration based on the classpath;
- a managed application context;
- embedded web-server support;
- dependency injection;
- external configuration; and
- production-friendly conventions.

The project starts at `RewardsPaymentServiceApplication`, which is annotated with `@SpringBootApplication`. That annotation combines component scanning, auto-configuration, and Spring configuration.

Run the application with:

```bash
./mvnw spring-boot:run
```

## 2. Dependencies become capabilities

The `pom.xml` selects the application capabilities:

- `spring-boot-starter-webmvc` enables HTTP controllers;
- `spring-boot-starter-validation` enables Jakarta Bean Validation;
- `spring-boot-starter-data-jpa` enables JPA and repositories;
- `spring-boot-starter-kafka` enables Kafka integration; and
- the Flyway starters manage database migrations.

Starters are curated dependency bundles. They reduce manual version management and let Boot configure common infrastructure automatically.

## 3. Dependency injection

Spring creates objects called beans and supplies their dependencies. For example, `TransactionController` receives a `TransactionProducer` through its constructor:

```java
public TransactionController(TransactionProducer producer) {
    this.producer = producer;
}
```

Constructor injection is preferred because dependencies are explicit, required dependencies can be `final`, and the class is easier to unit test.

`@RestController` marks the class as an HTTP controller whose return values are written to the response. `@Service` marks application services such as the producer and consumer.

## 4. The HTTP boundary

The controller maps:

```text
POST /transactions
```

The `@RequestBody` contains JSON and `@RequestHeader("Idempotency-Key")` reads the request header. `ResponseEntity.accepted()` returns HTTP `202`, which communicates that work has been accepted but is not necessarily complete.

This distinction matters:

- `200 OK` commonly means the operation completed and a representation is available.
- `201 Created` commonly means a resource was created.
- `202 Accepted` means asynchronous processing has been accepted.
- `400 Bad Request` means the request is invalid.

## 5. DTOs and validation

`TransactionRequest` is a Java record:

```java
public record TransactionRequest(
    @NotBlank String accountId,
    @NotNull @DecimalMin("0.01") BigDecimal amount
) {}
```

Records are useful for immutable request data. `@Valid` on the controller parameter activates validation. Validation belongs at the boundary because invalid input should be rejected before it reaches Kafka or the database.

Validation is not business authorization. A future service layer should also decide whether an account exists, whether the caller can use it, and whether sufficient balance remains.

## 6. Why use `BigDecimal` for money?

Binary floating-point types such as `double` cannot represent many decimal fractions exactly. `BigDecimal` is appropriate for monetary values when combined with:

- a defined scale;
- a currency;
- explicit rounding rules; and
- database `DECIMAL` columns.

The current request validates a positive amount but does not yet include currency or a balance check.

## 7. Configuration

Spring reads YAML configuration from `src/main/resources/application.yml`. Configuration separates environment-specific values from Java code:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rewards_db
```

For production, credentials should come from environment variables or a secret manager rather than committed defaults.

## 8. Testing the web layer

`@WebMvcTest(TransactionController.class)` loads the MVC slice without starting the entire application. `MockMvc` sends an in-process HTTP request, and `@MockitoBean` replaces the producer dependency.

The existing tests verify:

- a valid request returns `202`;
- a negative amount returns `400`.

Good next tests should cover missing headers, blank account IDs, missing amounts, malformed JSON, and producer invocation with the expected idempotency key.

## 9. A useful mental model

```text
HTTP request
  -> controller binding
  -> validation
  -> application service
  -> infrastructure adapter (Kafka/database)
  -> HTTP response
```

Keep the controller thin. It should translate HTTP concepts into application input, not contain database queries, Kafka retry policy, or complex business rules.

## 10. Common beginner mistakes

- Returning `200` for an operation that is still asynchronous.
- Trusting client validation without server-side validation.
- Putting secrets in source control.
- Using field injection, which hides required dependencies.
- Returning internal entities directly from public APIs.
- Treating a successful Kafka send request as proof that business processing completed.
- Catching every exception and returning a success response.

## Suggested practice exercise

Add a typed response containing a correlation ID, then implement `GET /transactions/{id}`. This makes the asynchronous contract observable instead of asking clients to infer completion from logs.
