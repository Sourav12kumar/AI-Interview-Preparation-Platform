# AI Interview Preparation Platform

An AI-powered placement-preparation platform built with Java, Spring Boot, MySQL, and Google Gemini. It will generate personalized interview questions, evaluate answers, analyze resumes, manage coding practice, and show performance trends.

## Current status: Module 2

- Requirements and acceptance criteria
- Architecture and delivery plan
- Complete initial database design
- Spring Boot backend scaffold
- MySQL 8.4 and Flyway configuration
- Public health endpoint
- Automated test and GitHub Actions workflow
- Gemini SDK dependency and secret-safe configuration
- Registration and login with BCrypt password hashing
- Role-based authorization using `ROLE_USER` and `ROLE_ADMIN`
- Signed JWT access and refresh tokens
- Refresh-token rotation, revocation, logout, and replay detection
- Authenticated current-user endpoint and integration tests

## Prerequisites

- Java 17
- Maven 3.6.3 or newer
- Docker Desktop, or MySQL 8.4 installed locally

## Run locally

1. Copy `.env.example` to `.env` and replace the example passwords.
2. Start MySQL:

   ```bash
   docker compose up -d mysql
   ```

3. Export the values from `.env` in your terminal or set them in your IDE.
4. Run the backend:

   ```bash
   mvn spring-boot:run
   ```

5. Open `http://localhost:8080/api/v1/health`.

Expected response:

```json
{
  "status": "UP",
  "service": "ai-interview-preparation-platform",
  "version": "0.1.0",
  "timestamp": "2026-08-21T00:00:00Z"
}
```

The Gemini integration will be implemented in Module 4. Keep `GEMINI_API_KEY` and `JWT_SECRET` outside source control; `.env` is ignored by Git.

## Documentation

- [Requirements](docs/requirements.md)
- [Architecture](docs/architecture.md)
- [Database design](docs/database-design.md)

## Current API

| Method | Route | Authentication | Purpose |
|---|---|---|---|
| `GET` | `/api/v1/health` | Public | Application health check |
| `GET` | `/actuator/health` | Public | Infrastructure health check |
| `POST` | `/api/v1/auth/register` | Public | Register and receive a token pair |
| `POST` | `/api/v1/auth/login` | Public | Authenticate and receive a token pair |
| `POST` | `/api/v1/auth/refresh` | Refresh JWT | Rotate a refresh token and issue a new pair |
| `POST` | `/api/v1/auth/logout` | Refresh JWT | Revoke the supplied refresh session |
| `GET` | `/api/v1/auth/me` | Access JWT | Return the authenticated user |

## Roadmap

The next module adds candidate profile, skills, target role, and target company management.
