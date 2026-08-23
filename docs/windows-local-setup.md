# Download and Run on Windows

Yes—you can download the complete project and run it on a Windows 11 laptop. Docker is the simplest option because it provides the correct MySQL and Java runtime automatically.

## Option A: Git and Docker Desktop (recommended)

Install:

- [Git for Windows](https://git-scm.com/download/win)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)

Open PowerShell and clone the repository:

```powershell
cd $HOME\Desktop
git clone https://github.com/Sourav12kumar/AI-Interview-Preparation-Platform.git
cd AI-Interview-Preparation-Platform
Copy-Item .env.example .env
```

Create a secure JWT secret:

```powershell
$bytes = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Open `.env` in VS Code or Notepad. Replace at least:

```dotenv
DB_PASSWORD=choose_a_database_password
MYSQL_ROOT_PASSWORD=choose_a_different_root_password
JWT_SECRET=paste_the_generated_secret_here
GEMINI_API_KEY=paste_your_Google_AI_Studio_key_here
AUTH_COOKIE_SECURE=false
```

Keep `AUTH_COOKIE_SECURE=false` only for local HTTP. Start the complete application:

```powershell
docker compose up --build
```

Wait until MySQL is healthy and Spring Boot reports that it started. Open:

- Web application: `http://localhost:8080`
- Health check: `http://localhost:8080/actuator/health`

Stop containers with `Ctrl+C`, then:

```powershell
docker compose down
```

Your MySQL database and uploaded resume data remain in Docker volumes. To also delete that local data, run `docker compose down --volumes` only when you intentionally want a clean reset.

## Option B: Java and Maven

Install Java 17, Maven 3.9+, Git, and MySQL 8.4. Verify:

```powershell
java -version
mvn -version
git --version
```

The project is configured for Java 17. Avoid mixing a Java 8 compiler environment with JDK 21, which can cause the compiler-compliance problem you encountered in other Java projects.

Clone and configure the project as shown above. You may run only MySQL with Docker:

```powershell
docker compose up -d mysql
```

Load `.env` values into the current PowerShell process:

```powershell
Get-Content .env |
  Where-Object { $_ -match '^[A-Za-z_][A-Za-z0-9_]*=' } |
  ForEach-Object {
    $name, $value = $_.Split('=', 2)
    Set-Item -Path "Env:$name" -Value $value
  }
```

Run tests and start Spring Boot:

```powershell
mvn clean verify
mvn spring-boot:run
```

## Download ZIP without Git

Open the [GitHub repository](https://github.com/Sourav12kumar/AI-Interview-Preparation-Platform), select **Code**, then **Download ZIP**. Extract it, copy `.env.example` to `.env`, configure the secrets, and use either Docker or Maven. Git clone is preferable because future updates need only:

```powershell
git pull origin main
```

## Gemini setup

Create a key in Google AI Studio and place it only in `.env` as `GEMINI_API_KEY`. Never paste the key into Java, JavaScript, YAML committed to GitHub, screenshots, or chat messages. The application can start without the key, but interview generation, answer evaluation, resume analysis, and AI recommendations will return a configuration error until it is set.

## Optional code runner

The Spring application never executes candidate code itself. Coding submissions require a separate isolated service configured with `CODE_RUNNER_BASE_URL` and `CODE_RUNNER_API_KEY`. You can leave both empty while running authentication, profiles, interviews, analytics, and resume features.

## Common problems

| Problem | Check |
|---|---|
| `JWT secret must contain at least 32 characters` | Generate and set a longer `JWT_SECRET` in `.env`. |
| MySQL connection refused | Wait for the MySQL health check or verify port `3306` is free. |
| Port `8080` already in use | Stop the other service or set `SERVER_PORT` to another port. |
| Interview generation returns 502 | Verify `GEMINI_API_KEY` and internet access. |
| Browser immediately returns to login | Keep `AUTH_COOKIE_SECURE=false` on local HTTP and clear old localhost cookies. |
| Docker is not recognized | Start Docker Desktop and reopen PowerShell. |

Do not commit `.env`; it is already excluded by `.gitignore`.
