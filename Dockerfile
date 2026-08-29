# syntax=docker/dockerfile:1

FROM gradle:9.7.1-jdk25 AS build
WORKDIR /workspace

# Cache dependencies separately from source so source-only changes don't re-download the world.
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY gradlew ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

FROM eclipse-temurin:25-jre-alpine AS runtime
RUN addgroup -S memoq && adduser -S memoq -G memoq
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER memoq:memoq

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
