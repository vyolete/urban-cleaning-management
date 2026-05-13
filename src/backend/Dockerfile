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
RUN mkdir -p /uploads && \
    chown -R spring:spring /uploads && \
    chmod 755 /uploads

# Copy jar and entrypoint from build stage / context
COPY --from=build /app/target/*.jar app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh

# Change ownership and mark entrypoint executable (must run as root before USER switch)
RUN chown -R spring:spring /app && \
    chmod +x /app/docker-entrypoint.sh

# Switch to non-root user
USER spring:spring

# Expose HTTP and HTTPS ports
EXPOSE 8080 8443

# JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Entrypoint generates the SSL keystore on first start (if SSL_ENABLED=true) then launches the JVM
ENTRYPOINT ["/app/docker-entrypoint.sh"]
