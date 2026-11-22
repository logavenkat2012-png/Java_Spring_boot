# RentVideo - Video Rental System API

Spring Boot 3 REST API with MySQL persistence, Basic Auth, BCrypt password hashing and role based
authorization (`CUSTOMER`, `ADMIN`).

## Requirements
- Java 21
- Maven 3.9+
- MySQL 8 (running locally)

## Setup
1. Make sure MySQL is running. The database `rentvideo_db` is created automatically.
2. Provide your credentials (defaults are `root` / `your_password`):
   ```cmd
   set DB_USERNAME=root
   set DB_PASSWORD=your_real_password
   ```
   (Linux/macOS: `export DB_PASSWORD=...`). Optional: `DB_URL` overrides the JDBC URL.
3. Run:
   ```cmd
   mvn spring-boot:run
   ```
   The API starts on `http://localhost:8080`.

## Run tests
Tests use in-memory H2, so MySQL is not needed:
```cmd
mvn test
```

## Endpoints

| Method | Path                 | Access          | Description                                   |
|--------|----------------------|-----------------|-----------------------------------------------|
| POST   | `/api/auth/register` | Public          | Register (role defaults to `CUSTOMER`)        |
| POST   | `/api/auth/login`    | Basic Auth      | Verify credentials, returns the user profile  |
| GET    | `/api/videos`        | CUSTOMER, ADMIN | List available videos (`?genre=Drama` filter) |
| GET    | `/api/videos/{id}`   | CUSTOMER, ADMIN | Get a video                                   |
| POST   | `/api/videos`        | ADMIN           | Create a video                                |
| PUT    | `/api/videos/{id}`   | ADMIN           | Update a video                                |
| DELETE | `/api/videos/{id}`   | ADMIN           | Delete a video                                |

## Error responses
All errors return JSON: `{ "timestamp", "status", "error", "message" }`.

| Code | When                                           |
|------|------------------------------------------------|
| 400  | Validation failure / malformed JSON / bad role |
| 401  | Missing or invalid credentials                 |
| 403  | Authenticated but wrong role                   |
| 404  | Video (or user) not found                      |
| 409  | Email already registered                       |
| 500  | Unexpected error                               |

## Examples (curl)
```bash
# Register an admin and a customer
curl -X POST localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"email":"admin@rentvideo.com","password":"admin123","firstName":"Ad","lastName":"Min","role":"ADMIN"}'
curl -X POST localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"email":"john@rentvideo.com","password":"john123","firstName":"John","lastName":"Doe"}'

# Login (Basic Auth)
curl -X POST -u john@rentvideo.com:john123 localhost:8080/api/auth/login

# Admin creates a video
curl -X POST -u admin@rentvideo.com:admin123 localhost:8080/api/videos -H "Content-Type: application/json" \
  -d '{"title":"Inception","director":"Christopher Nolan","genre":"Sci-Fi"}'

# Customer browses videos
curl -u john@rentvideo.com:john123 localhost:8080/api/videos
```

In Postman: choose **Authorization -> Basic Auth** and enter the email as the username.

## Design notes
- Passwords are hashed with BCrypt and never returned in responses.
- Emails are stored lower-case and are unique.
- New videos default to `available = true`.
- The spec lets users choose their role at registration, so anyone can register as `ADMIN`.
  That is intentional for this exercise; a production system would restrict admin creation.
