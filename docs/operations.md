# Operations and Deployment

## Runtime contract

The production service requires Java 17, MySQL 8.4, a secret `JWT_SECRET` of at least 32 characters, and a Gemini API key for AI features. Code execution remains disabled unless `CODE_RUNNER_BASE_URL` points to an independently isolated runner. Resume files must use a private durable volume or object-storage adapter.

Copy `.env.example` to `.env`, replace every placeholder, and start the stack:

```bash
docker compose up --build -d
docker compose ps
curl --fail http://localhost:8080/actuator/health
```

The application container runs as an unprivileged user with a read-only root filesystem. Only `/tmp` and the resume volume are writable. MySQL and resume volumes require independent encrypted backups.

Container deployments activate the `prod` Spring profile by default. It enables secure cookies by default, response compression, bounded request threads, and configurable Hikari pool limits. The local `.env.example` explicitly keeps `AUTH_COOKIE_SECURE=false`; change it to `true` for every HTTPS environment.

The web client is served by the same Spring Boot origin. Set `AUTH_COOKIE_SECURE=true` whenever the public site uses HTTPS. Keep it `false` only for plain-HTTP local development; secure cookies are not returned by browsers over HTTP.

## Administrator bootstrap

Registration grants only `ROLE_USER`. Promote an existing, verified operator directly in MySQL, then log in again so the new access JWT contains the administrator role:

```sql
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT users.id, roles.id
FROM users
JOIN roles ON roles.name = 'ROLE_ADMIN'
WHERE users.email = 'admin@example.com';
```

Use a dedicated administrator identity with a strong unique password. Administrator APIs expose hidden coding test cases and must never be made available to normal users.

## Health, metrics, and correlation

- `GET /actuator/health`, `/actuator/health/liveness`, and `/actuator/health/readiness` are public for orchestrator probes and never expose component details.
- `GET /actuator/prometheus` requires an access JWT with `ROLE_ADMIN`.
- `X-Correlation-ID` is accepted only when it contains 8–64 alphanumeric or hyphen characters; otherwise the service creates a UUID. The identifier is returned on every response and attached to audit events.
- Custom metrics include `rate_limit_rejections_total` by scope and `admin_audit_events_total` by action.

Do not put tokens in Prometheus scrape configuration. In production, place metrics behind a private network and authenticate through a controlled proxy or a dedicated internal security mechanism.

## Rate limiting

The local fixed-window limiter has separate general, authentication, and AI-heavy quotas. Configure it with `RATE_LIMIT_*` values from `.env.example`. Limits are keyed by the effective client address, so trusted-proxy forwarding must be configured carefully.

The in-memory store is deliberately bounded and appropriate for one application instance. Multiple replicas must use a shared gateway/Redis limiter to enforce a consistent platform-wide quota. A rejected request returns HTTP 429 with `Retry-After`, `X-RateLimit-Limit`, and `X-RateLimit-Remaining` headers.

## Account suspension and audit retention

Suspension immediately prevents login and refresh and revokes all active refresh sessions. Stateless access JWTs already issued to that user can remain usable until their short expiry (15 minutes by default); reduce `JWT_ACCESS_TTL` or add a shared token-denylist if immediate access-token invalidation is required.

Audit records are append-only through the application. Restrict direct database write access, retain records according to policy, and export them to immutable centralized storage for regulated environments.

## Production checklist

- Terminate TLS at a trusted reverse proxy and allow only HTTPS externally.
- Rotate JWT, Gemini, database, and runner secrets through a secrets manager.
- Use managed MySQL with encryption, automated backups, restore drills, and alerting.
- Store resumes in malware-scanned private object storage with short-lived access.
- Run the code runner on separate infrastructure with CPU, memory, process, filesystem, time, and outbound-network restrictions.
- Alert on health failures, rate-limit rejections, elevated 5xx responses, database saturation, and Gemini/runner latency.
- Drain traffic during graceful shutdown and keep the readiness probe enabled.

Use the complete [release-readiness checklist](release-readiness.md) for automated gates, immutable artifact verification, staging promotion, observation, rollback, and manual launch sign-off.
