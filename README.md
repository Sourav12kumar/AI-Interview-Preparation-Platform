# AI Interview Preparation Platform

An AI-powered placement-preparation platform built with Java, Spring Boot, MySQL, and Google Gemini. It will generate personalized interview questions, evaluate answers, analyze resumes, manage coding practice, and show performance trends.

## Current status: Release hardening complete

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
- Date-range analytics dashboard for interview and coding activity
- Daily score trends, acceptance rate, strongest topics, and improvement topics
- User-owned persisted performance-report history
- Gemini-powered coaching summaries and bounded next-step recommendations
- Administrator-only user status and coding-problem management APIs
- Immutable audit events for privileged mutations
- Correlation IDs, bounded request rate limiting, and Prometheus metrics
- Graceful shutdown, health probes, and a hardened non-root Docker image
- Responsive Thymeleaf landing, registration, login, and dashboard pages
- Browser-safe refresh-token rotation through an HttpOnly SameSite cookie
- In-memory access-token handling with automatic session restoration
- Live analytics summary and topic signals on the dashboard shell
- Candidate profile workspace with professional, education, experience, and career-target fields
- Target-company chip management with duplicate and maximum-count safeguards
- Skill inventory with proficiency, years used, create, update, and remove flows
- Profile completion guidance, empty states, validation feedback, and protected deletion
- Mock-interview configuration for type, difficulty, target role, and 1–10 questions
- Gemini generation states, interview history, status filters, and session continuation
- Focused question room with timer, progress map, answer submission, and review navigation
- Five-dimensional AI feedback with strengths, improvements, ideal answers, and final score
- Private drag-and-drop resume upload with browser and server-side type/size validation
- Resume history with analysis state, file metadata, ATS scores, selection, and protected deletion
- Role and optional job-description configuration for Gemini ATS comparison
- ATS match summary, strengths, weaknesses, keyword gaps, and prioritized improvement actions
- Reanalysis support with explicit Gemini processing, failure, empty, and responsive states
- Searchable coding-problem catalogue with difficulty, topic, solved, and attempt signals
- Accessible four-language editor with starter-code switching, in-memory drafts, and reset confirmation
- Isolated-runner submission flow with duplicate-submit protection and unavailable-runner feedback
- Final verdict, hidden-test count, score, execution time, memory, and language result details
- User-owned submission history with safe source-code review and resubmission workflow
- Custom 7/30/90/366-day analytics periods with exact server-calculated overview metrics
- Accessible interview/coding trend chart and strongest/improvement topic score breakdowns
- Gemini coaching-report generation, refresh, history, and owned detail views
- Role-aware administrator entry point with server-enforced `ROLE_ADMIN` authorization
- Administrator user search, status filtering, suspension/activation, and session revocation flow
- Coding-problem create, edit, reactivate, deactivate, starter-code, hidden-test, and tag management
- Immutable privileged audit trail with actor, target, outcome, correlation ID, and safe metadata display
- Playwright Chromium journeys for registration, authentication, profile persistence, logout, and anonymous redirects
- Automated axe WCAG A/AA checks for public and authenticated critical pages
- MySQL-backed browser CI with traces, screenshots, videos, reports, and server logs on failure
- CodeQL security-extended analysis for Java/Kotlin and JavaScript/TypeScript
- Manual and version-tag release-candidate packaging with SHA-256 verification
- Production Spring profile with secure-cookie defaults and bounded database/server capacity controls
- Promotion, observation, rollback, recovery, and human launch sign-off checklist

## Prerequisites

- Java 17
- Maven 3.6.3 or newer
- Docker Desktop, or MySQL 8.4 installed locally
- Node.js 24 for browser and accessibility tests

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

To run the complete stack in containers, set `JWT_SECRET` and other required values in `.env`, then run `docker compose up --build`.

To run the browser release gate after the application and MySQL are healthy:

```bash
npm ci
npx playwright install chromium
npm run test:e2e
```

Expected response:

```json
{
  "status": "UP",
  "service": "ai-interview-preparation-platform",
  "version": "0.1.0",
  "timestamp": "2026-08-21T00:00:00Z"
}
```

Gemini powers interview generation, answer evaluation, resume analysis, and performance recommendations. Keep `GEMINI_API_KEY`, `JWT_SECRET`, and `CODE_RUNNER_API_KEY` outside source control; `.env` is ignored by Git. Resume files are stored under `RESUME_STORAGE_DIR` (default `./data/resumes`) and this directory must be private in production.

Code submissions are never executed by the Spring Boot process. Configure `CODE_RUNNER_BASE_URL` to an isolated runner that accepts `POST /v1/execute`; for local development the example URL is `http://localhost:8090`. The runner receives the problem slug, language, source, hidden test cases, and resource limits, then returns a final verdict, test counts, execution time, memory, score, and a safe result message.

## Documentation

- [Requirements](docs/requirements.md)
- [Architecture](docs/architecture.md)
- [Database design](docs/database-design.md)
- [Operations and deployment](docs/operations.md)
- [Frontend architecture](docs/frontend.md)
- [Release readiness](docs/release-readiness.md)
- [Windows download and local setup](docs/windows-local-setup.md)

## Current API

| Method | Route | Authentication | Purpose |
|---|---|---|---|
| `GET` | `/api/v1/health` | Public | Application health check |
| `GET` | `/actuator/health` | Public | Infrastructure health check |
| `GET` | `/actuator/prometheus` | Admin JWT | Prometheus metrics |
| `GET` | `/`, `/login`, `/register`, `/dashboard`, `/profile`, `/interviews`, `/interviews/{id}`, `/resumes`, `/coding`, `/coding/{id}`, `/analytics`, `/admin` | Public shell | Render responsive web pages; protected data still requires browser authentication and admin APIs still require `ROLE_ADMIN` |
| `POST` | `/api/v1/auth/register` | Public | Register and receive a token pair |
| `POST` | `/api/v1/auth/login` | Public | Authenticate and receive a token pair |
| `POST` | `/api/v1/auth/refresh` | Refresh JWT | Rotate a refresh token and issue a new pair |
| `POST` | `/api/v1/auth/logout` | Refresh JWT | Revoke the supplied refresh session |
| `GET` | `/api/v1/auth/me` | Access JWT | Return the authenticated user |
| `POST` | `/api/v1/auth/browser/register` | Public | Register, return an access token, and set the refresh token as an HttpOnly cookie |
| `POST` | `/api/v1/auth/browser/login` | Public | Log in through the browser-safe cookie flow |
| `POST` | `/api/v1/auth/browser/refresh` | Refresh cookie | Rotate the cookie and restore the in-memory browser session |
| `POST` | `/api/v1/auth/browser/logout` | Refresh cookie | Revoke the session and clear the cookie |
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
| `GET` | `/api/v1/analytics/dashboard` | Access JWT | Return summaries, trends, and topic performance for an optional `from`/`to` date range |
| `POST` | `/api/v1/analytics/reports` | Access JWT | Generate or refresh a Gemini performance report for a date range |
| `GET` | `/api/v1/analytics/reports` | Access JWT | List the candidate's saved performance reports |
| `GET` | `/api/v1/analytics/reports/{reportId}` | Access JWT | Return one owned performance report |
| `GET` | `/api/v1/admin/overview` | Admin JWT | Return platform counts |
| `GET` | `/api/v1/admin/users` | Admin JWT | Search and filter users |
| `PATCH` | `/api/v1/admin/users/{userId}/status` | Admin JWT | Activate or suspend a user |
| `GET` | `/api/v1/admin/coding/problems` | Admin JWT | List all active and inactive problems including hidden tests |
| `POST` | `/api/v1/admin/coding/problems` | Admin JWT | Create a coding problem |
| `PUT` | `/api/v1/admin/coding/problems/{problemId}` | Admin JWT | Replace a coding problem |
| `DELETE` | `/api/v1/admin/coding/problems/{problemId}` | Admin JWT | Soft-deactivate a coding problem |
| `GET` | `/api/v1/admin/audit-events` | Admin JWT | Read newest privileged audit events |

## Next delivery

Deploy the immutable release candidate to staging, complete the manual accessibility and operational sign-off, validate live Gemini and isolated-runner journeys, and then promote the same image digest to production.
