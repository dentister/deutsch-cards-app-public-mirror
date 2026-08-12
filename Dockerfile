# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-17 AS builder

# Node is required for Vaadin production frontend build
ARG NODE_VERSION=20
RUN apt-get update && apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_${NODE_VERSION}.x | bash - && \
    apt-get install -y nodejs && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Cache Maven dependencies before copying full source
COPY pom.xml .
RUN mvn dependency:go-offline -Pbase,production -q --no-transfer-progress || true

# Copy source and build
COPY src ./src
RUN mvn package -Pbase,production -DskipTests --no-transfer-progress

# ── Stage 2: Runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/deutsche-cards-app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
