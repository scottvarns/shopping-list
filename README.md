# Shopping List Application
Shopping List Application (Created for Cyber Media Software Developer/Engineer Recruitment Coding Challenge)

---

## Description

- The Shopping List Application is a REST API for user registration, JWT authentication, and creating shopping lists. 
- Database migration are managed via flyway, and persists data to a MySQL database. 
- Protected API endpoints use JWT bearer authentication and verify that the authenticated user owns the shopping list being created.
- All API endpoints require a correlation ID header for request tracing.

### Technology stack

- Java 25
- Spring Boot 4.1, Spring MVC, Spring Validation, and Spring Actuator
- Spring Security, JWTs authentication, and BCrypt password hashing
- Spring Data JPA, MySQL, and Flyway migrations
- JUnit 5, Mockito, AssertJ, MockMvc, and Testcontainers with MySQL for integration tests
- Docker Compose for running a local MySQL database

---

## Prerequisites

- **JDK 25** installed and available on your `PATH` (`java -version`)
- **Docker Desktop** running, for the local MySQL database and MySQL Testcontainers integration tests
- No system Maven installation is required; use the included Maven Wrapper (`./mvnw`)
- A Base64-encoded JWT signing key of at least 32 bytes. Generate one with:

  ```zsh
  openssl rand -base64 32
  ```

### Local startup

1. Start MySQL:

   ```zsh
   docker compose up -d mysql
   ```

2. Set the JWT signing key and run the application with the local profile:

   ```zsh
   export JWT_SECRET="<your-base64-secret>"
   ./mvnw spring-boot:run -Plocal
   ```

Flyway applies the database migrations automatically when the application starts.

---

### API Endpoints
The application exposes the following RESTful API endpoints:

| Endpoint            | Method | Description                                          |
|---------------------|--------|------------------------------------------------------|
| /api/auth/signup    | POST   | Create a new user                                    |
| /api/auth/login     | POST   | Authenticate an existing user and return a JWT token |
| /api/shopping-list  | POST   | Create a new shopping list                           |

---

### Create a new user
To create a new user, send a POST request to `/api/auth/signup` with the following JSON payload:

```http
POST /api/auth/signup
Content-Type: application/json
X-Correlation-ID: e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3

{
    "email":"user@example.com",
    "password":"your-password", 
    "name":"Your Name"
}
```

This will create a new user on the database. The password will be securely hashed before being stored in the database.

---

### Authentication
All `/api/**` endpoints, apart from `/api/auth/login` & `api/auth/signup`, require a JWT bearer token.

Set `JWT_SECRET` to a Base64-encoded secret of at least 32 bytes before starting the application. Tokens expire after one hour by default; override this with `JWT_EXPIRATION_SECONDS` when needed.

To authenticate an existing user:

```http
POST /api/auth/login
Content-Type: application/json
X-Correlation-ID: e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3

{
    "email":"user@example.com",
    "password":"your-password"
}
```

Send the returned token on protected requests:

```http
Authorization: Bearer <token>
```
