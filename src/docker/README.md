# Docker Deployment - Urban Cleaning Management System

Este directorio contiene la configuración de Docker para desplegar el sistema completo.

## 📋 Contenido

- `docker-compose.yml` - Configuración de servicios (PostgreSQL, Backend, Frontend)
- `init-db.sql` - Script de inicialización de base de datos
- `.env.example` - Plantilla de variables de entorno

## 🚀 Inicio Rápido

### 1. Configurar Variables de Entorno

```bash
# Copiar plantilla de variables de entorno
cp .env.example .env

# Editar .env y cambiar valores por defecto (especialmente passwords y secrets)
nano .env
```

**⚠️ IMPORTANTE**: Cambiar los siguientes valores en producción:
- `DB_PASSWORD` - Contraseña de PostgreSQL
- `JWT_SECRET` - Clave secreta para JWT (mínimo 256 bits)

### 2. Construir e Iniciar Servicios

```bash
# Construir imágenes y iniciar servicios
docker-compose up -d --build

# Ver logs de todos los servicios
docker-compose logs -f

# Ver logs de un servicio específico
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres
```

### 3. Verificar Estado de Servicios

```bash
# Ver estado de contenedores
docker-compose ps

# Verificar health checks
docker-compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
```

### 4. Acceder a la Aplicación

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **Backend Health**: http://localhost:8080/actuator/health
- **PostgreSQL**: localhost:5432

## 🛠️ Comandos Útiles

### Gestión de Servicios

```bash
# Detener servicios
docker-compose stop

# Iniciar servicios detenidos
docker-compose start

# Reiniciar servicios
docker-compose restart

# Detener y eliminar contenedores
docker-compose down

# Detener y eliminar contenedores + volúmenes (⚠️ elimina datos)
docker-compose down -v
```

### Logs y Debugging

```bash
# Ver logs en tiempo real
docker-compose logs -f

# Ver últimas 100 líneas de logs
docker-compose logs --tail=100

# Ver logs de un servicio específico
docker-compose logs -f backend

# Ejecutar comando en contenedor
docker-compose exec backend sh
docker-compose exec postgres psql -U urbanclean_user -d urbanclean
```

### Reconstruir Servicios

```bash
# Reconstruir un servicio específico
docker-compose up -d --build backend

# Reconstruir todos los servicios
docker-compose up -d --build

# Forzar recreación de contenedores
docker-compose up -d --force-recreate
```

## 🗄️ Gestión de Base de Datos

### Acceder a PostgreSQL

```bash
# Conectar a PostgreSQL
docker-compose exec postgres psql -U urbanclean_user -d urbanclean

# Ejecutar query desde línea de comandos
docker-compose exec postgres psql -U urbanclean_user -d urbanclean -c "SELECT version();"
```

### Backup y Restore

```bash
# Crear backup
docker-compose exec postgres pg_dump -U urbanclean_user urbanclean > backup.sql

# Restaurar backup
docker-compose exec -T postgres psql -U urbanclean_user urbanclean < backup.sql

# Backup con compresión
docker-compose exec postgres pg_dump -U urbanclean_user urbanclean | gzip > backup.sql.gz
```

### Verificar PostGIS

```bash
# Verificar extensión PostGIS
docker-compose exec postgres psql -U urbanclean_user -d urbanclean -c "SELECT PostGIS_Version();"

# Listar extensiones instaladas
docker-compose exec postgres psql -U urbanclean_user -d urbanclean -c "\dx"
```

## 📊 Monitoreo

### Health Checks

Todos los servicios tienen health checks configurados:

```bash
# Ver estado de health checks
docker inspect urbanclean-backend | grep -A 10 Health
docker inspect urbanclean-frontend | grep -A 10 Health
docker inspect urbanclean-postgres | grep -A 10 Health
```

### Métricas de Recursos

```bash
# Ver uso de recursos
docker stats

# Ver uso de recursos de servicios específicos
docker stats urbanclean-backend urbanclean-frontend urbanclean-postgres
```

## 🔧 Configuración Avanzada

### Variables de Entorno

Todas las variables de entorno están documentadas en `.env.example`:

| Variable | Descripción | Valor por Defecto |
|----------|-------------|-------------------|
| `DB_NAME` | Nombre de la base de datos | `urbanclean` |
| `DB_USER` | Usuario de PostgreSQL | `urbanclean_user` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `password` |
| `JWT_SECRET` | Clave secreta para JWT | (cambiar en producción) |
| `BACKEND_PORT` | Puerto del backend | `8080` |
| `FRONTEND_PORT` | Puerto del frontend | `3000` |

### Volúmenes

El sistema utiliza volúmenes persistentes:

- `postgres_data` - Datos de PostgreSQL
- `backend_uploads` - Archivos subidos (fotos de reportes)

```bash
# Listar volúmenes
docker volume ls

# Inspeccionar volumen
docker volume inspect docker_postgres_data

# Eliminar volúmenes no utilizados (⚠️ cuidado)
docker volume prune
```

### Red

Los servicios se comunican a través de una red bridge personalizada:

```bash
# Inspeccionar red
docker network inspect docker_urbanclean-network

# Ver IPs de contenedores
docker-compose exec backend hostname -i
```

## 🐛 Troubleshooting

### Backend no inicia

```bash
# Ver logs detallados
docker-compose logs backend

# Verificar que PostgreSQL esté listo
docker-compose exec postgres pg_isready -U urbanclean_user

# Reiniciar backend
docker-compose restart backend
```

### Frontend no carga

```bash
# Verificar logs de Nginx
docker-compose logs frontend

# Verificar que el build se completó
docker-compose exec frontend ls -la /usr/share/nginx/html

# Probar health check
curl http://localhost:3000/health
```

### Problemas de conexión a base de datos

```bash
# Verificar conectividad desde backend
docker-compose exec backend ping postgres

# Verificar variables de entorno
docker-compose exec backend env | grep SPRING_DATASOURCE

# Reiniciar PostgreSQL
docker-compose restart postgres
```

### Limpiar todo y empezar de nuevo

```bash
# Detener y eliminar todo (⚠️ elimina datos)
docker-compose down -v

# Eliminar imágenes
docker-compose down --rmi all

# Reconstruir desde cero
docker-compose up -d --build
```

## 📦 Producción

### Consideraciones de Seguridad

1. **Cambiar credenciales por defecto**:
   - `DB_PASSWORD`
   - `JWT_SECRET`

2. **Usar HTTPS**:
   - Configurar certificados SSL/TLS
   - Usar reverse proxy (Nginx, Traefik)

3. **Limitar puertos expuestos**:
   - No exponer PostgreSQL (5432) públicamente
   - Usar firewall

4. **Configurar backups automáticos**:
   - Programar backups de PostgreSQL
   - Almacenar en ubicación segura

### Optimizaciones

1. **Recursos**:
   - Ajustar límites de memoria y CPU en docker-compose.yml
   - Configurar JVM heap size para backend

2. **Logging**:
   - Configurar log rotation
   - Enviar logs a sistema centralizado

3. **Monitoreo**:
   - Integrar con Prometheus/Grafana
   - Configurar alertas

## 📚 Referencias

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [PostgreSQL Docker Image](https://hub.docker.com/_/postgres)
- [PostGIS Docker Image](https://hub.docker.com/r/postgis/postgis)
- [Nginx Docker Image](https://hub.docker.com/_/nginx)

## 🆘 Soporte

Para problemas o preguntas:
1. Revisar logs: `docker-compose logs`
2. Verificar configuración: `.env` y `docker-compose.yml`
3. Consultar documentación del proyecto principal
