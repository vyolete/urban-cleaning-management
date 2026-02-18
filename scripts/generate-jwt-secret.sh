#!/bin/bash
# Script to generate a secure JWT secret for HS512 algorithm
# HS512 requires at least 512 bits (64 bytes)

echo "Generating secure JWT_SECRET for HS512 algorithm..."
echo ""
echo "JWT_SECRET (512 bits / 64 bytes in base64):"
openssl rand -base64 64 | tr -d '\n'
echo ""
echo ""
echo "Copy the generated secret above and set it as JWT_SECRET environment variable in AWS"
