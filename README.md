# resafety_tool

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

---

# Deployment & maintenance — CIn UFPE VM

This is the operational guide for the production deployment of the **RESafety Tool**
on the CIn UFPE virtual machine. It covers the first-time deploy, shipping new
versions, managing users and the Google API key, backups, and troubleshooting.

## 1. Architecture

```
Browser ──HTTPS──> https://resafetytool.cin.ufpe.br
                         │  (CIn nginx reverse proxy — terminates TLS)
                         ▼
                   http://vm-resafetytool.cin.ufpe.br  (port 80)
                         │  (Docker publishes host 80 -> container 8080)
                         ▼
        ┌────────────────────────────────────────────┐
        │  Docker Compose project (vm)                │
        │  ┌─────────────────┐   ┌──────────────────┐ │
        │  │ resafetytool-app│──▶│ resafetytool-db  │ │
        │  │ Quarkus + Angular│  │ postgres:16      │ │
        │  │ container :8080  │  │ (pgdata volume)  │ │
        │  └─────────────────┘   └──────────────────┘ │
        └────────────────────────────────────────────┘
```

- The single **app** container serves the Angular SPA, the REST API, and the
  in-process Gemini AI integration on port `8080`.
- The **db** container (PostgreSQL 16) is **not** published to the host — only the
  app reaches it over the private Compose network. Data lives in the `pgdata` named volume.
- The CIn nginx proxy fronts HTTPS; the app itself only speaks plain HTTP on port 80.
- TLS certificates, the public DNS name, and the proxy are managed by the CIn team.

## 2. Key facts (fill in / confirm)

| Item | Value |
|---|---|
| Public URL | <https://resafetytool.cin.ufpe.br> |
| VM host (SSH) | `vm-resafetytool.cin.ufpe.br` (172.21.6.198) |
| SSH login | `wss2` or `jbc` — **same credentials as the CIn VPN** |
| Network access | SSH is reachable **only over the CIn VPN** |
| App directory | `/opt/docker/resafetytool` (git clone of this repo, branch `main`) |
| Compose directory | `/opt/docker/resafetytool/deploy/vm` |
| Secrets file | `/opt/docker/resafetytool/deploy/vm/.env` (git-ignored, `chmod 600`) |
| Containers | `resafetytool-app`, `resafetytool-db` |
| Git remote | `https://github.com/WandembergSS/resafety_tool.git` |

> **Convention:** every command below assumes you have first connected to the VM
> over the **CIn VPN** and SSH'd in:
>
> ```bash
> ssh wss2@vm-resafetytool.cin.ufpe.br
> cd /opt/docker/resafetytool/deploy/vm
> ```
>
> All `docker compose` commands must be run from `/opt/docker/resafetytool/deploy/vm`
> (the folder that contains `docker-compose.yml`).

## 3. Everyday operations

```bash
cd /opt/docker/resafetytool/deploy/vm

# Status of both containers (look for "healthy")
docker compose ps

# Follow application logs (Ctrl+C to stop)
docker compose logs -f app

# Last 100 lines of app / db logs
docker compose logs --tail=100 app
docker compose logs --tail=100 db

# Restart just the app (no data loss)
docker compose restart app

# Stop / start the whole stack (data is preserved in the pgdata volume)
docker compose stop
docker compose up -d

# Health check from inside the VM
curl -fsS http://localhost/q/health/ready
```

From your workstation (on the VPN) you can also verify the public endpoint:

```bash
curl -I https://resafetytool.cin.ufpe.br/
curl -fsS https://resafetytool.cin.ufpe.br/q/health/ready
```

## 4. First-time deployment (already done — for reference / rebuild)

Use this only if the VM is empty or you are rebuilding from scratch.

```bash
# 1. Clone the repo into the CIn-mandated apps directory
git clone --branch main https://github.com/WandembergSS/resafety_tool.git \
  /opt/docker/resafetytool
cd /opt/docker/resafetytool/deploy/vm

# 2. Create the secrets file from the template
cp .env.example .env
chmod 600 .env

# 3. Generate a clean DB password (alphanumeric/hex only — see the SASLprep note)
sed -i "s|^DB_PASSWORD=.*|DB_PASSWORD=$(openssl rand -hex 24)|" .env

# 4. Fill in the two real secrets (see sections 6 and 7):
nano .env        # set GOOGLE_API_KEY=... and AUTH_USERS=user:pass,...

# 5. Build the image on the VM and start everything
docker compose build app
docker compose up -d

# 6. Watch the boot — expect Flyway migrations V1..V8 and "started in ...s"
docker compose logs -f app
```

> **Build vs. transfer:** the VM has outbound access to Docker Hub, Maven Central,
> npm, and Red Hat UBI, so it builds the image locally (`docker compose build app`).
> If a future firewall change blocks those registries, build elsewhere and use
> [`deploy/vm/transfer-image.sh`](deploy/vm/transfer-image.sh) to `docker save | ssh | docker load`
> the image, then `docker compose up -d` (without `build`).

## 5. Updating to a new version (deploy new code)

After pushing changes to `main` on GitHub:

```bash
cd /opt/docker/resafetytool

# 1. Pull the latest code (does NOT touch .env — it is git-ignored)
git fetch origin main
git reset --hard origin/main

# 2. Rebuild the app image and recreate the container
cd deploy/vm
docker compose build app
docker compose up -d

# 3. Verify
docker compose ps
docker compose logs --tail=80 app
curl -fsS http://localhost/q/health/ready && echo "  OK"
```

Notes:
- `git reset --hard origin/main` discards any **local** edits to tracked files on the
  VM. Your secrets in `.env` are safe (git-ignored). If you handed-edit tracked files
  on the VM, commit them first.
- The PostgreSQL data volume (`pgdata`) and all projects survive updates. Flyway only
  applies **new** migrations; existing data is untouched.
- To roll back, `git reset --hard <previous-commit>` then rebuild.

## 6. Change the Google API key (Gemini)

The key lives in `GOOGLE_API_KEY` inside `.env`. Changing it requires recreating the
app container so the new value is injected.

```bash
cd /opt/docker/resafetytool/deploy/vm

# Edit the key
nano .env            # update the line:  GOOGLE_API_KEY=your-new-key

# Recreate the app so it picks up the new env value
docker compose up -d --force-recreate app

# Confirm the app restarted cleanly
docker compose logs --tail=40 app
```

> The Gemini model is configured separately via `GEMINI_MODEL` in `.env`
> (currently `gemini-3-flash-preview`). AI features need outbound HTTPS to
> `generativelanguage.googleapis.com`; if that egress is ever blocked, the app
> still runs but every "Generate with AI" action fails — request the egress from
> the CIn helpdesk.

## 7. Add, change, or remove a login user

Users are defined as a comma-separated list in `AUTH_USERS` (no database table):

```
AUTH_USERS=user1:password1,user2:password2,admin:secret
```

To change the user list:

```bash
cd /opt/docker/resafetytool/deploy/vm

# Edit the AUTH_USERS line (add/remove "user:password" pairs, comma-separated)
nano .env

# Recreate the app to apply the change
docker compose up -d --force-recreate app
```

Rules and tips:
- Format is `username:password`, pairs separated by commas, **no spaces**.
- Removing a pair revokes that user's ability to obtain new tokens. Existing JWTs
  remain valid until they expire (`AUTH_JWT_LIFESPAN_SECONDS`, default 3600s = 1h).
- Verify the login endpoint afterwards (a wrong password must return HTTP 401):

  ```bash
  curl -s -o /dev/null -w '%{http_code}\n' \
    -X POST https://resafetytool.cin.ufpe.br/auth/login \
    -H 'Content-Type: application/json' \
    -d '{"username":"someuser","password":"theirpassword"}'
  # 200 = credentials accepted, 401 = rejected
  ```

## 8. Database backups

A daily backup runs via cron (installed during deploy):

```
0 3 * * *  /opt/docker/resafetytool/deploy/vm/backup.sh >> .../backups/backup.log 2>&1
```

It writes a gzipped `pg_dump` into `deploy/vm/backups/` and prunes dumps older than
14 days (`RETENTION_DAYS`).

```bash
cd /opt/docker/resafetytool/deploy/vm

# Run a backup right now
./backup.sh

# List existing backups
ls -lh backups/

# Inspect the cron entry
crontab -l | grep backup.sh
```

### Restore from a backup

> ⚠️ Restoring overwrites current data. Take a fresh backup first.

```bash
cd /opt/docker/resafetytool/deploy/vm
./backup.sh                                   # safety snapshot first

# Drop & recreate the schema, then load the chosen dump
gunzip -c backups/resafetytool-YYYY-MM-DD_HHMMSS.sql.gz \
  | docker exec -i resafetytool-db psql -U resafetytool -d resafetytool

# Restart the app so Hibernate/Flyway see a clean state
docker compose restart app
```

## 9. Editing secrets safely (`.env`)

- The file is at `/opt/docker/resafetytool/deploy/vm/.env`, mode `600`, git-ignored.
  **Never commit it.**
- After any change to `.env`, apply it with `docker compose up -d --force-recreate app`
  (a plain `restart` does **not** re-read `env_file`).
- **`DB_PASSWORD` is special:** PostgreSQL stores the password the first time the
  `pgdata` volume is initialised. Changing `DB_PASSWORD` in `.env` afterwards makes the
  app fail to log in (`password authentication failed`). To rotate it you must also
  change it inside Postgres:

  ```bash
  # Set the new password inside the running database, then update .env to match
  docker exec -it resafetytool-db \
    psql -U resafetytool -d resafetytool -c "ALTER USER resafetytool PASSWORD 'NEW_CLEAN_PASSWORD';"
  nano .env        # set DB_PASSWORD=NEW_CLEAN_PASSWORD (alphanumeric/hex only)
  docker compose up -d --force-recreate app
  ```

  Passwords must be **clean alphanumeric/hex** — no spaces, quotes, `#`, or newlines
  (PostgreSQL SCRAM/SASLprep rejects control characters with "Prohibited character").

## 10. Troubleshooting

**`502 Bad Gateway` at `https://resafetytool.cin.ufpe.br`**
The CIn proxy is up but the app on port 80 is not answering. Almost always the app
container is down or crash-looping.

```bash
cd /opt/docker/resafetytool/deploy/vm
docker compose ps                      # is resafetytool-app "Up (healthy)"?
docker compose logs --tail=120 app     # why did it stop?
docker compose up -d                   # (re)start the stack
curl -fsS http://localhost/q/health/ready && echo OK
```

**App keeps restarting with `password authentication failed` / `Prohibited character`**
`DB_PASSWORD` in `.env` does not match the password stored in the `pgdata` volume.
Either fix it with the `ALTER USER` procedure in section 9, or — only if there is no
data worth keeping — wipe the volume and let Flyway reseed:

```bash
docker compose down -v        # ⚠️ DELETES the database volume (all projects)
docker compose up -d          # fresh DB, Flyway re-applies V1..V8 incl. the CRCS seed
```

**Cannot SSH to the VM**
SSH is only reachable over the **CIn VPN**. Confirm the VPN is actually routing
(you must be able to reach `172.21.6.198:22`) before retrying.

**AI ("Generate with AI") fails but the app otherwise works**
Outbound HTTPS to `generativelanguage.googleapis.com` is blocked, or `GOOGLE_API_KEY`
is wrong/expired. Test egress from the VM:

```bash
curl -s -o /dev/null -w '%{http_code}\n' https://generativelanguage.googleapis.com/v1beta/models
# 401/403 = reachable (egress OK, key/headers aside); 000 = blocked by firewall
```

## 11. Helper scripts (in `deploy/vm/`)

| Script | Purpose |
|---|---|
| [`bootstrap.sh`](deploy/vm/bootstrap.sh) | Clone/update the repo, create the `.env` skeleton (generated DB password), build the image. |
| [`set-secrets.sh`](deploy/vm/set-secrets.sh) | Interactively prompt for `GOOGLE_API_KEY` and `AUTH_USERS` and write them into `.env` (input hidden). |
| [`launch.sh`](deploy/vm/launch.sh) | `docker compose up -d` and wait until the app is healthy. |
| [`setup-backup-cron.sh`](deploy/vm/setup-backup-cron.sh) | Install the daily backup cron and run a test backup. |
| [`backup.sh`](deploy/vm/backup.sh) | One gzipped `pg_dump` into `backups/`, prune old dumps. |
| [`egress-probe.sh`](deploy/vm/egress-probe.sh) | Test build-registry and Gemini reachability from the VM. |
| [`transfer-image.sh`](deploy/vm/transfer-image.sh) | Build the image elsewhere and load it onto the VM (if the VM cannot build). |

---

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/resafety_tool-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- Hibernate ORM with Panache ([guide](https://quarkus.io/guides/hibernate-orm-panache)): Simplify your persistence code for Hibernate ORM via the active record or the repository pattern
- SmallRye Health ([guide](https://quarkus.io/guides/smallrye-health)): Monitor service health
- Flyway ([guide](https://quarkus.io/guides/flyway)): Handle your database schema migrations
- SmallRye OpenAPI ([guide](https://quarkus.io/guides/openapi-swaggerui)): Document your REST APIs with OpenAPI - comes with Swagger UI
- JDBC Driver - PostgreSQL ([guide](https://quarkus.io/guides/datasource)): Connect to the PostgreSQL database via JDBC
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it

## Provided Code

### Hibernate ORM

Create your first JPA entity

[Related guide section...](https://quarkus.io/guides/hibernate-orm)

[Related Hibernate with Panache section...](https://quarkus.io/guides/hibernate-orm-panache)


### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

### SmallRye Health

Monitor your application's health using SmallRye Health

[Related guide section...](https://quarkus.io/guides/smallrye-health)
