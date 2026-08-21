# --- Stage 1: Build ---
# Full JDK + Maven here — this stage's image is discarded entirely
# after the jar is built, so its size/tooling doesn't matter.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Downloading dependencies as a SEPARATE layer, before copying source
# code, means Docker's layer-cache reuses this expensive step on every
# rebuild unless pom.xml itself changed — a source-code-only change
# won't re-download the entire dependency tree.
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Stage 2: Runtime ---
# Only a slim JRE (not a full JDK — no compiler needed to RUN a jar),
# and only the final built jar gets copied in. No Maven, no source
# code, no build-cache in the actual production image — smaller image,
# smaller attack surface.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user — running as root inside a container is unnecessary
# privilege; if the app were ever compromised, a non-root process
# limits what an attacker could do to the container's filesystem.
RUN addgroup -S nearnow && adduser -S nearnow -G nearnow
COPY --from=build /app/target/*.jar app.jar
RUN chown nearnow:nearnow app.jar
USER nearnow

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
