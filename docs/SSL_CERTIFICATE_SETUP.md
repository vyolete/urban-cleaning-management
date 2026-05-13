# SSL Certificate Setup Guide

This guide explains how to configure SSL/TLS certificates for HTTPS support in the Urban Cleaning Management System.

## Table of Contents
- [Development Environment](#development-environment)
- [Production Environment](#production-environment)
- [Docker Deployment](#docker-deployment)
- [Troubleshooting](#troubleshooting)

## Development Environment

### Generate Self-Signed Certificate

For development and testing, you can generate a self-signed certificate using Java's `keytool`:

```bash
# Navigate to backend resources directory
cd src/backend/src/main/resources

# Generate PKCS12 keystore with self-signed certificate
keytool -genkeypair \
  -alias tomcat \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore keystore.p12 \
  -validity 365 \
  -storepass changeit

# You will be prompted for:
# - Your name (CN): localhost
# - Organizational unit: Development
# - Organization: Urban Cleaning
# - City/Locality: Your City
# - State/Province: Your State
# - Country code: ES (or your country)
```

### Configure Application

Update `application.properties` or set environment variables:

```properties
# Enable SSL
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=tomcat
server.port=8443

# CORS for HTTPS
cors.allowed-origins=https://localhost:3000
```

### Test HTTPS

```bash
# Start the application
./mvnw spring-boot:run

# Test HTTPS endpoint (ignore certificate warning for self-signed)
curl -k https://localhost:8443/actuator/health

# Or in browser (accept security warning)
https://localhost:8443/api/docs
```

## Production Environment

### Option 1: Let's Encrypt (Recommended)

Let's Encrypt provides free SSL certificates with automatic renewal.

#### Install Certbot

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install certbot

# CentOS/RHEL
sudo yum install certbot
```

#### Obtain Certificate

```bash
# Stop your application first
sudo systemctl stop urbanclean-backend

# Obtain certificate (standalone mode)
sudo certbot certonly --standalone \
  -d yourdomain.com \
  -d www.yourdomain.com \
  --email your-email@example.com \
  --agree-tos

# Certificates will be saved to:
# /etc/letsencrypt/live/yourdomain.com/fullchain.pem
# /etc/letsencrypt/live/yourdomain.com/privkey.pem
```

#### Convert to PKCS12

Spring Boot requires PKCS12 format:

```bash
# Convert PEM to PKCS12
sudo openssl pkcs12 -export \
  -in /etc/letsencrypt/live/yourdomain.com/fullchain.pem \
  -inkey /etc/letsencrypt/live/yourdomain.com/privkey.pem \
  -out /etc/ssl/certs/urbanclean-keystore.p12 \
  -name tomcat \
  -passout pass:your-secure-password

# Set proper permissions
sudo chmod 600 /etc/ssl/certs/urbanclean-keystore.p12
sudo chown urbanclean:urbanclean /etc/ssl/certs/urbanclean-keystore.p12
```

#### Configure Application

```bash
# Set environment variables
export SSL_ENABLED=true
export SSL_KEYSTORE_PATH=/etc/ssl/certs/urbanclean-keystore.p12
export SSL_KEYSTORE_PASSWORD=your-secure-password
export SSL_KEYSTORE_TYPE=PKCS12
export SSL_KEY_ALIAS=tomcat
export SERVER_PORT=8443

# Update CORS for production domain
export CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
```

#### Setup Auto-Renewal

```bash
# Test renewal
sudo certbot renew --dry-run

# Create renewal script
sudo nano /etc/cron.d/certbot-renewal

# Add this content:
0 3 * * * root certbot renew --quiet --deploy-hook "/usr/local/bin/urbanclean-cert-renewal.sh"
```

Create renewal hook script:

```bash
sudo nano /usr/local/bin/urbanclean-cert-renewal.sh
```

```bash
#!/bin/bash
# Convert renewed certificate to PKCS12
openssl pkcs12 -export \
  -in /etc/letsencrypt/live/yourdomain.com/fullchain.pem \
  -inkey /etc/letsencrypt/live/yourdomain.com/privkey.pem \
  -out /etc/ssl/certs/urbanclean-keystore.p12 \
  -name tomcat \
  -passout pass:your-secure-password

# Restart application
systemctl restart urbanclean-backend
```

```bash
# Make executable
sudo chmod +x /usr/local/bin/urbanclean-cert-renewal.sh
```

### Option 2: Commercial Certificate

If you have a commercial SSL certificate:

1. **Obtain certificate files** from your CA:
   - Certificate file (`.crt` or `.pem`)
   - Private key file (`.key`)
   - Intermediate certificates (if any)

2. **Convert to PKCS12**:

```bash
openssl pkcs12 -export \
  -in your-certificate.crt \
  -inkey your-private-key.key \
  -out keystore.p12 \
  -name tomcat \
  -CAfile intermediate-ca.crt \
  -caname root \
  -passout pass:your-secure-password
```

3. **Configure application** as shown above.

## Docker Deployment

### Prepare Certificate Directory

```bash
# Create certs directory
mkdir -p src/docker/certs

# Copy your keystore
cp /path/to/keystore.p12 src/docker/certs/

# Set permissions
chmod 600 src/docker/certs/keystore.p12
```

### Update .env File

Create or update `src/docker/.env`:

```env
# SSL Configuration
SSL_ENABLED=true
SSL_CERT_PATH=./certs
SSL_KEYSTORE_PASSWORD=your-secure-password
SSL_KEYSTORE_TYPE=PKCS12
SSL_KEY_ALIAS=tomcat

# Ports
BACKEND_PORT=8080
BACKEND_HTTPS_PORT=8443

# CORS
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
```

### Docker Compose

The `docker-compose.yml` is already configured to:
- Mount the `certs` directory
- Expose both HTTP (8080) and HTTPS (8443) ports
- Redirect HTTP to HTTPS automatically

```bash
# Start with HTTPS
cd src/docker
docker-compose up -d

# Check logs
docker-compose logs -f backend

# Test HTTPS
curl -k https://localhost:8443/actuator/health
```

### Production Docker Deployment

For production, use a reverse proxy (Nginx or Traefik) to handle SSL termination:

#### Option A: Nginx Reverse Proxy

```nginx
server {
    listen 80;
    server_name yourdomain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        proxy_pass http://frontend:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

#### Option B: Traefik (Automatic Let's Encrypt)

Add to `docker-compose.yml`:

```yaml
services:
  traefik:
    image: traefik:v2.10
    command:
      - "--api.insecure=true"
      - "--providers.docker=true"
      - "--entrypoints.web.address=:80"
      - "--entrypoints.websecure.address=:443"
      - "--certificatesresolvers.letsencrypt.acme.email=your-email@example.com"
      - "--certificatesresolvers.letsencrypt.acme.storage=/letsencrypt/acme.json"
      - "--certificatesresolvers.letsencrypt.acme.httpchallenge.entrypoint=web"
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - traefik-certificates:/letsencrypt
    networks:
      - urbanclean-network

  backend:
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.backend.rule=Host(`api.yourdomain.com`)"
      - "traefik.http.routers.backend.entrypoints=websecure"
      - "traefik.http.routers.backend.tls.certresolver=letsencrypt"

volumes:
  traefik-certificates:
```

## Troubleshooting

### Certificate Not Found

```
Error: java.io.FileNotFoundException: keystore.p12
```

**Solution**: Verify the keystore path and ensure the file exists:

```bash
# Check if file exists
ls -la src/backend/src/main/resources/keystore.p12

# Or for absolute path
ls -la /etc/ssl/certs/urbanclean-keystore.p12
```

### Wrong Password

```
Error: Keystore was tampered with, or password was incorrect
```

**Solution**: Verify the password matches:

```bash
# Test keystore password
keytool -list -keystore keystore.p12 -storepass your-password
```

### Certificate Expired

```
Error: Certificate has expired
```

**Solution**: Renew the certificate:

```bash
# For Let's Encrypt
sudo certbot renew --force-renewal

# Then convert to PKCS12 again
```

### CORS Errors with HTTPS

```
Error: CORS policy: No 'Access-Control-Allow-Origin' header
```

**Solution**: Ensure CORS origins use HTTPS:

```properties
cors.allowed-origins=https://yourdomain.com
```

### Mixed Content Warnings

If frontend is HTTPS but makes HTTP API calls:

**Solution**: Update frontend API URL to use HTTPS:

```env
VITE_API_URL=https://api.yourdomain.com
```

### Port Already in Use

```
Error: Port 8443 is already in use
```

**Solution**: Check what's using the port:

```bash
# Linux/Mac
sudo lsof -i :8443

# Windows
netstat -ano | findstr :8443

# Kill the process or use a different port
```

## Security Best Practices

1. **Use Strong Passwords**: Never use default passwords in production
2. **Restrict File Permissions**: `chmod 600` for keystore files
3. **Regular Updates**: Keep certificates renewed (Let's Encrypt expires every 90 days)
4. **TLS Version**: Use TLS 1.2 or higher only
5. **HSTS Header**: Enable HTTP Strict Transport Security
6. **Certificate Monitoring**: Set up alerts for expiring certificates

## Testing SSL Configuration

### Test SSL/TLS Configuration

```bash
# Using OpenSSL
openssl s_client -connect yourdomain.com:443 -servername yourdomain.com

# Using SSL Labs (online)
https://www.ssllabs.com/ssltest/analyze.html?d=yourdomain.com

# Using testssl.sh
git clone https://github.com/drwetter/testssl.sh.git
cd testssl.sh
./testssl.sh https://yourdomain.com
```

### Verify Certificate Chain

```bash
# Check certificate details
openssl x509 -in /etc/letsencrypt/live/yourdomain.com/fullchain.pem -text -noout

# Verify certificate chain
openssl verify -CAfile /etc/letsencrypt/live/yourdomain.com/chain.pem \
  /etc/letsencrypt/live/yourdomain.com/cert.pem
```

## Additional Resources

- [Let's Encrypt Documentation](https://letsencrypt.org/docs/)
- [Spring Boot SSL Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.webserver.configure-ssl)
- [Mozilla SSL Configuration Generator](https://ssl-config.mozilla.org/)
- [SSL Labs Best Practices](https://github.com/ssllabs/research/wiki/SSL-and-TLS-Deployment-Best-Practices)

---

*For questions or issues, please refer to the main project documentation or create an issue in the repository.*
