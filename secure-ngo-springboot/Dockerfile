# ============================================
# Stage 1: Build the application with Maven
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies first (caching layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# ============================================
# Stage 2: Run the application with JRE
# ============================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create directories for data and logs
RUN mkdir -p /app/data /app/logs

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the application port (Render provides PORT env variable)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:${PORT:-8080}/ || exit 1

# Run the application
# Render sets the PORT environment variable automatically
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]