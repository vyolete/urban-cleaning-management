# Multi-stage Dockerfile for React Frontend
# Optimized for production deployment with Nginx

# Build stage
FROM node:18-alpine AS build
WORKDIR /app

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

# Create non-root user for security
RUN addgroup -S nginx-app && adduser -S nginx-app -G nginx-app

# Remove default nginx static assets
RUN rm -rf ./*

# Copy build artifacts from build stage
COPY --from=build /app/dist .

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy nginx main config for non-root user
RUN chown -R nginx-app:nginx-app /usr/share/nginx/html && \
    chown -R nginx-app:nginx-app /var/cache/nginx && \
    chown -R nginx-app:nginx-app /var/log/nginx && \
    chown -R nginx-app:nginx-app /etc/nginx/conf.d && \
    touch /var/run/nginx.pid && \
    chown -R nginx-app:nginx-app /var/run/nginx.pid

# Switch to non-root user
USER nginx-app

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:80/ || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
