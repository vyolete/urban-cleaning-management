#!/bin/bash

###############################################################################
# Test Database Verification Script
# 
# Purpose: Verifies that the test database is correctly configured
# Usage: ./verify-test-db.sh
###############################################################################

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

CONTAINER_NAME="urbanclean-postgres"
DB_USER="urbanclean_user"
DB_NAME="urbanclean_test"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Test Database Verification${NC}"
echo -e "${BLUE}========================================${NC}"

# Check 1: Container running
echo -e "\n${BLUE}[1/6]${NC} Checking Docker container..."
if docker ps | grep -q "$CONTAINER_NAME"; then
    echo -e "${GREEN}✓ Container '$CONTAINER_NAME' is running${NC}"
else
    echo -e "${RED}✗ Container '$CONTAINER_NAME' is not running${NC}"
    exit 1
fi

# Check 2: Database exists
echo -e "\n${BLUE}[2/6]${NC} Checking database existence..."
DB_EXISTS=$(docker exec -it $CONTAINER_NAME psql -U $DB_USER -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$DB_NAME';" | tr -d '[:space:]')
if [ "$DB_EXISTS" = "1" ]; then
    echo -e "${GREEN}✓ Database '$DB_NAME' exists${NC}"
else
    echo -e "${RED}✗ Database '$DB_NAME' does not exist${NC}"
    echo -e "${YELLOW}Run: ./init-test-db.sh${NC}"
    exit 1
fi

# Check 3: PostGIS extension
echo -e "\n${BLUE}[3/6]${NC} Checking PostGIS extension..."
POSTGIS_EXISTS=$(docker exec -it $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -tAc "SELECT 1 FROM pg_extension WHERE extname = 'postgis';" | tr -d '[:space:]')
if [ "$POSTGIS_EXISTS" = "1" ]; then
    POSTGIS_VERSION=$(docker exec -it $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -tAc "SELECT PostGIS_Version();" | tr -d '[:space:]')
    echo -e "${GREEN}✓ PostGIS extension enabled (version: $POSTGIS_VERSION)${NC}"
else
    echo -e "${RED}✗ PostGIS extension not enabled${NC}"
    echo -e "${YELLOW}Run: ./init-test-db.sh${NC}"
    exit 1
fi

# Check 4: Database connection
echo -e "\n${BLUE}[4/6]${NC} Testing database connection..."
CONNECTION_TEST=$(docker exec -it $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -tAc "SELECT 'connected';" | tr -d '[:space:]')
if [ "$CONNECTION_TEST" = "connected" ]; then
    echo -e "${GREEN}✓ Database connection successful${NC}"
else
    echo -e "${RED}✗ Database connection failed${NC}"
    exit 1
fi

# Check 5: User permissions
echo -e "\n${BLUE}[5/6]${NC} Checking user permissions..."
OWNER=$(docker exec -it $CONTAINER_NAME psql -U $DB_USER -d postgres -tAc "SELECT pg_catalog.pg_get_userbyid(d.datdba) FROM pg_catalog.pg_database d WHERE d.datname = '$DB_NAME';" | tr -d '[:space:]')
if [ "$OWNER" = "$DB_USER" ]; then
    echo -e "${GREEN}✓ User '$DB_USER' owns the database${NC}"
else
    echo -e "${YELLOW}⚠ Database owner is '$OWNER', expected '$DB_USER'${NC}"
fi

# Check 6: Test configuration file
echo -e "\n${BLUE}[6/6]${NC} Checking test configuration..."
if [ -f "../application-test.properties" ]; then
    echo -e "${GREEN}✓ application-test.properties exists${NC}"
    
    # Check if it references the test database
    if grep -q "urbanclean_test" "../application-test.properties"; then
        echo -e "${GREEN}✓ Configuration references test database${NC}"
    else
        echo -e "${YELLOW}⚠ Configuration may not reference test database${NC}"
    fi
else
    echo -e "${RED}✗ application-test.properties not found${NC}"
fi

# Summary
echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}Verification Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "\n${BLUE}Database Information:${NC}"
echo -e "  Name: ${GREEN}$DB_NAME${NC}"
echo -e "  User: ${GREEN}$DB_USER${NC}"
echo -e "  Owner: ${GREEN}$OWNER${NC}"
echo -e "  PostGIS: ${GREEN}Enabled${NC}"
echo -e "\n${BLUE}Ready to run tests:${NC}"
echo -e "  ${YELLOW}mvn test -Dtest=EndToEndIntegrationTest${NC}"
echo -e "  ${YELLOW}mvn test${NC}"
