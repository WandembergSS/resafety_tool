# syntax=docker/dockerfile:1
#
# Single-container build for Google Cloud Run.
# Stage 1 compiles the Angular SPA (via Quarkus Quinoa) and packages the Quarkus
# fast-jar. Stage 2 is a slim JDK 21 runtime that serves both the REST API and the
# SPA on one port.
#
# Build locally:   docker build -t resafety_tool .
# Run locally:     docker run --rm -p 8080:8080 -e DB_JDBC_URL=... -e DB_USER=... -e DB_PASSWORD=... -e GOOGLE_API_KEY=... resafety_tool

# --- Node toolchain (pinned to match quarkus.quinoa.package-manager-install.node-version) ---
FROM node:20.11.1-bookworm-slim AS node

# --- Build stage: Angular (Quinoa) + Quarkus fast-jar ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Reuse the pinned Node/npm so Quinoa does not download its own toolchain at build time
COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=node /usr/local/lib/node_modules /usr/local/lib/node_modules
RUN ln -sf /usr/local/lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm \
 && ln -sf /usr/local/lib/node_modules/npm/bin/npx-cli.js /usr/local/bin/npx \
 && node --version && npm --version

# Warm the Maven dependency cache before copying the full source tree
COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline || true

# Build: Quinoa compiles src/main/webui into the app, Quarkus produces target/quarkus-app
COPY src ./src
RUN mvn -B -DskipTests -Dquarkus.quinoa.package-manager-install=false clean package

# --- Runtime stage: Quarkus fast-jar on the same UBI base used by the repo's other images ---
FROM registry.access.redhat.com/ubi8/openjdk-21:1.23
ENV LANGUAGE='en_US:en'

# Layered copy so dependency layers stay cached across application-only changes
COPY --from=build --chown=185 /workspace/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build --chown=185 /workspace/target/quarkus-app/*.jar /deployments/
COPY --from=build --chown=185 /workspace/target/quarkus-app/app/ /deployments/app/
COPY --from=build --chown=185 /workspace/target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8080
USER 185
# Cloud Run sets PORT; application.properties already maps quarkus.http.port to it.
ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]
