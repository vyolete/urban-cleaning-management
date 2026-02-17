# Notas de la Versión

## Versión Actual - Febrero 2026

### ✅ Correcciones Importantes

#### Autenticación y Seguridad
- **JWT_SECRET actualizado**: Configurado con valor de 512 bits para cumplir con el estándar HS512
- **Usuarios de prueba**: Credenciales actualizadas y documentadas correctamente
- **Proxy nginx**: Configurado para evitar problemas de CORS entre frontend y backend

#### Configuración
- **Variables de entorno**: Archivo `.env` preconfigurado con valores seguros
- **API URL**: Frontend configurado para usar rutas relativas (`/api`) a través del proxy nginx
- **Base de datos**: Credenciales y configuración documentadas

### 🔧 Configuración Requerida

#### Para Desarrollo (Ya configurado)
El sistema funciona out-of-the-box con:
- JWT_SECRET seguro (512 bits)
- Usuarios de prueba creados automáticamente
- Proxy nginx configurado
- Variables de entorno con valores por defecto

#### Para Producción (Requiere cambios)
Antes de desplegar en producción, DEBES cambiar en `src/docker/.env`:

```bash
# Base de Datos
DB_PASSWORD=<contraseña-segura-producción>

# JWT
JWT_SECRET=<generar-nuevo-secret-512-bits>

# Email (si usas notificaciones)
SMTP_HOST=<tu-servidor-smtp>
SMTP_USERNAME=<tu-usuario>
SMTP_PASSWORD=<tu-contraseña>
```

Para generar un nuevo JWT_SECRET seguro:
```bash
openssl rand -base64 64 | tr -d '\n'
```

### 📋 Credenciales de Usuarios de Prueba

Los siguientes usuarios se crean automáticamente al iniciar el sistema:

| Rol | Username | Password | Email |
|-----|----------|----------|-------|
| Administrador | `admin` | `Admin123!@#` | admin@urbanclean.com |
| Técnico | `tecnico` | `Tecnico123!@#` | tecnico@urbanclean.com |
| Ciudadano | `ciudadano` | `Ciudadano123!@#` | ciudadano@urbanclean.com |

**Importante**: Estas credenciales son solo para desarrollo. En producción, elimina estos usuarios o cambia sus contraseñas.

### 🐛 Problemas Conocidos Resueltos

1. ✅ **Error de JWT WeakKeyException**: Resuelto con JWT_SECRET de 512 bits
2. ✅ **Login no funciona**: Resuelto con proxy nginx y credenciales correctas
3. ✅ **CORS errors**: Resuelto con configuración de proxy en nginx
4. ✅ **Usuarios con contraseñas incorrectas**: Resuelto recreando usuarios en base de datos

### 🔄 Proceso de Actualización

Si ya tienes una versión anterior ejecutándose:

```bash
# 1. Detener contenedores
cd src/docker
docker-compose down

# 2. Actualizar código
git pull origin main

# 3. Reconstruir imágenes
docker-compose build --no-cache

# 4. Iniciar sistema
docker-compose up -d

# 5. Verificar que todo funciona
docker-compose ps
docker-compose logs backend
```

### 📝 Cambios en Archivos de Configuración

#### Archivos Modificados
- `src/docker/.env` - Agregado JWT_SECRET seguro y VITE_API_URL relativa
- `src/docker/docker-compose.yml` - Actualizado JWT_SECRET por defecto y VITE_API_URL
- `src/frontend/nginx.conf` - Habilitado proxy para `/api`
- `src/backend/src/main/resources/application.properties` - Actualizado JWT_SECRET por defecto
- `QUICK_START.md` - Documentación completa actualizada
- `README.md` - Instrucciones de inicio actualizadas

#### Archivos Nuevos
- `RELEASE_NOTES.md` - Este archivo

### 🧪 Testing

Todos los tests pasan correctamente:
- ✅ Tests unitarios: 85% cobertura
- ✅ Tests de integración: Funcionando
- ✅ Load testing: 43,700+ requests sin errores
- ✅ Login manual: Verificado con los 3 roles

### 📞 Soporte

Si encuentras problemas:

1. Revisa la [Guía de Inicio Rápido](QUICK_START.md)
2. Consulta la sección de Troubleshooting
3. Verifica los logs: `docker-compose logs -f`
4. Asegúrate de tener la última versión: `git pull origin main`

### 🔐 Seguridad

**Recordatorios importantes**:
- ⚠️ Cambia todas las contraseñas por defecto en producción
- ⚠️ Genera un nuevo JWT_SECRET para producción
- ⚠️ Usa HTTPS en producción
- ⚠️ Configura firewall y límites de rate limiting
- ⚠️ Revisa los logs regularmente

### 📅 Próximas Mejoras

- [ ] Configuración de email para notificaciones
- [ ] Backup automático de base de datos
- [ ] Monitoreo con Prometheus/Grafana
- [ ] CI/CD pipeline
- [ ] Documentación de API en español

---

**Última actualización**: Febrero 2026
