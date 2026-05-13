# Multi-stage Dockerfile for React Frontend
# Optimized for production deployment with Nginx

# Build stage
FROM node:18-alpine AS build
WORKDIR /app

# Build argument for API URL (injected during build)
ARG VITE_API_URL

# Copy package files and install dependencies (cached layer)
COPY package*.json ./
RUN npm install && npm cache clean --force

# Copy source code
COPY . .

# Build application for production
RUN npm run build

# Runtime stage - Use Nginx for serving static files
FROM nginx:1.25-alpine
WORKDIR /usr/share/nginx/html

# Install openssl CLI for self-signed cert generation
RUN apk add --no-cache openssl

# Create non-root user for security
RUN addgroup -S nginx-app && adduser -S nginx-app -G nginx-app

# Remove default nginx static assets
RUN rm -rf ./*

# Copy build artifacts and configuration
COPY --from=build /app/dist .
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY nginx-ssl.conf /etc/nginx/nginx-ssl.conf
COPY docker-entrypoint.sh /docker-entrypoint.sh

# Set ownership and permissions (run as root before USER switch)
RUN chown -R nginx-app:nginx-app /usr/share/nginx/html && \
    chown -R nginx-app:nginx-app /var/cache/nginx && \
    chown -R nginx-app:nginx-app /var/log/nginx && \
    chown -R nginx-app:nginx-app /etc/nginx/conf.d && \
    mkdir -p /etc/nginx/ssl && \
    chown nginx-app:nginx-app /etc/nginx/ssl /etc/nginx/nginx-ssl.conf && \
    touch /var/run/nginx.pid && \
    chown -R nginx-app:nginx-app /var/run/nginx.pid && \
    chmod +x /docker-entrypoint.sh

# Switch to non-root user
USER nginx-app

# Expose HTTP and HTTPS ports
EXPOSE 80 443

# Entrypoint generates SSL cert on first start (if SSL_ENABLED=true) then launches Nginx
ENTRYPOINT ["/docker-entrypoint.sh"]
