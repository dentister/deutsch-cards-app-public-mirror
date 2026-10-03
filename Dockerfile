# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS builder

# Node is required for Vaadin production frontend build
ARG NODE_VERSION=20
RUN apt-get update && apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_${NODE_VERSION}.x | bash - && \
    apt-get install -y nodejs && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Cache Maven dependencies before copying full source
COPY pom.xml .
COPY core/pom.xml core/
COPY ai-integration/pom.xml ai-integration/
COPY telegram-bots/pom.xml telegram-bots/
COPY app/pom.xml app/
RUN mvn dependency:go-offline -Pproduction -q --no-transfer-progress || true

# Copy source and build
COPY core ./core
COPY ai-integration ./ai-integration
COPY telegram-bots ./telegram-bots
COPY app ./app
RUN mvn package -Pproduction -DskipTests --no-transfer-progress

# ── Stage 2: Runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/app/target/deutsche-cards-app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
