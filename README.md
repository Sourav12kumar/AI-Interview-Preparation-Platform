# AI Interview Preparation Platform

An AI-powered placement-preparation platform built with Java, Spring Boot, MySQL, and Google Gemini. It will generate personalized interview questions, evaluate answers, analyze resumes, manage coding practice, and show performance trends.

## Current status: Module 7

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
- Candidate profile create/update, read, and delete
- Education, experience, target role, and target company management
- User-owned skill assignments with proficiency and years of use
- Cross-user access protection and profile integration tests
- Gemini-powered personalized interview question generation
- Persisted interview sessions with AI model and prompt-version metadata
- Strict structured-output validation and deterministic AI-boundary tests
- Answer submission with one answer per generated question
- Gemini-powered scoring, strengths, improvements, and ideal answers
- Interview progress tracking, completion, and aggregate scoring
- Private PDF, DOCX, and TXT resume upload with 5 MB limits
- Apache Tika content detection and bounded text extraction
- Gemini-powered ATS score, keyword gap, strengths, weaknesses, and suggestions
- User-owned resume history, reanalysis, and deletion
- Authenticated coding-problem catalogue with difficulty and tag filters
- Starter code for Java, Python, JavaScript, and C++ without exposing hidden tests
- User-owned code submissions, verdicts, metrics, scores, and history
- Remote-only code-runner boundary with timeouts and response validation

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

Gemini powers interview generation, answer evaluation, and resume analysis. Keep `GEMINI_API_KEY`, `JWT_SECRET`, and `CODE_RUNNER_API_KEY` outside source control; `.env` is ignored by Git. Resume files are stored under `RESUME_STORAGE_DIR` (default `./data/resumes`) and this directory must be private in production.

Code submissions are never executed by the Spring Boot process. Configure `CODE_RUNNER_BASE_URL` to an isolated runner that accepts `POST /v1/execute`; for local development the example URL is `http://localhost:8090`. The runner receives the problem slug, language, source, hidden test cases, and resource limits, then returns a final verdict, test counts, execution time, memory, score, and a safe result message.

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
| `GET` | `/api/v1/profile` | Access JWT | Return the candidate profile aggregate |
| `PUT` | `/api/v1/profile` | Access JWT | Create or replace profile details and target companies |
| `DELETE` | `/api/v1/profile` | Access JWT | Delete profile details and assignments |
| `GET` | `/api/v1/profile/skills` | Access JWT | List the candidate's skills |
| `POST` | `/api/v1/profile/skills` | Access JWT | Add a skill assignment |
| `PUT` | `/api/v1/profile/skills/{skillId}` | Access JWT | Update proficiency and years used |
| `DELETE` | `/api/v1/profile/skills/{skillId}` | Access JWT | Remove a skill assignment |
| `POST` | `/api/v1/interviews` | Access JWT | Generate and persist a personalized interview |
| `GET` | `/api/v1/interviews` | Access JWT | List the candidate's interview sessions |
| `GET` | `/api/v1/interviews/{sessionId}` | Access JWT | Return one owned session and its questions |
| `POST` | `/api/v1/interviews/{sessionId}/answers` | Access JWT | Submit and evaluate one answer |
| `GET` | `/api/v1/interviews/{sessionId}/answers` | Access JWT | List evaluated answers for an owned session |
| `POST` | `/api/v1/resumes` | Access JWT | Upload and extract a PDF, DOCX, or TXT resume |
| `POST` | `/api/v1/resumes/{resumeId}/analysis` | Access JWT | Run Gemini ATS analysis for a target role/job description |
| `GET` | `/api/v1/resumes` | Access JWT | List the candidate's resume history |
| `GET` | `/api/v1/resumes/{resumeId}` | Access JWT | Return one owned resume and its analysis |
| `DELETE` | `/api/v1/resumes/{resumeId}` | Access JWT | Delete owned resume metadata and private file |
| `GET` | `/api/v1/coding/problems` | Access JWT | List active problems; optionally filter by `difficulty` and `tag` |
| `GET` | `/api/v1/coding/problems/{problemId}` | Access JWT | Return a problem and starter code without hidden tests |
| `POST` | `/api/v1/coding/problems/{problemId}/submissions` | Access JWT | Execute code through the isolated runner and persist the result |
| `GET` | `/api/v1/coding/submissions` | Access JWT | List the candidate's submission history |
| `GET` | `/api/v1/coding/submissions/{submissionId}` | Access JWT | Return one owned submission and result |

## Roadmap

The next module adds analytics summaries, score trends, weak-topic insights, and Gemini recommendations.
