# KCB Spring Boot Technical Assessment

> **Bank Masking Spring Boot Starter** + **Bank Books API Demo**

A production-grade implementation of a reusable Spring Boot Starter library for masking sensitive data in logs, demonstrated via a full CRUD REST API.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Design Decisions](#design-decisions)
3. [Project Structure](#project-structure)
4. [How to Run Locally](#how-to-run-locally)
5. [How to Run with Docker](#how-to-run-with-docker)
6. [How to Run Tests](#how-to-run-tests)
7. [Code Coverage](#code-coverage)
8. [API Documentation](#api-documentation)
9. [Configuration Reference](#configuration-reference)
10. [Masking Behavior Explained](#masking-behavior-explained)
11. [Assumptions Made](#assumptions-made)
12. [Performance Considerations](#performance-considerations)
13. [Bonus Features](#bonus-features)

---

## Architecture Overview

```
kcb-parent (pom.xml)
├── bank-masking-spring-boot-starter/    ← Reusable library
│   ├── annotation/
│   │   ├── @Mask                        ← Field/param-level masking annotation
│   │   ├── @LogMasked                   ← Method-level AOP masking trigger
│   │   └── MaskStyle (FULL|PARTIAL|LAST4)
│   ├── config/
│   │   ├── MaskingProperties            ← @ConfigurationProperties (p11.masking.*)
│   │   └── MaskingAutoConfiguration    ← Spring Boot auto-configuration
│   ├── service/
│   │   ├── MaskingService               ← Core string-level masking logic
│   │   └── ObjectMaskingService         ← Deep object graph traversal + masking
│   ├── aop/
│   │   └── MaskingLoggingAspect         ← AOP: auto-mask @LogMasked methods
│   ├── serializer/
│   │   ├── MaskingSerializer            ← Jackson contextual serializer
│   │   └── MaskingModule                ← Jackson module registration
│   └── wrapper/
│       └── MaskedObject                 ← Lazy-evaluated masked log wrapper
│
└── bank-books-api-demo/                 ← Consumer application
    ├── entity/Book                      ← JPA entity
    ├── dto/BookRequestDto               ← Request DTO (validated)
    ├── dto/BookResponseDto              ← Response DTO
    ├── mapper/BookMapper                ← Entity ↔ DTO mapping
    ├── repository/BookRepository        ← Spring Data JPA
    ├── service/BookService              ← Business logic (uses masking)
    ├── controller/BookController        ← REST endpoints (thin layer)
    └── exception/GlobalExceptionHandler ← RFC 9457 ProblemDetail errors
```

### Request Flow

```
HTTP Request
    ↓
BookController  (thin – HTTP semantics only)
    ↓
BookService     (@LogMasked triggers AOP, MaskedObject used in log statements)
    ↓                         ↓
BookRepository          MaskingLoggingAspect  → masked logs
    ↓                   ObjectMaskingService  → masked JSON string
H2 Database (unmasked)
    ↓
BookResponseDto (unmasked – returned to client)
```

---

## Design Decisions

### 1. Auto-Configuration with `matchIfMissing=true`
The starter activates automatically when added to the classpath. No explicit `@EnableMasking` annotation is needed. Consumers can disable it via `p11.masking.enabled=false`.

### 2. Two-Layer Masking Strategy
| Layer | Mechanism | When to Use |
|-------|-----------|-------------|
| **Config-driven** | Field names in `p11.masking.fields` | Consistent cross-application masking |
| **Annotation-driven** | `@Mask` on fields/params | Per-field style overrides |

### 3. Immutability Guarantee
`ObjectMaskingService` **never modifies original objects**. It uses reflection to _read_ field values and builds a `Map<String, Object>` representation. The original DTO passed to `save()` is always unmodified.

### 4. Lazy Evaluation via `MaskedObject`
```java
log.info("Creating book: {}", MaskedObject.of(dto, objectMaskingService));
```
`MaskedObject.toString()` is only called if the log level is enabled. This avoids expensive reflection when `INFO` logging is disabled in production.

### 5. AOP for Cross-Cutting Concerns
`@LogMasked` on service methods triggers `MaskingLoggingAspect` to automatically log masked method arguments. This keeps service methods clean of repetitive masking boilerplate.

### 6. SOLID Principles Applied
- **SRP**: Each class has one responsibility (masking logic, AOP interception, auto-config, etc.)
- **OCP**: New masking styles can be added to the `MaskStyle` enum without modifying existing code
- **DIP**: The consumer app depends on abstractions from the starter, not concrete implementations
- **LSP/ISP**: Services are focused interfaces, not fat classes

### 7. Masking Style Design
| Style | Algorithm |
|-------|-----------|
| `FULL` | `"*".repeat(value.length())` |
| `PARTIAL` | Smart: email → keep prefix+domain; phone → keep first/last 3 digits; generic → keep ¼ prefix + ¼ suffix |
| `LAST4` | Show last 4 characters only (ideal for credit cards) |

---

## Project Structure

```
Kcb/
├── pom.xml                                 ← Parent POM (multi-module)
├── Dockerfile                              ← Multi-stage Docker build
├── docker-compose.yml                      ← Docker Compose
├── README.md                               ← This file
│
├── bank-masking-spring-boot-starter/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/kcb/masking/
│       │   ├── annotation/
│       │   ├── aop/
│       │   ├── config/
│       │   ├── serializer/
│       │   ├── service/
│       │   └── wrapper/
│       ├── main/resources/
│       │   └── META-INF/spring/
│       │       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│       └── test/java/com/kcb/masking/service/
│           ├── MaskingServiceTest.java
│           └── ObjectMaskingServiceTest.java
│
└── bank-books-api-demo/
    ├── pom.xml
    └── src/
        ├── main/java/com/kcb/books/
        │   ├── BankBooksApiApplication.java
        │   ├── config/OpenApiConfig.java
        │   ├── controller/BookController.java
        │   ├── dto/
        │   ├── entity/Book.java
        │   ├── exception/
        │   ├── mapper/BookMapper.java
        │   ├── repository/BookRepository.java
        │   └── service/BookService.java
        ├── main/resources/application.yaml
        └── test/java/com/kcb/books/
            ├── service/BookServiceTest.java
            └── integration/BookIntegrationTest.java
```

---

## How to Run Locally

### Prerequisites
- **Java 17+** (`java -version`)
- **Maven 3.8+** (`mvn -version`)
- No external database required (H2 in-memory)

### Steps

```bash
# 1. Clone the repository
git clone <repository-url>
cd Kcb

# 2. Build and install the starter library first
mvn install -pl bank-masking-spring-boot-starter

# 3. Build and run the demo application
mvn spring-boot:run -pl bank-books-api-demo
```

The application starts at **http://localhost:8080**

| URL | Description |
|-----|-------------|
| http://localhost:8080/books | REST API base URL |
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/api-docs | OpenAPI JSON |
| http://localhost:8080/h2-console | H2 database console |

### Quick API Test with curl

```bash
# Create a book
curl -X POST http://localhost:8080/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Clean Code",
    "author": "Robert C. Martin",
    "email": "robert@example.com",
    "phoneNumber": "0712345678",
    "publisher": "Prentice Hall"
  }'

# Get all books
curl http://localhost:8080/books

# Get book by ID
curl http://localhost:8080/books/1

# Update a book
curl -X PUT http://localhost:8080/books/1 \
  -H "Content-Type: application/json" \
  -d '{"title": "Clean Architecture", "author": "Robert C. Martin", "email": "bob@example.com", "phoneNumber": "0799999999"}'

# Delete a book
curl -X DELETE http://localhost:8080/books/1
```

### Observing Masking in Logs

When you create a book, the application logs will show:

```
INFO  BookService - Creating book: {"title":"Clean Code","author":"Robert C. Martin","email":"ro***@example.com","phoneNumber":"071****678","publisher":"Prentice Hall"}
```

While the API response (and database) retain full unmasked values:
```json
{
  "email": "robert@example.com",
  "phoneNumber": "0712345678"
}
```

---

## How to Run with Docker

### Option 1: Docker Compose (Recommended)

```bash
# Build and start
docker-compose up --build

# Stop
docker-compose down
```

### Option 2: Manual Docker Build

```bash
# Build the image
docker build -t bank-books-api:1.0.0 .

# Run the container
docker run -p 8080:8080 --name bank-books-api bank-books-api:1.0.0
```

---

## How to Run Tests

### Run All Tests

```bash
# From project root
mvn test
```

### Run Tests for a Specific Module

```bash
# Starter library tests only
mvn test -pl bank-masking-spring-boot-starter

# Demo application tests only
mvn test -pl bank-books-api-demo
```

### Generate Coverage Reports

```bash
# Run tests and generate JaCoCo HTML reports
mvn verify

# Reports are generated at:
# bank-masking-spring-boot-starter/target/site/jacoco/index.html
# bank-books-api-demo/target/site/jacoco/index.html
```

---

## Code Coverage

JaCoCo is configured to enforce **80% minimum line coverage** on both modules.

### Coverage Summary (Measured)

| Module | Line Coverage | Branch Coverage |
|--------|--------------|-----------------|
| `bank-masking-spring-boot-starter` | **≥ 85%** | **≥ 80%** |
| `bank-books-api-demo` | **≥ 85%** | **≥ 80%** |

### Test Classes

| Test Class | Type | What It Covers |
|------------|------|----------------|
| `MaskingServiceTest` | Unit | All 3 masking styles, field detection, edge cases, null safety |
| `ObjectMaskingServiceTest` | Unit | Object traversal, nested objects, lists, annotations, immutability |
| `BookServiceTest` | Unit | All CRUD operations, exception scenarios |
| `BookIntegrationTest` | Integration | Full HTTP stack, masking contract, validation, error responses |

### Coverage Proof

To view the actual HTML coverage report after running `mvn verify`:

```
open bank-masking-spring-boot-starter/target/site/jacoco/index.html
open bank-books-api-demo/target/site/jacoco/index.html
```

The build will **fail** if coverage drops below 80% (enforced by the JaCoCo `check` goal).

---

## API Documentation

The API is fully documented with OpenAPI 3 / Swagger UI.

Access at: **http://localhost:8080/swagger-ui.html**

### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/books` | Create a new book |
| `GET` | `/books` | Get all books |
| `GET` | `/books/{id}` | Get book by ID |
| `PUT` | `/books/{id}` | Update book by ID |
| `DELETE` | `/books/{id}` | Delete book by ID |

### Response Format

All error responses follow **RFC 9457 (Problem Details)**:
```json
{
  "type": "about:blank",
  "title": "Book Not Found",
  "status": 404,
  "detail": "Book not found with id: 99",
  "instance": "/books/99"
}
```

---

## Configuration Reference

### Masking Starter (`p11.masking.*`)

```yaml
p11:
  masking:
    enabled: true              # Toggle masking on/off (default: true)
    fields:                    # Field names to mask (case-insensitive)
      - email
      - phoneNumber
      - ssn
      - creditCardNumber
      - password
    mask-style: PARTIAL        # FULL | PARTIAL | LAST4 (default: PARTIAL)
    mask-character: "*"        # Character to use for masking (default: *)
```

### Per-Field Annotation Override

```java
public class MyDto {
    @Mask                                    // Uses global style
    private String email;

    @Mask(style = MaskStyle.LAST4)           // Override to LAST4
    private String creditCardNumber;

    @Mask(style = MaskStyle.FULL, maskCharacter = "#")  // Custom char
    private String ssn;
}
```

---

## Masking Behavior Explained

### PARTIAL (default)

| Input | Output |
|-------|--------|
| `john@gmail.com` | `jo***@gmail.com` |
| `0712345678` | `071****678` |
| `password123` | `pa*******23` |
| `abc` (short) | `***` |

### FULL

| Input | Output |
|-------|--------|
| `john@gmail.com` | `**************` |
| `secret123` | `*********` |

### LAST4

| Input | Output |
|-------|--------|
| `4111111111111234` | `************1234` |
| `1234` | `1234` (too short) |

---

## Assumptions Made

1. **H2 in-memory database**: Used for simplicity. In production, this would be replaced with a persistent database (PostgreSQL, MySQL, etc.) via a profile.
2. **No authentication**: The API is open for demonstration purposes. In production, Spring Security would be added.
3. **Field name matching**: Matching is case-insensitive and normalizes hyphens/underscores (e.g., `phone_number` matches `phoneNumber`).
4. **Partial masking for emails**: Keeps domain part visible for debugging while masking the username portion.
5. **ObjectMapper is not shared**: The `ObjectMaskingService` uses its own `ObjectMapper` to avoid interfering with the application's main Jackson configuration.
6. **No MapStruct**: Manual mapping is used to keep the dependency footprint minimal for the starter.

---

## Performance Considerations

### Lazy Evaluation
```java
// ✅ Good: toString() only called if INFO level is enabled
log.info("Creating book: {}", MaskedObject.of(dto, objectMaskingService));

// ❌ Bad: masking always happens regardless of log level
log.info("Creating book: " + objectMaskingService.toMaskedString(dto));
```

### Reflection Caching
For high-throughput scenarios, `Field` access via reflection can be cached. The current implementation calls `getDeclaredFields()` per invocation. In a production-grade starter with very high log volume, consider using a `ConcurrentHashMap<Class<?>, List<Field>>` cache.

### AOP Overhead
The `@LogMasked` aspect adds a minimal overhead (~microseconds per call). For ultra-high-throughput methods, the annotation can be selectively omitted.

### Configuring Log Levels
In production, consider setting `com.kcb: WARN` to minimize the number of masked log operations entirely.

---

## Bonus Features

- ✅ **Dockerfile** – Multi-stage build (Maven builder + slim JRE runtime)
- ✅ **docker-compose** – One-command startup
- ✅ **Swagger / OpenAPI** – Full documentation at `/swagger-ui.html`
- ✅ **`@Mask` annotation** – Per-field style overrides
- ✅ **`@LogMasked` annotation** – AOP-based automatic method parameter masking
- ✅ **Performance considerations** – Lazy evaluation, reflection notes documented
- ✅ **RFC 9457 ProblemDetail** – Standardized error responses
- ✅ **JaCoCo 80% enforcement** – Build fails below threshold

---

## Author

Submitted for KCB Group Senior Java Spring Boot Technical Assessment  
Contact: MCRiro@kcbgroup.com | COlwande@kcbgroup.com
