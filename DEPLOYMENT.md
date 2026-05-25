# QuickPoll — Complete Deployment Guide

This document walks through **every deployment method** covered in the lecture, applied to the QuickPoll project. Sections are self-contained — pick whichever path you need.

## Contents

1. [Local development from IntelliJ](#1-local-development-from-intellij)
2. [Building an executable JAR (`java -jar`)](#2-building-an-executable-jar-java--jar)
3. [Sharing your local server with Ngrok](#3-sharing-your-local-server-with-ngrok)
4. [Building a WAR and deploying to Apache Tomcat](#4-building-a-war-and-deploying-to-apache-tomcat)
5. [Building and running a Docker image](#5-building-and-running-a-docker-image)
6. [Running the full stack with Docker Compose](#6-running-the-full-stack-with-docker-compose)
7. [Deploying to Render.com](#7-deploying-to-rendercom)
8. [Deploying to Railway](#8-deploying-to-railway)
9. [Deploying to Fly.io](#9-deploying-to-flyio)
10. [Deploying to Koyeb](#10-deploying-to-koyeb)
11. [Deploying to Google Cloud Run](#11-deploying-to-google-cloud-run)
12. [Deploying to AWS Elastic Beanstalk](#12-deploying-to-aws-elastic-beanstalk)
13. [Deploying to Azure App Service](#13-deploying-to-azure-app-service)
14. [Deploying to Oracle Cloud (OCI) — Always Free tier](#14-deploying-to-oracle-cloud-oci--always-free-tier)
15. [Connecting to a managed database (Supabase, Neon, Aiven)](#15-connecting-to-a-managed-database)
16. [Troubleshooting](#16-troubleshooting)

---

## 1. Local development from IntelliJ

The simplest possible workflow — everything runs in-process, the database is in-memory.

### Prerequisites

- JDK 25 (Eclipse Temurin recommended): <https://adoptium.net>
- IntelliJ IDEA — Community Edition is enough
- Internet (first Maven dependency download only)

### Steps

1. **Open the project**
   `File → Open` and select the `quickpoll-deploy` folder. IntelliJ detects the `pom.xml` and imports the project. Wait for the indexing bar at the bottom to finish.

2. **Verify the JDK**
   `File → Project Structure → Project → SDK` must say *25*. If it doesn't, click *Add SDK → Download JDK* and pick Temurin 25.

3. **Run**
   Open `src/main/java/hr/algebra/quickpoll/QuickpollApplication.java`, click the green ▶ icon next to `public static void main`, choose *Run 'QuickpollApplication'*.

4. **Open the app**
   <http://localhost:8080> — you'll see two seeded demo polls.

### What just happened?

- Spring Boot started **embedded Tomcat** on port 8080.
- Hibernate created the H2 schema in memory (`create-drop` mode).
- `DataInitializer` seeded two polls.
- The `dev` profile is active by default (see `application.properties`).

### Useful URLs while developing

| URL | Purpose |
|---|---|
| <http://localhost:8080> | The app itself |
| <http://localhost:8080/h2-console> | H2 web console (JDBC URL: `jdbc:h2:mem:quickpoll`, user `sa`, no password) |
| <http://localhost:8080/actuator/health> | Liveness probe |
| <http://localhost:8080/api/polls> | JSON API |

---

## 2. Building an executable JAR (`java -jar`)

This is the **canonical Spring Boot way** of running an app — one self-contained file that includes embedded Tomcat.

### Build

```bash
# from the project root
./mvnw clean package
```

Maven produces `target/quickpoll.jar` (~25 MB). Inside that file: your compiled classes, all dependencies, and an embedded Tomcat.

### Run

```bash
java -jar target/quickpoll.jar
```

Open <http://localhost:8080>.

### Pointing at a real database

Override the profile and the connection at startup:

```bash
java -jar target/quickpoll.jar \
  --spring.profiles.active=prod \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/quickpoll \
  --spring.datasource.username=quickpoll \
  --spring.datasource.password=quickpoll
```

…or via environment variables, which is what cloud platforms do:

```bash
SPRING_PROFILES_ACTIVE=prod \
DATABASE_URL=jdbc:postgresql://localhost:5432/quickpoll \
DATABASE_USERNAME=quickpoll \
DATABASE_PASSWORD=quickpoll \
java -jar target/quickpoll.jar
```

### Common JVM tuning flags

```bash
java -Xms256m -Xmx512m \
     -XX:+UseG1GC \
     -Dserver.port=9090 \
     -jar target/quickpoll.jar
```

---

## 3. Sharing your local server with Ngrok

You want to demo the app to a classmate, the lecturer, or a client — but they're not on your network. Ngrok exposes your `localhost:8080` over HTTPS to the public internet.

### Prerequisites

1. Start QuickPoll locally (see section 1 or 2).
2. Install Ngrok: <https://ngrok.com/download>
3. Sign up for a free account, then authenticate the CLI once:
   ```bash
   ngrok config add-authtoken <YOUR_TOKEN>
   ```

### Run

In a second terminal:

```bash
ngrok http 8080
```

Ngrok prints something like:

```
Forwarding   https://abcd-1234.ngrok-free.app -> http://localhost:8080
```

Share that HTTPS URL. Done.

### Caveats

- Free tier: the URL changes on every restart. Reserved subdomains are paid.
- All traffic goes through Ngrok's servers — **never** expose anything sensitive this way.
- Spring Boot may reject the request with `Bad Request: This server cannot accept the request because the Host HTTP header is not valid`. Fix by adding to `application.properties`:
  ```properties
  server.forward-headers-strategy=framework
  ```

---

## 4. Building a WAR and deploying to Apache Tomcat

The traditional Java EE way — build a WAR file and drop it into a host servlet container. Still used by lots of enterprise environments.

### Build the WAR

```bash
./mvnw clean package -Pwar
```

The `war` Maven profile (defined in `pom.xml`) does two things:
- Sets `<packaging>war</packaging>`.
- Marks `spring-boot-starter-tomcat` as `provided` — so the JAR's embedded Tomcat is **not** bundled inside the WAR. Otherwise it would collide with the host Tomcat.

Output: `target/quickpoll.war` (~22 MB).

### Install Apache Tomcat

Download Tomcat 11 (servlet API 6.1, required by Spring Boot 4): <https://tomcat.apache.org/download-11.cgi>

Extract to e.g. `~/apache-tomcat-11.0.0`.

### Deploy the WAR

**Option A — drop into `webapps/`:**

```bash
cp target/quickpoll.war ~/apache-tomcat-11.0.0/webapps/
~/apache-tomcat-11.0.0/bin/startup.sh   # or startup.bat on Windows
```

Tomcat auto-deploys the WAR. Watch the console — when you see `Deployment of web application archive [...] has finished` the app is live at:

**<http://localhost:8080/quickpoll>**

Notice the URL includes `/quickpoll` — that's the **context path** derived from the WAR filename.

**Option B — deploy to root context:**

Rename the WAR to `ROOT.war` before copying. Then the app is at `http://localhost:8080/` with no path prefix.

**Option C — Tomcat Manager:**

If you've set up `<role rolename="manager-gui"/>` in `conf/tomcat-users.xml`, go to <http://localhost:8080/manager/html> and upload the WAR via the web form.

### Database for WAR deployments

The `prod` profile expects PostgreSQL. Set the connection by editing `~/apache-tomcat-11.0.0/bin/setenv.sh` (create it if missing):

```bash
#!/bin/sh
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL="jdbc:postgresql://localhost:5432/quickpoll"
export DATABASE_USERNAME=quickpoll
export DATABASE_PASSWORD=quickpoll
export DEPLOYMENT_LABEL="apache-tomcat-11"
```

Restart Tomcat. The Spring Boot app picks these up through `application-prod.properties`.

### Stop

```bash
~/apache-tomcat-11.0.0/bin/shutdown.sh
```

---

## 5. Building and running a Docker image

### Prerequisites

Install Docker Desktop (macOS/Windows) or Docker Engine (Linux): <https://docs.docker.com/get-docker/>

Verify:

```bash
docker --version
docker info
```

### Build the image

```bash
docker build -t quickpoll:1.0.0 .
```

What happens:

1. **Stage 1 (build):** Maven runs inside `maven:3.9-eclipse-temurin-25`, downloads dependencies, compiles, packages the JAR.
2. **Stage 2 (runtime):** copies just the JAR into `eclipse-temurin:25-jre-alpine` — a tiny image (~180 MB).

First build takes ~3 minutes (Maven downloads dependencies). Subsequent builds: ~30 seconds (cached layers).

### Inspect

```bash
docker images | grep quickpoll
docker history quickpoll:1.0.0   # see layer sizes
```

### Run

```bash
docker run -d \
  --name quickpoll \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  quickpoll:1.0.0
```

Flags explained:
- `-d` — detached (background).
- `--name quickpoll` — friendly container name.
- `-p 8080:8080` — map host port → container port.
- `-e SPRING_PROFILES_ACTIVE=dev` — pass env variable.

Open <http://localhost:8080>.

### Logs and shell access

```bash
docker logs -f quickpoll
docker exec -it quickpoll sh
```

### Stop and remove

```bash
docker stop quickpoll
docker rm quickpoll
```

### Alternative: Buildpacks

Spring Boot can build an OCI image without a Dockerfile:

```bash
./mvnw spring-boot:build-image
```

Produces `quickpoll:1.0.0` automatically — useful when you don't want to maintain a Dockerfile.

---

## 6. Running the full stack with Docker Compose

The Dockerfile only runs the **app**. With H2 that's fine, but for a real PostgreSQL you'd need a separate container. Docker Compose orchestrates both.

### Run

```bash
docker compose up --build
```

What happens:
1. PostgreSQL 16 starts and becomes healthy (compose waits for `pg_isready`).
2. The Spring Boot app starts, sees `SPRING_PROFILES_ACTIVE=docker`, connects to `db:5432`.
3. Hibernate runs schema updates against PostgreSQL.

Open <http://localhost:8080>.

### Daily commands

```bash
docker compose up -d             # background
docker compose logs -f app       # follow app logs only
docker compose ps                # what's running
docker compose down              # stop, keep data volume
docker compose down -v           # stop AND wipe the database
```

### Connecting from your IDE to the PostgreSQL container

```
JDBC URL:  jdbc:postgresql://localhost:5432/quickpoll
User:      quickpoll
Password:  quickpoll
```

DBeaver / DataGrip will see your `polls` and `poll_options` tables.

### Why this matters

The `app` service in compose uses the **same image** you'd push to production. Compose just adds a local PostgreSQL. This is the closest you can get to production locally.

---

## 7. Deploying to Render.com

The recommended entry point — generous free tier, native Docker support, zero CLI tools required.

### Steps

1. **Push the project to GitHub.**
   ```bash
   git init
   git add .
   git commit -m "Initial QuickPoll"
   git branch -M main
   git remote add origin git@github.com:<your-user>/quickpoll.git
   git push -u origin main
   ```

2. **Create a Render account** at <https://render.com> — sign in with GitHub.

3. **New + → Blueprint.** Render reads the `render.yaml` in the repo root. It creates:
   - A free PostgreSQL database called `quickpoll-db`
   - A free web service called `quickpoll` built from the `Dockerfile`
   - Wires `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` from the DB into the web service automatically.

4. **Apply.** First build takes ~5 minutes. Watch the *Logs* tab.

5. **Open the URL** Render assigns — something like `https://quickpoll-xxxx.onrender.com`.

### Free tier caveats

- Free web services **spin down after 15 min of inactivity** — first request after that takes ~30 s. Acceptable for demos, not production.
- Free PostgreSQL **expires after 90 days** and must be recreated. Upgrade to a paid DB for anything serious.
- Custom domains + free TLS are included.

### Without `render.yaml`

If you don't want a Blueprint:
1. Render dashboard → *New +* → *Web Service* → connect repo.
2. Environment: *Docker*.
3. Add a PostgreSQL: *New +* → *PostgreSQL*.
4. In the web service's *Environment* tab, add:
   - `SPRING_PROFILES_ACTIVE=prod`
   - `DATABASE_URL` ← copy from the database's *Connections* page.

---

## 8. Deploying to Railway

Similar to Render but **credit-based** instead of always-free. New accounts get $5 of credit/month which is enough for a small Java app + Postgres.

### Steps

1. Install the CLI:
   ```bash
   npm install -g @railway/cli
   railway login
   ```

2. From the project root:
   ```bash
   railway init               # create new project
   railway add --database postgres
   railway up                 # build + deploy
   ```

   Railway detects the `Dockerfile` automatically (via `railway.toml`).

3. Wire the Postgres:
   ```bash
   railway variables --set "SPRING_PROFILES_ACTIVE=prod"
   railway variables --set "DEPLOYMENT_LABEL=railway"
   ```

   Railway injects `DATABASE_URL` automatically when the Postgres plugin is attached.

4. Get your URL:
   ```bash
   railway domain
   ```

### Useful commands

```bash
railway logs           # tail logs
railway status         # what's running
railway open           # open dashboard
```

---

## 9. Deploying to Fly.io

Fly runs your container in **micro-VMs** in many regions worldwide. No cold starts after the first deploy (machines can auto-stop, but they boot in ~250 ms).

### Steps

1. Install flyctl: <https://fly.io/docs/flyctl/install/>
2. Sign up & log in:
   ```bash
   fly auth signup
   ```
3. From the project root (fly.toml is already there):
   ```bash
   fly launch --no-deploy --copy-config
   ```
   Confirm app name and region (we set `fra` — Frankfurt, closest to Zagreb).

4. **Add Postgres:**
   ```bash
   fly postgres create --name quickpoll-db --region fra --vm-size shared-cpu-1x --volume-size 1
   fly postgres attach quickpoll-db --app quickpoll
   ```
   The `attach` command sets `DATABASE_URL` automatically.

5. **Deploy:**
   ```bash
   fly deploy
   ```

6. **Open:**
   ```bash
   fly open
   ```

### Useful commands

```bash
fly status            # machines, IPs, regions
fly logs              # live logs
fly ssh console       # SSH into the running machine
fly scale count 2     # scale to 2 instances
```

### Caveats

- Pay-as-you-go pricing — there is no "free forever" tier, only a $5/month *trial* credit on new accounts.
- Set a hard spending limit in the dashboard: *Billing → Spending Limit*.

---

## 10. Deploying to Koyeb

Often the best **truly always-free** option — one nano service free forever (512 MB RAM, 100 ms vCPU, no cold starts).

### Steps

1. Sign up at <https://app.koyeb.com> (GitHub login).
2. *Create App* → *Deploy from GitHub*.
3. Select your repo. Koyeb auto-detects the `Dockerfile` and the `Procfile`.
4. **Configure:**
   - Instance type: *Nano* (free)
   - Region: *Frankfurt (fra)*
   - Environment variables:
     - `SPRING_PROFILES_ACTIVE` = `prod`
     - `DEPLOYMENT_LABEL` = `koyeb`
     - `DATABASE_URL` = (paste from a managed DB — see section 15)
   - Health check: path `/actuator/health`, port `8080`.
5. *Deploy*. First build: ~4 minutes.

### Database

Koyeb itself does **not** offer managed databases on the free tier. Pair it with **Neon** or **Supabase** (see section 15).

---

## 11. Deploying to Google Cloud Run

Serverless containers — pay per request, scale to zero, scale up to thousands. Generous free tier: 2 million requests/month free forever.

### Steps

1. Install gcloud: <https://cloud.google.com/sdk/docs/install>
2. Authenticate:
   ```bash
   gcloud auth login
   gcloud projects create quickpoll-demo --set-as-default
   gcloud config set project quickpoll-demo
   ```
3. Enable required services:
   ```bash
   gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com
   ```
4. **Build and push the image to Google Artifact Registry:**
   ```bash
   gcloud builds submit --tag gcr.io/quickpoll-demo/quickpoll
   ```
   This uploads source, builds the Dockerfile on Google's infrastructure, pushes the image — all in one step.

5. **Deploy:**
   ```bash
   gcloud run deploy quickpoll \
     --image gcr.io/quickpoll-demo/quickpoll \
     --platform managed \
     --region europe-west3 \
     --allow-unauthenticated \
     --set-env-vars SPRING_PROFILES_ACTIVE=prod,DEPLOYMENT_LABEL=cloud-run \
     --memory 512Mi \
     --cpu 1
   ```

6. gcloud prints the service URL. Open it.

### Database

Use **Cloud SQL for PostgreSQL** (no free tier — minimum ~$10/month) or pair Cloud Run with a free Neon/Supabase database.

Set the connection via env vars in step 5:
```
--set-env-vars DATABASE_URL=jdbc:postgresql://...,DATABASE_USERNAME=...,DATABASE_PASSWORD=...
```

### Caveats

- Cold start: ~3 s for the first request after idle. Set `--min-instances 1` to keep one warm (costs money).
- Cloud Run requires the app to listen on the `PORT` env var — Spring Boot does this automatically because `application.properties` says `server.port=${PORT:8080}`.

---

## 12. Deploying to AWS Elastic Beanstalk

Beanstalk is AWS's PaaS abstraction over EC2/RDS — drop in a JAR and AWS handles servers, load balancers, auto-scaling.

### Steps

1. Install the AWS CLI + EB CLI:
   ```bash
   pip install awsebcli
   aws configure   # paste your access key + secret
   ```

2. **Build the JAR:**
   ```bash
   ./mvnw clean package
   ```

3. **Initialize Beanstalk:**
   ```bash
   eb init -p "Corretto 21" -r eu-central-1 quickpoll
   ```
   At time of writing Beanstalk doesn't offer a "Corretto 25" platform yet — Corretto 21 runs the JAR fine, but if you need Java 25, use the Docker platform instead (`-p docker`).

4. **Create the environment:**
   ```bash
   eb create quickpoll-env \
     --instance-type t3.small \
     --envvars SPRING_PROFILES_ACTIVE=prod,DEPLOYMENT_LABEL=beanstalk
   ```
   ~5 minutes.

5. **Provision RDS PostgreSQL:**
   Console: *RDS → Create database → PostgreSQL → Free tier* (db.t4g.micro).
   Note the endpoint, then:
   ```bash
   eb setenv \
     DATABASE_URL=jdbc:postgresql://your-endpoint:5432/quickpoll \
     DATABASE_USERNAME=quickpoll \
     DATABASE_PASSWORD=...
   ```

6. **Deploy:**
   ```bash
   eb deploy
   ```

7. **Open:**
   ```bash
   eb open
   ```

### Free tier

12 months of EC2 t2.micro + 12 months of db.t4g.micro RDS, then you pay. Always set up a billing alarm.

---

## 13. Deploying to Azure App Service

Azure's PaaS for web apps — best when your client/employer is already on Microsoft 365 or Azure AD.

### Steps

1. Install Azure CLI: <https://learn.microsoft.com/cli/azure/install-azure-cli>
2. Authenticate:
   ```bash
   az login
   ```

3. **Create resource group + plan + app:**
   ```bash
   az group create --name quickpoll-rg --location westeurope

   az appservice plan create --name quickpoll-plan \
     --resource-group quickpoll-rg \
     --sku F1 --is-linux

   az webapp create --name quickpoll \
     --resource-group quickpoll-rg \
     --plan quickpoll-plan \
     --runtime "JAVA:21-java21"
   ```
   The F1 SKU is free forever (60 CPU min/day, sufficient for a demo).

4. **Build the JAR + deploy:**
   ```bash
   ./mvnw clean package
   az webapp deploy --resource-group quickpoll-rg --name quickpoll \
     --src-path target/quickpoll.jar --type jar
   ```

5. **Provision Azure Database for PostgreSQL** (Burstable B1ms tier, ~12 EUR/month — no free PostgreSQL on Azure):
   Console: *Azure Database for PostgreSQL flexible server → Create*.

6. **Wire env vars:**
   ```bash
   az webapp config appsettings set \
     --resource-group quickpoll-rg --name quickpoll \
     --settings SPRING_PROFILES_ACTIVE=prod \
                DEPLOYMENT_LABEL=azure \
                DATABASE_URL="jdbc:postgresql://..." \
                DATABASE_USERNAME=... \
                DATABASE_PASSWORD=...
   ```

7. **Open:**
   ```
   https://quickpoll.azurewebsites.net
   ```

### Free tier caveats

- F1 has a 60 CPU-minutes/day quota. Beyond that, the app returns HTTP 403.
- Custom domains require at least the B1 plan (~10 EUR/month).

---

## 14. Deploying to Oracle Cloud (OCI) — Always Free tier

Oracle's free tier is the most **generous** in the industry — 4 ARM Ampere cores + 24 GB RAM **always free**. Catch: you self-manage everything.

This is more involved because OCI doesn't offer Spring Boot PaaS — you provision an Ampere VM and run the JAR/Docker yourself.

### Steps

1. **Sign up:** <https://www.oracle.com/cloud/free/> (requires credit card for identity verification, no charges on free tier).

2. **Create a free Ampere A1 VM:**
   - Console: *Compute → Instances → Create Instance*
   - Image: *Ubuntu 22.04*
   - Shape: *VM.Standard.A1.Flex* — 2 OCPUs, 12 GB RAM (still within free tier)
   - Add an SSH public key.
   - Open port 8080: *Networking → Virtual Cloud Networks → Default Security List → Add Ingress Rule* — source `0.0.0.0/0`, TCP port 8080.

3. **SSH in:**
   ```bash
   ssh ubuntu@<public-ip>
   ```

4. **Install JDK + PostgreSQL:**
   ```bash
   sudo apt update
   sudo apt install -y openjdk-21-jdk postgresql
   sudo systemctl start postgresql
   sudo -u postgres psql -c "CREATE USER quickpoll WITH PASSWORD 'quickpoll';"
   sudo -u postgres psql -c "CREATE DATABASE quickpoll OWNER quickpoll;"
   ```
   (At time of writing Ubuntu's `openjdk-25-jdk` isn't in the default repos; use the Adoptium APT repo if you really need Java 25.)

5. **Upload the JAR:**
   From your laptop:
   ```bash
   scp target/quickpoll.jar ubuntu@<public-ip>:~/
   ```

6. **Run as a systemd service.** On the VM:
   ```bash
   sudo tee /etc/systemd/system/quickpoll.service > /dev/null <<'EOF'
   [Unit]
   Description=QuickPoll Spring Boot app
   After=network.target

   [Service]
   User=ubuntu
   Environment="SPRING_PROFILES_ACTIVE=prod"
   Environment="DATABASE_URL=jdbc:postgresql://localhost:5432/quickpoll"
   Environment="DATABASE_USERNAME=quickpoll"
   Environment="DATABASE_PASSWORD=quickpoll"
   Environment="DEPLOYMENT_LABEL=oci-ampere"
   ExecStart=/usr/bin/java -jar /home/ubuntu/quickpoll.jar
   SuccessExitStatus=143
   Restart=on-failure

   [Install]
   WantedBy=multi-user.target
   EOF

   sudo systemctl daemon-reload
   sudo systemctl enable --now quickpoll
   sudo systemctl status quickpoll
   ```

7. **Open** `http://<public-ip>:8080`.

### Make it HTTPS

Install Nginx + Certbot:
```bash
sudo apt install -y nginx certbot python3-certbot-nginx
sudo certbot --nginx -d yourdomain.com
```
Add an Nginx reverse proxy in front of port 8080.

---

## 15. Connecting to a managed database

Most cloud platforms in sections 7–14 either bundle a database or expect you to bring your own. Here are the recommended free SQL/NoSQL options.

### Supabase (PostgreSQL + REST + Auth)

1. Sign up at <https://supabase.com>, create a project.
2. *Project Settings → Database → Connection string → JDBC*.
3. In your platform's env vars:
   ```
   DATABASE_URL=jdbc:postgresql://db.xxx.supabase.co:5432/postgres?sslmode=require
   DATABASE_USERNAME=postgres
   DATABASE_PASSWORD=...
   SPRING_PROFILES_ACTIVE=prod
   ```

Free tier: 500 MB DB, 2 projects.

### Neon (Serverless Postgres with branching)

1. Sign up at <https://neon.tech>, create a project.
2. Copy the connection string from the dashboard.
3. Same as above — paste into your platform's env vars.

Free tier: 0.5 GB, branches for staging environments.

### Aiven

Managed PostgreSQL/MySQL/Kafka/OpenSearch with strong EU presence. Free trial only (1 month + $300 credit), not always-free.

### MongoDB Atlas (if you switch to a document model)

Free M0 cluster: 512 MB storage. Replace the `spring-boot-starter-data-jpa` dependency with `spring-boot-starter-data-mongodb` and rewrite the entities — beyond the scope of this guide.

### Quick test for a Supabase/Neon connection

From your laptop, before deploying:

```bash
java -jar target/quickpoll.jar \
  --spring.profiles.active=prod \
  --spring.datasource.url="jdbc:postgresql://db.xxx.supabase.co:5432/postgres?sslmode=require" \
  --spring.datasource.username=postgres \
  --spring.datasource.password=...
```

If it starts cleanly and the seeded polls appear at <http://localhost:8080>, your cloud deployment will work too.

---

## 16. Troubleshooting

### "Port 8080 already in use"

Another process owns the port. Either kill it or change ports:
```bash
java -jar target/quickpoll.jar --server.port=9090
```

### Build fails: "Source option 25 is no longer supported"

Your Maven is using an older JDK. Check with `mvn -v`. Either install JDK 25, or downgrade `<java.version>` in `pom.xml` (Spring Boot 4 requires at least Java 21).

### Tomcat deploys the WAR but you get 404 on the app

- Check `~/apache-tomcat-11.0.0/logs/catalina.out` for stack traces.
- If you see `ServletInitializer` not found — you skipped that class. Re-add it.
- If the WAR contains an embedded Tomcat — your `war` profile didn't activate. Make sure you built with `-Pwar`.

### Docker container exits immediately

```bash
docker logs quickpoll
```
Look for the stack trace. Most common: bad `DATABASE_URL` (e.g. pointing at `localhost` from inside a container — it should point at the host's IP or another container's service name).

### Cloud app shows "Application failed to start"

The platform tried to connect to the database before it was ready, or the env vars are wrong. Re-check:
- `SPRING_PROFILES_ACTIVE=prod`
- `DATABASE_URL` is a full JDBC URL starting with `jdbc:postgresql://`
- The Postgres user has rights to create tables (Hibernate runs `ddl-auto=update`)

### Render free service is slow on first request

Expected — free instances spin down after 15 min idle. First request wakes them up (~30 s). Upgrade to the $7/month *Starter* plan for always-on.

### "Connection refused" between app and PostgreSQL in Docker Compose

The app container started before the database was ready. The compose file already includes a `healthcheck` + `depends_on: condition: service_healthy` — if you removed those, add them back.

---

## Cheat sheet — what to use when

| Scenario | Package | Hosting | Database |
|---|---|---|---|
| Demo to a classmate, 10 minutes | JAR + Ngrok | Your laptop | H2 in-memory |
| Student portfolio project, 24/7 | Docker | Render or Koyeb free | Supabase or Render Postgres |
| Side project that's gaining users | Docker | Fly.io or Railway | Neon |
| Serverless API, low traffic | Docker | Google Cloud Run | Firestore or Upstash |
| Enterprise / corporate | WAR + Docker | AWS Beanstalk or Azure App Service | Managed Postgres or DynamoDB |
| Legacy environment | WAR | On-prem Apache Tomcat | Existing corporate DB |

Pick the simplest combination that meets your requirements **today** — every choice in this table can grow into the next row when traffic justifies it.
