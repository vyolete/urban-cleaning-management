# How to Generate a Secure JWT_SECRET

## Why You Need a Secure JWT_SECRET

The JWT_SECRET is used to sign and verify JSON Web Tokens (JWT) for authentication. A weak or predictable secret can compromise your entire authentication system.

## Requirements

- **Minimum length**: 512 bits (64 bytes)
- **Encoding**: Base64
- **Randomness**: Cryptographically secure random bytes

## Generation Methods

### Windows (PowerShell)

```powershell
$bytes = New-Object byte[] 64
[Security.Cryptography.RNGCryptoServiceProvider]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

**Example output:**
```
ViU8ekYQtVtv3kc7XRx/j1gKdRjcxu4yfPs0Co4433S7NzyMcsbQagCrVXqZsuOlPdbZtXoa6jSBYJ/jpqOBNQ==
```

### Linux/Mac (OpenSSL)

```bash
openssl rand -base64 64 | tr -d '\n'
```

**Example output:**
```
9CbirXCzuIemM8OmZVIiRVzHeLTkoRXqkeUCGRqh+6MzjzrXEMvt9A4F1cVANppaMmhgDLSx4NDVFhP/i5l/dQ==
```

### Node.js

```javascript
const crypto = require('crypto');
const secret = crypto.randomBytes(64).toString('base64');
console.log(secret);
```

### Python

```python
import secrets
import base64

secret_bytes = secrets.token_bytes(64)
secret = base64.b64encode(secret_bytes).decode('utf-8')
print(secret)
```

## How to Update JWT_SECRET

### 1. Generate a new secret using one of the methods above

### 2. Update the .env file

Edit `src/docker/.env`:

```bash
JWT_SECRET=YOUR_GENERATED_SECRET_HERE
```

### 3. Restart the backend container

```bash
cd src/docker
docker-compose restart backend
```

### 4. Verify the backend is healthy

```bash
curl http://localhost:8080/actuator/health
```

You should see: `{"status":"UP"}`

## Security Best Practices

1. **Never commit the JWT_SECRET to version control**
   - The `.env` file should be in `.gitignore`
   - Only commit `.env.example` with placeholder values

2. **Use different secrets for different environments**
   - Development: Can use a simpler secret
   - Staging: Use a production-grade secret
   - Production: Use a unique, cryptographically secure secret

3. **Rotate secrets periodically**
   - Change the JWT_SECRET every 6-12 months
   - When rotating, all users will need to log in again

4. **Store secrets securely**
   - Use environment variables or secret management services
   - Never hardcode secrets in application code
   - Consider using AWS Secrets Manager, Azure Key Vault, or HashiCorp Vault

## Troubleshooting

### Error: "WeakKeyException: The specified key byte array is X bits which is not secure enough"

**Cause**: The JWT_SECRET is too short (less than 512 bits)

**Solution**: Generate a new 64-byte (512-bit) secret using the methods above

### Error: "SignatureException: JWT signature does not match locally computed signature"

**Cause**: The JWT_SECRET changed after tokens were issued

**Solution**: 
- All users need to log in again
- Clear browser localStorage/cookies
- Restart the backend with the correct JWT_SECRET

### Error: "MalformedJwtException: Unable to read JSON value"

**Cause**: The JWT_SECRET contains invalid characters or encoding

**Solution**: Ensure the secret is properly base64 encoded without line breaks

## Current Configuration

The system is currently configured with:
- **JWT_SECRET**: 512-bit cryptographically secure random key (base64 encoded)
- **JWT_EXPIRATION**: 86400000 ms (24 hours)
- **Algorithm**: HS512 (HMAC with SHA-512)

## Additional Resources

- [JWT.io](https://jwt.io/) - JWT debugger and documentation
- [OWASP JWT Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
- [Spring Security JWT Documentation](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
