# QuickPoll — Deployable Spring Boot Demo

A deliberately small Spring Boot 4 / Java 25 web app used to demonstrate **every deployment method** covered in the *Java Web Application Deployment* lecture.

The app itself is a tiny polling site: create a question, add 2–4 options, share the URL, see live vote counts.

What makes this project interesting is **not the app** — it's the fact that the **same codebase** can be shipped in seven different ways without changing a single line of Java:

1. Runnable JAR with embedded Tomcat (`java -jar`)
2. WAR deployed to a standalone Apache Tomcat
3. Local share via Ngrok tunnel
4. Docker image (single container)
5. Docker Compose (app + PostgreSQL)
6. Cloud platforms — Render, Railway, Fly.io, Koyeb, Google Cloud Run, AWS Elastic Beanstalk, Azure App Service, Oracle Cloud
7. Managed databases — Supabase, Neon, MongoDB Atlas, and friends

The detailed walk-through for each method lives in [`DEPLOYMENT.md`](./DEPLOYMENT.md).

---

## Quick start (IntelliJ IDEA)

1. **Open** the folder `quickpoll-deploy` in IntelliJ IDEA (File → Open).
2. Wait for Maven to import the dependencies.
3. Right-click `QuickpollApplication.java` → **Run**.
4. Open <http://localhost:8080>.

The default profile is `dev`, which uses an H2 in-memory database — zero setup, data is reset on every restart.

## Quick start (command line)

```bash
./mvnw clean package
java -jar target/quickpoll.jar
```

Or with Docker:

```bash
docker compose up --build
```

---

## Project structure

```
quickpoll-deploy/
├── pom.xml                          ← Maven config + JAR/WAR profiles
├── Dockerfile                       ← multi-stage build
├── docker-compose.yml               ← app + PostgreSQL
├── render.yaml                      ← Render Blueprint
├── fly.toml                         ← Fly.io config
├── railway.toml                     ← Railway config
├── Procfile                         ← Heroku-style PaaSes
├── system.properties                ← pins JDK 25 for buildpacks
├── app.yaml                         ← Google Cloud Run / App Engine
├── DEPLOYMENT.md                    ← detailed guide for every method
├── .github/workflows/ci.yml         ← CI builds JAR, WAR, Docker
└── src/
    ├── main/
    │   ├── java/hr/algebra/quickpoll/
    │   │   ├── QuickpollApplication.java
    │   │   ├── ServletInitializer.java        ← required for WAR
    │   │   ├── controller/
    │   │   │   ├── PollWebController.java     ← Thymeleaf UI
    │   │   │   └── PollRestController.java    ← /api/* JSON
    │   │   ├── model/                         ← JPA entities
    │   │   ├── repository/                    ← Spring Data
    │   │   ├── service/                       ← business logic
    │   │   └── config/DataInitializer.java    ← seeds demo polls
    │   └── resources/
    │       ├── application.properties         ← base
    │       ├── application-dev.properties     ← H2 (default)
    │       ├── application-prod.properties    ← PostgreSQL via env vars
    │       ├── application-docker.properties  ← PostgreSQL via compose
    │       ├── templates/                     ← Thymeleaf pages
    │       └── static/css/style.css
    └── test/java/hr/algebra/quickpoll/
        └── QuickpollApplicationTests.java
```

## Endpoints

| Path | What |
|---|---|
| `/` | List polls (Thymeleaf) |
| `/polls/new` | Create new poll form |
| `/polls/{id}` | Vote + see results |
| `/api/polls` | JSON list of polls |
| `/api/polls/{id}` | JSON single poll |
| `/api/polls/{id}/vote?optionId=X` | Cast vote (POST) |
| `/actuator/health` | Health probe (used by every cloud platform) |
| `/h2-console` | H2 web console (dev profile only) |

## Spring profiles

| Profile | Database | Used by |
|---|---|---|
| `dev` (default) | H2 in-memory | IntelliJ, local dev, JAR runs without env vars |
| `prod` | PostgreSQL via `DATABASE_URL` or `SPRING_DATASOURCE_*` | All cloud platforms |
| `docker` | PostgreSQL service named `db` | `docker-compose` only |

Switch profiles with `--spring.profiles.active=prod` or the env var `SPRING_PROFILES_ACTIVE=prod`.

---

## What's next?

Open [`DEPLOYMENT.md`](./DEPLOYMENT.md) and pick a deployment path. Each section is self-contained — you don't need to read them in order.

Made for the **Java Web Programming** course at Algebra Bernays University by *doc. dr. sc. Aleksander Radovan*.
