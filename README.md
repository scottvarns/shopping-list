# Shopping List Application
Shopping List Application (Created for Cyber Media Software Developer/Engineer Recruitment Coding Challenge)

---

## Table of contents

- [Description](#description)
  - [Technology stack](#technology-stack)
- [Prerequisites](#prerequisites)
  - [Local startup](#local-startup)
- [API documentation](#api-documentation)
  - [Endpoint summary](#endpoint-summary)
  - [Sign up](#sign-up)
  - [Log in](#log-in)
  - [Create a shopping list](#create-a-shopping-list)
  - [Retrieve a shopping list](#retrieve-a-shopping-list)
  - [Append a list item](#append-a-list-item)
  - [Toggle an item in the basket](#toggle-an-item-in-the-basket)
  - [Delete a list item](#delete-a-list-item)
  - [Reorder list items](#reorder-list-items)
  - [Error response bodies](#error-response-bodies)
- [Testing](#testing)
  - [Running the complete test suite](#running-the-complete-test-suite)
  - [Running individual test layers](#running-individual-test-layers)
  - [Running tests in IntelliJ IDEA](#running-tests-in-intellij-idea)

---

## Description

- The Shopping List Application is a REST API for user registration, JWT authentication, shopping-list creation, with built in budget management functionality.
- Database migrations are managed by Flyway, and application data is persisted in MySQL.
- Protected API endpoints use JWT bearer authentication and verify that the authenticated user owns the requested shopping list.
- All API endpoints require a correlation ID header for request tracing.


### Technology stack

- Java 25
- Spring Boot 4.1, Spring MVC, Spring Validation, and Spring Actuator
- Spring Security, JWTs authentication, and BCrypt password hashing
- Spring Data JPA, MySQL, and Flyway migrations
- JUnit 5, Cucumber, Mockito, AssertJ, MockMvc, and Testcontainers with MySQL for integration and BDD tests
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

## API documentation

The complete OpenAPI specifications are available in the `apiSpecs` folder of this repository:

- [Authentication OpenAPI specification](apiSpecs/auth.yaml)
- [Shopping-list OpenAPI specification](apiSpecs/shoppingList.yaml)

Every endpoint requires an `X-Correlation-ID` header containing a UUID. Every shopping-list endpoint also requires the JWT returned by the login endpoint:

```http
X-Correlation-ID: e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3
Authorization: Bearer <token>
```

Authentication endpoints do not require a bearer token. Shopping-list operations verify that the authenticated user exists and owns the requested list.

### Endpoint summary

| Method   | Endpoint                                    | Authentication | Successful response | Description                                          |
|----------|---------------------------------------------|----------------|---------------------|------------------------------------------------------|
| `POST`   | `/api/auth/signup`                          | Public         | `201 Created`       | Create a user account                                |
| `POST`   | `/api/auth/login`                           | Public         | `200 OK`            | Authenticate and obtain a JWT                        |
| `POST`   | `/api/shopping-list`                        | Bearer JWT     | `201 Created`       | Create an empty shopping list                        |
| `GET`    | `/api/shopping-list/{listId}`               | Bearer JWT     | `200 OK`            | Retrieve a list, it's items, and budget calculations |
| `POST`   | `/api/shopping-list/{listId}/item`          | Bearer JWT     | `201 Created`       | Append an item to a list                             |
| `PATCH`  | `/api/shopping-list/{listId}/item/{itemId}` | Bearer JWT     | `204 No Content`    | Toggle an item's `inBasket` state                    |
| `DELETE` | `/api/shopping-list/{listId}/item/{itemId}` | Bearer JWT     | `204 No Content`    | Delete an item and close the position gap            |
| `PATCH`  | `/api/shopping-list/{listId}/item/reorder`  | Bearer JWT     | `204 No Content`    | Partially or completely reorder list items           |

### Sign up

Creates a user and stores a BCrypt password hash. Email addresses must be valid and unique, passwords must contain 8–72 characters, and names are limited to 150 characters.

```http
POST /api/auth/signup
Content-Type: application/json
X-Correlation-ID: e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3

{
  "email": "user@example.com",
  "password": "your-password",
  "name": "Your Name"
}
```

```json
{
  "userId": 1,
  "email": "user@example.com",
  "name": "Your Name"
}
```

Responses:

- `201 Created` when the user is created successfully.
- `400 Bad Request` when the correlation ID is missing or is not a UUID, or when the email, password, or name fails request validation.
- `409 Conflict` when an account already exists for the supplied email address.
- `500 Internal Server Error` when an unexpected server or database error occurs.

### Log in

Authenticates an existing user. Tokens expire after one hour by default; set `JWT_EXPIRATION_SECONDS` to override the lifetime.

```http
POST /api/auth/login
Content-Type: application/json
X-Correlation-ID: e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3

{
  "email": "user@example.com",
  "password": "your-password"
}
```

```json
{
  "token": "<jwt>"
}
```

Responses:

- `200 OK` when the credentials are valid; the response contains the JWT.
- `400 Bad Request` when the correlation ID is missing or is not a UUID, or when the email or password fails request validation.
- `401 Unauthorized` when the email does not exist or the password does not match.
- `500 Internal Server Error` when an unexpected server or database error occurs.

### Create a shopping list

The `userId` must match the user ID held in the JWT. `spendingLimit` and `date` are optional. List names must be unique for each user.

```http
POST /api/shopping-list
Authorization: Bearer <token>
Content-Type: application/json
X-Correlation-ID: e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3

{
  "userId": 1,
  "listName": "Weekly Shop",
  "spendingLimit": 75.00,
  "date": "2026-07-18"
}
```

New lists return `totalCost` as zero, `remainingBudget` equal to the spending limit, and `overBudget` as `false`. If no spending limit is supplied, `remainingBudget` is `null`.

Responses:

- `201 Created` when the shopping list is created successfully.
- `400 Bad Request` when the correlation ID or request body is invalid.
- `401 Unauthorized` when the bearer token is missing or invalid.
- `403 Forbidden` when the authenticated token's user ID does not match the request's `userId`.
- `404 Not Found` when the requested user does not exist.
- `409 Conflict` when the user already has a shopping list with the same name.
- `500 Internal Server Error` when an unexpected server or database error occurs.

### Retrieve a shopping list

Returns list details and items ordered by `listPosition`. Each nested item omits `listId` because it is already present at the list level. `totalCost` is the sum of `unitCost × quantity`; `remainingBudget` is `spendingLimit - totalCost`, and `overBudget` is `true` only when the total exceeds the limit. With no spending limit, `remainingBudget` is `null` and `overBudget` is `false`.

```http
GET /api/shopping-list/12
Authorization: Bearer <token>
X-Correlation-ID: e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3
```

```json
{
  "listId": 12,
  "userId": 1,
  "listName": "Weekly Shop",
  "spendingLimit": 10.00,
  "date": "2026-07-18",
  "totalCost": 11.00,
  "remainingBudget": -1.00,
  "overBudget": true,
  "items": [
    {
      "itemId": 44,
      "itemName": "Milk",
      "unitCost": 5.50,
      "quantity": 2,
      "inBasket": false,
      "listPosition": 1
    }
  ]
}
```

Responses:

- `200 OK` when the list is found and returned with its ordered items and budget calculations.
- `400 Bad Request` when the correlation ID is invalid or `listId` is non-numeric.
- `401 Unauthorized` when the bearer token is missing or invalid.
- `403 Forbidden` when the authenticated user does not own the list.
- `404 Not Found` when the list or its associated user does not exist.
- `500 Internal Server Error` when an unexpected server or database error occurs.

### Append a list item

Adds the item at the current maximum position plus one. `inBasket` defaults to `false`, and item names are unique within a shopping list.

```http
POST /api/shopping-list/12/item
Authorization: Bearer <token>
Content-Type: application/json
X-Correlation-ID: e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3

{
  "itemName": "Milk",
  "unitCost": 1.25,
  "quantity": 2
}
```

The `201 Created` response includes `itemId`, `listId`, `itemName`, `unitCost`, `quantity`, `inBasket`, and `listPosition`.

Responses:

- `201 Created` when the item is appended successfully.
- `400 Bad Request` when the correlation ID, `listId`, or request body is invalid.
- `401 Unauthorized` when the bearer token is missing or invalid.
- `403 Forbidden` when the authenticated user does not own the list.
- `404 Not Found` when the list or its associated user does not exist.
- `409 Conflict` when the same item name already exists on that shopping list.
- `500 Internal Server Error` when an unexpected server or database error occurs.

### Toggle an item in the basket

Toggles `inBasket` between `true` and `false`. The item remains on the list and its position does not change. This endpoint has no request or response body.

```http
PATCH /api/shopping-list/12/item/44
Authorization: Bearer <token>
X-Correlation-ID: e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3
```

Responses:

- `204 No Content` when the item's `inBasket` value is toggled successfully.
- `400 Bad Request` when the correlation ID is invalid or `listId` or `itemId` is non-numeric.
- `401 Unauthorized` when the bearer token is missing or invalid.
- `403 Forbidden` when the authenticated user does not own the list.
- `404 Not Found` when the list, its associated user, or the item on that list does not exist.
- `500 Internal Server Error` when an unexpected server or database error occurs.

### Delete a list item

Deletes the item and decrements subsequent positions so the list remains contiguous. This endpoint has no request or response body.

```http
DELETE /api/shopping-list/12/item/44
Authorization: Bearer <token>
X-Correlation-ID: e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3
```

Responses:

- `204 No Content` when the item is deleted and subsequent positions are updated successfully.
- `400 Bad Request` when the correlation ID is invalid or `listId` or `itemId` is non-numeric.
- `401 Unauthorized` when the bearer token is missing or invalid.
- `403 Forbidden` when the authenticated user does not own the list.
- `404 Not Found` when the list, its associated user, or the item on that list does not exist.
- `500 Internal Server Error` when an unexpected server or database error occurs.

### Reorder list items

Accepts a non-empty JSON array of item IDs and final positions. A request may specify every item or only part of the list. Explicitly requested items are fixed at their requested positions; unspecified items fill the remaining positions while retaining their relative order. Item IDs and positions cannot be repeated, and positions must be within the current list size.

```http
PATCH /api/shopping-list/12/item/reorder
Authorization: Bearer <token>
Content-Type: application/json
X-Correlation-ID: e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3

[
  {
    "itemId": 46,
    "listPosition": 1
  },
  {
    "itemId": 44,
    "listPosition": 3
  }
]
```

Responses:

- `204 No Content` when all requested positions are applied successfully.
- `400 Bad Request` when the correlation ID or `listId` is invalid; the array is empty; an entry is invalid; an item ID or position is duplicated; or a position is outside the list size.
- `401 Unauthorized` when the bearer token is missing or invalid.
- `403 Forbidden` when the authenticated user does not own the list.
- `404 Not Found` when the list, its associated user, or a requested item on that list does not exist.
- `500 Internal Server Error` when an unexpected server or database error occurs.

### Error response bodies

Service-generated errors use this shape:

```json
{
  "error": "BAD_REQUEST",
  "message": "Request validation failed"
}
```

---

## Testing

The repository contains the following test layers:

| Test layer                      | Coverage                                                                                      | Database required   |
|---------------------------------|-----------------------------------------------------------------------------------------------|---------------------|
| Service and security unit tests | Business rules, mappings, JWT handling, and transaction boundaries                            | No                  |
| Controller tests                | Request validation, required headers, path parameters, response codes, and service delegation | No                  |
| API integration tests           | Each API endpoint in isolation through Spring MVC, Spring Security, JPA, and Flyway           | MySQL Testcontainer |
| Cucumber BDD test               | The complete registration, login, shopping-list, budgeting, and item-management journey       | MySQL Testcontainer |

### Running the complete test suite

Docker Desktop must be running before executing the complete suite. Run all unit, controller, integration, and Cucumber tests with:

```zsh
./mvnw test
```

The integration and Cucumber tests create disposable MySQL containers automatically. Flyway applies the repository migrations to each test database, so the MySQL instance started by `docker compose` is not used by the tests.

### Running individual test layers

Run a specific unit or controller test class by passing its class name:

```zsh
./mvnw -Dtest=ShoppingListServiceTest test
./mvnw -Dtest=ShoppingListControllerTest test
```

Run the isolated API integration tests with:

```zsh
./mvnw -Dtest=ApiIntegrationTest test
```

Run the complete-shopping-journey Cucumber scenario with:

```zsh
./mvnw -Dtest=RunCucumberTest test
```

The Cucumber feature is located at `src/test/resources/features/complete-shopping-journey.feature`.

### Running tests in IntelliJ IDEA

1. Configure JDK 25 as the project SDK and Maven runner JDK.
2. Reload the Maven project so that all JUnit, Cucumber, and Testcontainers dependencies are available.
3. Start Docker Desktop before running an integration or Cucumber test.
4. Run your desired tests. To execute the Cucumber feature, open `RunCucumberTest` and run the class.
