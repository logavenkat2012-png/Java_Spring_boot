# RentVideo – online video rental REST API

Spring Boot 3 (Java 17) · Spring Security + JWT (stateless) · Spring Data JPA · MySQL

## Run

```bash
# 1. MySQL running locally (database is auto-created), or set DB_USERNAME / DB_PASSWORD
# 2. optional: export JWT_SECRET="<at least 32 characters>"
mvn spring-boot:run
mvn test            # unit tests (Mockito) + end-to-end API test on in-memory H2

# or with Gradle (build.gradle is included)
gradle bootRun
gradle test
```

## Auth flow

1. `POST /auth/register` (public) – JSON body, password stored as BCrypt hash.
2. `POST /auth/login` (public) – send **Basic Auth** (`email:password`), receive a JWT.
3. Call everything else with `Authorization: Bearer <jwt>`. No server-side session.

## Endpoints

| Method | Path | Access | Notes |
|---|---|---|---|
| POST | /auth/register | public | `role` optional, defaults to CUSTOMER → 201 |
| POST | /auth/login | public | Basic Auth → `{token, tokenType, expiresInSeconds, role}` |
| GET | /videos | any authenticated | only videos currently available |
| GET | /videos/{id} | any authenticated | |
| POST | /videos | ADMIN | → 201, always created as available |
| PUT | /videos/{id} | ADMIN | title/director/genre (availability is driven only by rent/return) |
| DELETE | /videos/{id} | ADMIN | soft delete; 409 if currently rented → 204 |
| POST | /videos/{videoId}/rent | any authenticated | → 201 |
| POST | /videos/{videoId}/return | any authenticated | → 200 |
| GET | /rentals/me?activeOnly= | any authenticated | caller's own rentals |

> The brief lists `POST /books/{videoId}/return`; I treated "books" as a typo and used
> `/videos/{videoId}/return` so it is symmetrical with `/rent`.

Error codes: 400 validation / malformed JSON · 401 no/invalid token or bad credentials ·
403 wrong role · 404 not found (or no active rental to return) · 409 conflict
(duplicate email, video already rented, 2-rental limit, delete of rented video) · 500 unexpected.
All errors share one JSON shape (`timestamp, status, error, message, fieldErrors?`).

## Schema

```sql
users   (id PK, email UNIQUE NOT NULL, password NOT NULL, first_name, last_name, role ENUM-as-string NOT NULL)
videos  (id PK, title, director, genre, available BOOL NOT NULL, deleted BOOL NOT NULL)
rentals (id PK, user_id FK→users, video_id FK→videos, rented_at NOT NULL, returned_at NULL,
         INDEX(user_id, returned_at), INDEX(video_id, returned_at))
```

## Design decisions (and why)

* **Rental as its own table, "active" = `returned_at IS NULL`.** Keeps full history, lets the
  2-active-rentals rule be a simple indexed count, and avoids putting `renter_id` on the video.
* **`videos.available` is denormalised** from the rentals table so browsing is a trivial query.
  It is only ever changed inside the rent/return transactions, under row locks, so it cannot drift.
* **Concurrency:** `rent`/`return` take a pessimistic write lock on the user row, then the video row
  (always in that order → no deadlocks). Locking the user makes the "max 2" check race-free
  (two parallel requests for different videos can't both slip through); locking the video stops two
  users renting the same copy.
* **Soft delete for videos** so past rentals still reference a valid row. Deleting a rented video is refused.
* **Both CUSTOMER and ADMIN can rent/return.** An admin is a superset role (staff may also be customers);
  restricting rentals to CUSTOMER would be an equally valid choice, but it's a one-line change in `SecurityConfig`.
* **Role chosen at registration** because the brief requires it. In a real system, self-assigning ADMIN is a
  privilege-escalation hole; admins would be seeded or created by another admin. Flagging this on purpose.
* **Basic Auth only at `/auth/login`**, which exchanges it for a short-lived JWT (1 h). Credentials then
  travel once, and every other call is stateless Bearer auth. CSRF is disabled because there are no cookies/sessions.
* **JWT filter reloads the user** on each request so deleted users/changed roles are honoured immediately
  (one indexed lookup per request – the trade-off for revocation without a blacklist).
* **Authorization rules in one place** (`SecurityConfig`), URL-based, so the permission matrix is reviewable at a glance.
* Emails are normalised to lower case; login failures return a generic 401 (no user enumeration).
* `ddl-auto=update` is for convenience; use Flyway/Liquibase in production. Pagination for `GET /videos` is a natural next step.

## Quick try

```bash
curl -X POST localhost:8080/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"admin@x.com","password":"Password123","firstName":"Ada","lastName":"Min","role":"ADMIN"}'
TOKEN=$(curl -s -X POST localhost:8080/auth/login -u admin@x.com:Password123 | sed 's/.*"token":"\([^"]*\)".*/\1/')
curl -X POST localhost:8080/videos -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Alien","director":"Ridley Scott","genre":"Sci-Fi"}'
curl localhost:8080/videos -H "Authorization: Bearer $TOKEN"
curl -X POST localhost:8080/videos/1/rent -H "Authorization: Bearer $TOKEN"
```
