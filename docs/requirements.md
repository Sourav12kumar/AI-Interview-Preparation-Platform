# Software Requirements Specification

## 1. Purpose

The AI Interview Preparation Platform helps job seekers prepare for technical, HR, behavioral, and coding interviews. It creates personalized questions with Gemini, evaluates answers, analyzes resumes, tracks progress, and recommends focused practice.

## 2. Actors

| Actor | Responsibilities |
|---|---|
| Candidate | Manages profile, uploads resume, practises interviews, solves coding problems, views reports |
| Administrator | Manages users, questions, coding problems, reports, and platform health |
| Gemini API | Generates questions, evaluates answers, analyzes resume content, and creates recommendations |

## 3. Functional requirements

| ID | Requirement | Priority |
|---|---|---|
| FR-01 | A candidate can register, verify identity, log in, refresh a session, and log out securely. | Must |
| FR-02 | A candidate can maintain education, experience, target role, target company, and skills. | Must |
| FR-03 | A candidate can upload a PDF resume and receive ATS score, strengths, weaknesses, and missing keywords. | Must |
| FR-04 | The system can generate role-, skill-, type-, and difficulty-specific interview questions using Gemini. | Must |
| FR-05 | A candidate can complete a mock interview one question at a time. | Must |
| FR-06 | The system can evaluate relevance, clarity, technical correctness, and overall quality of an answer. | Must |
| FR-07 | A candidate can solve coding problems and receive test-case-based results. | Should |
| FR-08 | A candidate can view score trends, weak topics, completed sessions, and recommendations. | Must |
| FR-09 | An administrator can manage users, coding problems, and platform content. | Should |
| FR-10 | The system stores an auditable history of interviews, answers, evaluations, and submissions. | Must |

## 4. Non-functional requirements

| ID | Requirement |
|---|---|
| NFR-01 | Passwords must be hashed with BCrypt or Argon2 and never stored in plain text. |
| NFR-02 | Protected APIs must require short-lived JWT access tokens; refresh tokens must be revocable. |
| NFR-03 | Secrets must be supplied through environment variables and excluded from Git. |
| NFR-04 | Normal API requests should respond within two seconds, excluding Gemini and code-execution latency. |
| NFR-05 | Gemini calls must use timeouts, bounded retries, rate limits, and structured output validation. |
| NFR-06 | Uploaded files must be type-checked, size-limited, malware-scanned in production, and access-controlled. |
| NFR-07 | Database changes must be versioned through Flyway migrations. |
| NFR-08 | The backend must provide automated tests and a CI build for every pull request. |
| NFR-09 | Personally identifiable information and interview content must not appear in application logs. |
| NFR-10 | The design must support later separation of AI, code-execution, and file-storage services. |

## 5. MVP scope

The first release includes authentication, profile and skills, resume analysis, AI question generation, mock interviews, answer evaluation, a basic coding module, and a performance dashboard. Voice interviews, live video analysis, company-specific question scraping, and recruiter accounts are outside the MVP.

## 6. Acceptance criteria

- A new user can register, log in, and access only their own data.
- A user can configure a target role and receive a Gemini-generated interview set.
- Each submitted answer produces numeric scores and actionable feedback.
- A PDF resume produces a persisted analysis without exposing the Gemini API key.
- The dashboard summarizes interview and coding performance from stored results.
- The application starts with MySQL 8.4 and all Flyway migrations applied.
- CI builds and tests the application on Java 17.
