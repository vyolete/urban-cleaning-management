# Docker Configuration

Docker configuration for the Urban Cleaning Management System.

## Prerequisites

- Docker
- Docker Compose

## Running with Docker Compose

1. **Set up environment variables**
   ```bash
   cp ../.env.example .env
   # Edit .env with your configuration
   ```

2. **Start all services**
   ```bash
   docker-compose up -d
   ```

3. **View logs**
   ```bash
   docker-compose logs -f
   ```

4. **Stop services**
   ```bash
   docker-compose down
   ```

5. **Stop and remove volumes**
   ```bash
   docker-compose down -v
   ```

## Services

- **postgres**: PostgreSQL 15 with PostGIS 3.3 (port 5432)
- **backend**: Spring Boot API (port 8080)
- **frontend**: React SPA served by Nginx (port 3000)

## Accessing Services

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api
- PostgreSQL: localhost:5432

## Database Initialization

The database is automatically initialized with:
- PostGIS extension enabled
- Spatial indexes created
- Default algorithm configuration

See `init-db.sql` for details.
