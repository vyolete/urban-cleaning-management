#!/bin/bash

###############################################################################
# Test Database Initialization Script
# 
# Purpose: Creates and configures the urbanclean_test database for integration tests
# Architecture: Clean separation between production and test databases
# 
# Usage: ./init-test-db.sh
# 
# Requirements:
#   - Docker container 'urbanclean-postgres' must be running
#   - PostgreSQL with PostGIS extension
#   - User 'urbanclean_user' with appropriate permissions
###############################################################################

set -e  # Exit on error

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
CONTAINER_NAME="urbanclean-postgres"
DB_USER="urbanclean_user"
DB_NAME="urbanclean_test"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Test Database Initialization${NC}"
echo -e "${BLUE}========================================${NC}"

# Check if container is running
echo -e "\n${BLUE}[1/5]${NC} Checking Docker container..."
if ! docker ps | grep -q "$CONTAINER_NAME"; then
    echo -e "${RED}Error: Container '$CONTAINER_NAME' is not running${NC}"
    echo "Please start the container with: docker-compose up -d"
    exit 1
fi
echo -e "${GREEN}✓ Container is running${NC}"

# Check if database already exists
echo -e "\n${BLUE}[2/5]${NC} Checking if database exists..."
DB_EXISTS=$(docker exec -it $CONTAINER_NAME psql -U $DB_USER -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$DB_NAME';")

if [ -n "$DB_EXISTS" ]; then
    echo -e "${BLUE}Database '$DB_NAME' already exists${NC}"
    read -p "Do you want to recreate it? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Dropping existing database..."
        docker exec -it $CONTAINER_NAME psql -U $DB_USER -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;"
        echo -e "${GREEN}✓ Database dropped${NC}"
    else
        echo "Keeping existing database"
        exit 0
    fi
fi

# Create database
echo -e "\n${BLUE}[3/5]${NC} Creating database..."
docker exec -it $CONTAINER_NAME psql -U $DB_USER -d postgres -c "CREATE DATABASE $DB_NAME OWNER $DB_USER;"
echo -e "${GREEN}✓ Database '$DB_NAME' created${NC}"

# Enable PostGIS extension
echo -e "\n${BLUE}[4/5]${NC} Enabling PostGIS extension..."
docker exec -it $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -c "CREATE EXTENSION IF NOT EXISTS postgis;"
echo -e "${GREEN}✓ PostGIS extension enabled${NC}"

# Verify installation
echo -e "\n${BLUE}[5/5]${NC} Verifying installation..."
POSTGIS_VERSION=$(docker exec -it $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -tAc "SELECT PostGIS_Version();")
echo -e "${GREEN}✓ PostGIS version: $POSTGIS_VERSION${NC}"

# Summary
echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}Test Database Ready!${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "Database: ${BLUE}$DB_NAME${NC}"
echo -e "User: ${BLUE}$DB_USER${NC}"
echo -e "PostGIS: ${BLUE}Enabled${NC}"
echo -e "\n${BLUE}Next steps:${NC}"
echo "1. Run integration tests: mvn test -Dtest=EndToEndIntegrationTest"
echo "2. Run all tests: mvn test"
echo -e "\n${BLUE}Note:${NC} Spring Boot will automatically create tables using Flyway migrations"
