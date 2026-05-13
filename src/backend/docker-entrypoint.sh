#!/bin/sh
set -e

if [ "$SSL_ENABLED" = "true" ]; then
    KEYSTORE_FILE="${SSL_KEYSTORE_PATH:-/etc/ssl/certs/keystore.p12}"
    KEYSTORE_PASSWORD="${SSL_KEYSTORE_PASSWORD:-changeit}"
    KEY_ALIAS="${SSL_KEY_ALIAS:-tomcat}"

    if [ ! -f "$KEYSTORE_FILE" ]; then
        echo "[entrypoint] SSL enabled — generating self-signed keystore at $KEYSTORE_FILE"
        mkdir -p "$(dirname "$KEYSTORE_FILE")"
        keytool -genkeypair \
            -alias "$KEY_ALIAS" \
            -keyalg RSA \
            -keysize 2048 \
            -validity 3650 \
            -storetype PKCS12 \
            -keystore "$KEYSTORE_FILE" \
            -storepass "$KEYSTORE_PASSWORD" \
            -keypass "$KEYSTORE_PASSWORD" \
            -dname "CN=localhost, OU=Dev, O=UrbanClean, L=Local, ST=Local, C=ES" \
            -ext "SAN=dns:localhost,ip:127.0.0.1"
        echo "[entrypoint] Keystore generated successfully."
    else
        echo "[entrypoint] SSL enabled — keystore already exists, skipping generation."
    fi
fi

exec java $JAVA_OPTS -jar /app/app.jar
