# syntax=docker/dockerfile:1.7

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw
COPY config config
COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -q -B clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app app \
    && mkdir -p /app/uploads && chown -R app:app /app
COPY --from=build --chown=app:app /app/target/fast-vue3-server-0.1.0.jar app.jar
USER 10001:10001
ENV FILE_STORAGE_ROOT=/app/uploads
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/liveness || exit 1
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
