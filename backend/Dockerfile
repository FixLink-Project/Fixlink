# =============================================================================
# Stage 1: Build JAR with Maven & OpenJDK 21
# =============================================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy Maven descriptor and download dependencies (kèm plugin, để Lombok
# annotation processor có sẵn khi biên dịch offline)
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline dependency:resolve-plugins

# Copy source code and build production artifact
COPY checkstyle.xml ./
COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

# =============================================================================
# Stage 2: Runtime Image with Eclipse Temurin JRE 21 Alpine
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root system user for security
RUN addgroup -S fixlink && adduser -S fixlink -G fixlink

# Install wget for healthcheck
RUN apk add --no-cache wget

# Copy artifact from builder stage
COPY --from=builder /build/target/fixlink-backend-0.0.1-SNAPSHOT.jar /app/app.jar
RUN chown -R fixlink:fixlink /app

USER fixlink

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/login.html || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
