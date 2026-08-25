# Project-owned code runner

This service implements the backend's `POST /v1/execute` contract for Java 17,
Python 3.13, JavaScript (Node 24), and C++20. It is included in
`docker-compose.yml`; no external code-execution provider is required.

Each hidden test runs in a new container with:

- no network
- a read-only root filesystem and bounded temporary filesystem
- all Linux capabilities dropped and `no-new-privileges`
- CPU, memory, process, wall-clock, request-size, source-size, and output limits
- an unprivileged runtime user

At startup the controller verifies Docker access and pulls any missing
versioned official language-runtime tags. The first startup can therefore take
several minutes.

## Windows local use

1. Start Docker Desktop and use Linux containers.
2. Copy `.env.example` to `.env`, then set strong values for `JWT_SECRET`,
   `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`, and `CODE_RUNNER_API_KEY`.
3. From PowerShell in the repository root, run:

   ```powershell
   docker compose down
   docker compose up --build -d
   docker compose logs -f code-runner
   ```

4. Wait for `docker compose ps` to show `mysql`, `code-runner`, and `backend`
   as healthy/running. Open `http://localhost:8080/coding` and submit a solution.

If port 3307 is already used, choose another free host port in `.env`, for
example `MYSQL_HOST_PORT=3308`. Containers still communicate with MySQL on its
internal port 3306.

## Trust boundary

The controller mounts the Docker daemon socket so it can create disposable
runtime containers. Treat it as infrastructure: keep port 8090 internal, use a
strong API token, and never expose it directly to the internet. This Compose
topology is intended for local development and a single trusted staging host.
For multi-tenant production, run the controller on a dedicated worker host or
replace the Docker-socket boundary with a hardened sandbox/orchestrator.
