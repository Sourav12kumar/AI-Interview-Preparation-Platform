# Frontend Architecture

## Delivery plan

| Module | User-facing scope | Status |
|---|---|---|
| 1. Foundation and auth | Landing page, registration, login, secure refresh cookie, dashboard shell | Complete |
| 2. Candidate profile | Profile details, target companies, skills, education, and experience | Complete |
| 3. Mock interviews | Interview configuration, question flow, answer submission, and evaluation | Complete |
| 4. Resume workspace | Upload, history, ATS analysis, keyword gaps, and suggestions | Complete |
| 5. Coding workspace | Problem catalogue, editor integration, submissions, verdicts, and history | Next |
| 6. Analytics and admin | Trends, saved coaching reports, user controls, problem management, and audit history | Planned |

## Browser authentication

The existing `/api/v1/auth/*` routes remain available for API clients. The web interface uses `/api/v1/auth/browser/*`:

1. Registration or login returns the access token and user DTO in JSON.
2. The refresh token is set as an `HttpOnly`, `SameSite=Strict` cookie scoped to the browser-auth routes.
3. JavaScript stores the access token only in module memory, not local or session storage.
4. A page reload calls the refresh route, rotates the cookie, and restores the access token.
5. Authenticated API requests attach the access token as a Bearer credential and retry once after a 401 refresh.
6. Logout revokes the refresh session and expires the cookie even when the supplied token is already invalid.

The content security policy allows resources and API connections only from the same origin. Page shells are public so Spring Security does not require a Bearer token for HTML navigation; all personal data continues to come from protected REST APIs.

## Frontend conventions

- Thymeleaf owns page composition and same-origin asset URLs.
- JavaScript modules own browser authentication and REST interaction.
- DOM updates use `textContent` and element creation, never untrusted HTML injection.
- Layouts begin mobile-responsive and use system fonts, semantic landmarks, labels, and visible validation feedback.
- Shared colors, spacing, buttons, cards, navigation, and form states are defined in `static/css/app.css`.

## Dashboard integration

The dashboard restores the secure browser session and loads `/api/v1/analytics/dashboard`. Summary cards show interview score, coding score, completed interviews, submissions, acceptance rate, and strong/improvement topics. Practice links are visual entry points that the next modules will connect to full workspaces.

## Profile integration

The candidate profile page uses the same in-memory access-token client and integrates directly with `/api/v1/profile` and `/api/v1/profile/skills`.

- Professional identity includes headline, private phone/location fields, and a bounded summary.
- Education and experience include education level, institution, graduation year, and years of experience.
- Career targeting includes a target role and up to 20 case-insensitively unique companies.
- Skill assignments support add, update, and remove operations with category, proficiency, and years used.
- A completion score is calculated locally from filled sections and is never treated as an employment or personality assessment.
- Profile deletion requires an explicit confirmation and removes profile fields, target companies, and skill assignments without deleting the account.

All API-originated content is rendered using safe DOM text nodes. Validation errors are mapped to their fields, and destructive operations preserve clear scope and confirmation.

## Interview integration

The interview studio integrates with `/api/v1/interviews` and the owned `/api/v1/interviews/{sessionId}/answers` collection.

- Candidates configure interview type, difficulty, question count, and an optional target-role override.
- Session history shows created, in-progress, and completed interviews without exposing another user's data.
- The interview room loads persisted questions and answers, resumes at the first unanswered question, and supports review of completed feedback.
- A local timer records bounded response time; it is not used as a score and stops at the backend's 7,200-second maximum.
- Answer text is submitted once per question. The UI never provides an overwrite path for evaluated answers.
- Gemini evaluation renders technical, relevance, clarity, confidence, and overall scores with native progress elements, plus strengths, improvements, and an ideal answer.
- The final score is read from the completed server-side session rather than calculated in the browser.

Generation and evaluation can take several seconds. Full-screen progress states prevent accidental duplicate submissions while the backend processes a Gemini request.

## Resume integration

The resume workspace integrates with the owned `/api/v1/resumes` collection and its analysis endpoint.

- Candidates can upload PDF, DOCX, or TXT files up to 5 MB through a file picker or drag-and-drop target.
- Browser validation provides immediate type, size, and empty-file feedback; the backend remains authoritative and verifies both the extension and detected content.
- The history view shows private file metadata, analysis state, score, and the current selection without providing a file-download route.
- Analysis requires a target role, initially populated from the candidate profile, and accepts an optional job description up to 20,000 characters.
- Gemini results render the server-provided ATS score, summary, strengths, weaknesses, missing keywords, suggestions, model, and analysis time.
- Reanalysis replaces the stored result through the existing backend workflow. The UI never calculates or modifies the ATS score.
- File and analysis deletion requires explicit confirmation and calls the ownership-protected delete endpoint.

Uploaded content and AI-originated text are rendered only with safe DOM text nodes. Access tokens remain in memory, refresh credentials remain in the HttpOnly cookie, and multipart requests are sent only to the same-origin API.
