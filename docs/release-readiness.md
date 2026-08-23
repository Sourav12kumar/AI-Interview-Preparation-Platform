# Release Readiness

This document is the repeatable promotion checklist for InterviewPilot. A release candidate is deployable only when the backend, browser, accessibility, container, and security gates are green for the exact commit being promoted.

## Automated release gates

| Gate | Workflow | Required evidence |
|---|---|---|
| Java and frontend validation | `Backend CI / test` | Maven verification and syntax validation pass |
| Production image | `Backend CI / container` | Docker image builds from the same commit |
| Browser and accessibility | `Backend CI / browser` | Chromium smoke journey and automated WCAG A/AA scans pass against MySQL |
| Static security analysis | `CodeQL` | Java/Kotlin and JavaScript/TypeScript analyses complete without an unresolved release-blocking alert |
| Release package | `Release Candidate` | JAR, example environment, checklist, and SHA-256 checksum are uploaded together |

Browser failures retain the application log, Playwright HTML report, traces, screenshots, and videos for 14 days. Do not approve a flaky retry without reviewing the first-attempt evidence.

## Runtime configuration

Run the application with `SPRING_PROFILES_ACTIVE=prod`. The production profile enables secure browser cookies by default, response compression, bounded Tomcat threads, and an explicitly sized Hikari connection pool.

Required secrets must be injected by the deployment platform and never copied into an image, workflow, log, or repository:

- `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`
- `JWT_SECRET` with at least 32 high-entropy characters
- `GEMINI_API_KEY`
- `CODE_RUNNER_API_KEY` when coding execution is enabled

Set `AUTH_COOKIE_SECURE=true` behind HTTPS. Configure `CODE_RUNNER_BASE_URL` only for a separately isolated runner. Use a private durable `RESUME_STORAGE_DIR` or replace the filesystem adapter with private object storage before running more than one application replica.

Optional production capacity controls are `DB_POOL_MAX_SIZE`, `DB_POOL_MIN_IDLE`, `DB_CONNECTION_TIMEOUT_MS`, `DB_VALIDATION_TIMEOUT_MS`, `SERVER_MAX_THREADS`, and `SERVER_MIN_SPARE_THREADS`. Size the database pool across all replicas, not per replica in isolation.

## Promotion procedure

1. Confirm the target commit is on `main` and every required workflow is green.
2. Review open dependency and CodeQL alerts; record an owner and expiry for any accepted risk.
3. Back up MySQL and verify the latest restore drill before applying a migration-bearing release.
4. Trigger `Release Candidate` manually for a staging build, or push a signed `v*` tag for a versioned build.
5. Download the artifact and verify it with `sha256sum --check SHA256SUMS` from inside the artifact directory.
6. Deploy the exact commit/image digest to staging with production-like MySQL, TLS, cookie, proxy, storage, Gemini, and runner configuration.
7. Verify `/actuator/health/liveness`, `/actuator/health/readiness`, registration/login/logout, profile persistence, one Gemini interview, one resume analysis, and one isolated code submission.
8. Promote the same immutable image digest to production using a rolling or blue/green deployment.
9. Watch readiness, 5xx rate, authentication failures, database saturation, Gemini latency/errors, runner latency/errors, and rate-limit rejections during the observation window.
10. Record the deployed digest, migration version, operator, timestamp, and links to workflow evidence.

## Rollback and recovery

Application rollback must reuse the previously approved image digest. Flyway migrations are forward-only, so a release containing a non-backward-compatible schema change requires a tested roll-forward repair plan before deployment. Never edit an applied migration.

If health or critical journeys fail:

1. stop further promotion and drain the failing replicas;
2. preserve logs, correlation IDs, audit events, and workflow evidence;
3. restore the prior image when the schema remains backward compatible;
4. otherwise execute the reviewed roll-forward repair;
5. restore MySQL only for confirmed data corruption, using the documented backup and point-in-time recovery procedure;
6. rotate any secret that may have appeared in diagnostics.

## Manual sign-off

- TLS certificate, HTTPS redirect, HSTS, and trusted proxy forwarding verified
- Secure, HttpOnly, SameSite refresh cookie verified in the browser
- Database encryption, backups, point-in-time recovery, and restore ownership verified
- Resume storage encryption, access policy, malware scanning, retention, and deletion verified
- Code runner CPU, memory, process, filesystem, timeout, and outbound-network isolation verified
- Gemini quota, safety, timeout, error, and data-retention behavior verified
- Administrator bootstrap, least privilege, audit export, and emergency access verified
- Dashboards, alerts, on-call routing, incident runbook, rollback owner, and observation window confirmed
- Privacy notice, terms, data deletion, and retention policy reviewed for the launch region

The automated axe scan finds many common accessibility defects, but it does not replace keyboard-only, screen-reader, zoom/reflow, focus-order, and human usability review. Complete those manual checks before a public launch.
