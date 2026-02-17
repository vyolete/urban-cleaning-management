# Configuración de JWT_SECRET en AWS

## Problema

El error `WeakKeyException: The signing key's size is 384 bits which is not secure enough for the HS512 algorithm` indica que el JWT_SECRET configurado en AWS es demasiado corto.

**Requisito:** HS512 requiere una clave de al menos 512 bits (64 bytes).

## Solución

### 1. Generar un JWT_SECRET Seguro

Ejecuta el script incluido para generar un secreto seguro:

```bash
./scripts/generate-jwt-secret.sh
```

O genera uno manualmente:

```bash
openssl rand -base64 64 | tr -d '\n'
```

Ejemplo de salida (NO uses este, genera el tuyo):
```
9CbirXCzuIemM8OmZVIiRVzHeLTkoRXqkeUCGRqh+6MzjzrXEMvt9A4F1cVANppaMmhgDLSx4NDVFhP/i5l/dQ==
```

### 2. Configurar en AWS

Dependiendo de tu configuración de despliegue, configura la variable de entorno:

#### Opción A: EC2 User Data

Si usas EC2 con User Data script, agrega:

```bash
export JWT_SECRET="tu_secreto_generado_aqui"
```

#### Opción B: Docker Compose en EC2

Si usas docker-compose, crea/edita el archivo `.env`:

```bash
# Conectarse a la instancia EC2
ssh ec2-user@tu-instancia

# Editar el archivo .env
cd /path/to/docker
nano .env
```

Agrega o actualiza:
```
JWT_SECRET=tu_secreto_generado_aqui
```

Luego reinicia los contenedores:
```bash
docker-compose down
docker-compose up -d
```

#### Opción C: AWS Systems Manager Parameter Store

Almacena el secreto de forma segura:

```bash
aws ssm put-parameter \
  --name "/urbix/jwt-secret" \
  --value "tu_secreto_generado_aqui" \
  --type "SecureString" \
  --region us-west-2
```

Luego modifica tu User Data o script de inicio para recuperarlo:

```bash
JWT_SECRET=$(aws ssm get-parameter \
  --name "/urbix/jwt-secret" \
  --with-decryption \
  --query "Parameter.Value" \
  --output text \
  --region us-west-2)

export JWT_SECRET
```

#### Opción D: AWS Secrets Manager

Almacena el secreto:

```bash
aws secretsmanager create-secret \
  --name urbix/jwt-secret \
  --secret-string "tu_secreto_generado_aqui" \
  --region us-west-2
```

Recupera en tu aplicación o script de inicio:

```bash
JWT_SECRET=$(aws secretsmanager get-secret-value \
  --secret-id urbix/jwt-secret \
  --query SecretString \
  --output text \
  --region us-west-2)

export JWT_SECRET
```

### 3. Verificar la Configuración

Después de configurar, verifica que el backend inicie correctamente:

```bash
# Ver logs del contenedor backend
docker logs urbanclean-backend

# O si usas systemd
journalctl -u urbix-backend -f
```

No deberías ver el error `WeakKeyException`.

### 4. Probar la Autenticación

Prueba el login para verificar que JWT funciona:

```bash
curl -X POST http://tu-servidor/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Deberías recibir un token JWT válido.

## Notas de Seguridad

1. **NUNCA** commits el JWT_SECRET en el repositorio
2. **NUNCA** compartas el JWT_SECRET públicamente
3. Usa diferentes secretos para desarrollo, staging y producción
4. Rota el secreto periódicamente (cada 90 días recomendado)
5. Si el secreto se compromete, genera uno nuevo inmediatamente

## Troubleshooting

### El error persiste después de configurar

1. Verifica que la variable de entorno esté realmente configurada:
   ```bash
   docker exec urbanclean-backend env | grep JWT_SECRET
   ```

2. Verifica la longitud del secreto:
   ```bash
   echo -n "tu_secreto" | wc -c
   # Debe ser >= 64 caracteres en base64
   ```

3. Reinicia completamente los contenedores:
   ```bash
   docker-compose down
   docker-compose up -d
   ```

### Cambiar el algoritmo JWT (no recomendado)

Si por alguna razón no puedes usar un secreto de 512 bits, puedes cambiar el algoritmo a HS256 (requiere 256 bits mínimo) en el código Java, pero **esto es menos seguro**.

## Referencias

- [RFC 7518 - JSON Web Algorithms (JWA)](https://tools.ietf.org/html/rfc7518#section-3.2)
- [JJWT Security Best Practices](https://github.com/jwtk/jjwt#security)
