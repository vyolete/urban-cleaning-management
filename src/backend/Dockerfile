# Multi-stage Dockerfile for Spring Boot Backend
# Optimized for production deployment

# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build application
COPY src ./src
RUN mvn clean package -DskipTests -B -U

# Runtime stage - Use slim JRE for smaller image
FROM eclipse-temurin:17-jre
WORKDIR /app

# Create non-root user for security (Debian/Ubuntu commands)
RUN groupadd -r spring && useradd -r -g spring spring

# Create uploads directory and set ownership to 'spring' user
# IMPORTANTE: Esto asegura que el usuario 'spring' pueda escribir en el volumen montado
RUN mkdir -p /uploads && \
    chown -R spring:spring /uploads && \
    chmod 755 /uploads

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership of application files
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
