#!/bin/sh
set -e

if [ "${SSL_ENABLED:-false}" = "true" ]; then
    if [ ! -f /etc/nginx/ssl/nginx.crt ]; then
        echo "[entrypoint] Generating self-signed certificate for frontend..."
        openssl req -x509 -nodes -days 3650 -newkey rsa:2048 \
            -keyout /etc/nginx/ssl/nginx.key \
            -out /etc/nginx/ssl/nginx.crt \
            -subj "/CN=localhost/OU=Dev/O=UrbanClean/L=Local/ST=Local/C=ES" \
            -addext "subjectAltName=DNS:localhost,IP:127.0.0.1" 2>/dev/null
        chmod 600 /etc/nginx/ssl/nginx.key
        echo "[entrypoint] Certificate generated."
    else
        echo "[entrypoint] Certificate already exists, skipping generation."
    fi
    cp /etc/nginx/nginx-ssl.conf /etc/nginx/conf.d/default.conf
    echo "[entrypoint] Using HTTPS configuration."
fi

exec nginx -g "daemon off;"
