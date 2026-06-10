# syntax=docker/dockerfile:1
# Multi-stage build: one image runs as either the API node or the indexing worker
# (role chosen at runtime via app.worker.enabled / LLM_PROVIDER / EMBEDDING_PROVIDER).

# --- build stage ---
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
# Copy build descriptors first so dependency resolution is cached when only src changes.
COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies >/dev/null 2>&1 || true
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# --- runtime stage ---
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app
# Run as a non-root user.
RUN useradd -r -u 1001 workmate
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar
USER workmate
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
