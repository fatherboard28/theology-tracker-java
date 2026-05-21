# ─────────────────────────────────────────────────────────────────────────────
# Stage 1 — Build
# Uses the official Maven image with JDK 21 to compile and package the app.
# The Maven cache is preserved in a named cache mount for faster rebuilds.
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3.9.7-eclipse-temurin-21 AS build

WORKDIR /build

# Copy POM first so Maven can resolve dependencies before copying source.
# This layer is cached as long as pom.xml does not change.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -q

# Copy application source and build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -q

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2 — Runtime
# Lean JRE-only image; no Maven, no source code.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create a non-root user to run the application
RUN addgroup -S tracker && adduser -S tracker -G tracker

WORKDIR /app

# Copy the fat JAR from the build stage
COPY --from=build /build/target/tracker-*.jar app.jar

# The data directory will be bind-mounted from the host (see docker-compose.yml).
# Create it here so the directory exists with correct ownership if the volume
# is not mounted (e.g. during `docker run` without compose).
RUN mkdir -p /app/data && chown -R tracker:tracker /app/data

USER tracker

# Spring Boot embedded server port
EXPOSE 8080

# THEOLOGY_DB_PATH is read by application.properties to set the SQLite path.
# The default in application.properties already points here, but we make it
# explicit so it is obvious and can be overridden at runtime.
ENV THEOLOGY_DB_PATH=/app/data/theology.db

# JVM tuning for a small local application:
#   -XX:+UseSerialGC    — serial GC suits a single-threaded local app well
#   -Xms128m / -Xmx512m — conservative heap bounds for local use
ENTRYPOINT ["java", \
    "-XX:+UseSerialGC", \
    "-Xms128m", \
    "-Xmx512m", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
