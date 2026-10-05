# Stage 1: build
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./

RUN ./gradlew --no-daemon --version

COPY server/ server/

RUN ./gradlew --no-daemon :server:bootJar

# Stage 2: runtime
FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --home /app app \
    && mkdir -p /app/data && chown app:app /app/data

COPY --from=build --chown=app:app /app/server/build/libs/kdm-tracker.jar /app/app.jar

USER app

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
