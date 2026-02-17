# Guía de Inicio Rápido - URBIX

## Requisitos Previos

- **Docker** y **Docker Compose** instalados
- **Git** para clonar el repositorio
- **Puertos disponibles**: 3000 (frontend), 8080 (backend), 5432 (database)

## Instalación y Ejecución

### 1. Clonar el Repositorio
```bash
git clone <repository-url>
cd urban-cleaning-management
```

### 2. Configurar Variables de Entorno (Opcional)

El sistema funciona con valores por defecto, pero puedes personalizar la configuración:

```bash
cd src/docker
cp .env.example .env
# Editar .env si necesitas cambiar configuraciones
```

**Nota importante**: El archivo `.env` ya está configurado con valores seguros. Solo necesitas modificarlo si quieres cambiar puertos o configuraciones específicas.

### 3. Ejecutar el Sistema Completo

```bash
cd src/docker
docker-compose up -d
```

Este comando:
- Descarga las imágenes necesarias
- Construye el backend y frontend
- Inicia PostgreSQL con PostGIS
- Crea los usuarios de prueba automáticamente
- Configura el proxy nginx

**Tiempo estimado**: 2-3 minutos en la primera ejecución

### 4. Verificar el Despliegue

```bash
# Verificar que todos los contenedores estén ejecutándose
docker-compose ps

# Deberías ver 3 contenedores con estado "healthy":
# - urbanclean-postgres
# - urbanclean-backend
# - urbanclean-frontend

# Ver logs si hay problemas
docker-compose logs backend
docker-compose logs frontend
```

### 5. Acceder al Sistema

- **Frontend (Aplicación Web)**: http://localhost:3000
- **Página de Login**: http://localhost:3000/login
- **API Backend**: http://localhost:8080
- **Documentación API (Swagger)**: http://localhost:8080/swagger-ui.html
- **Base de Datos**: localhost:5432 (usuario: urbanclean_user, password: change_this_password_in_production)

## Usuarios de Prueba

El sistema crea automáticamente tres usuarios de prueba al iniciar:

### Ciudadano
- **Username**: `ciudadano`
- **Email**: ciudadano@urbanclean.com
- **Password**: `Ciudadano123!@#`
- **Permisos**: Crear reportes de incidencias

### Operador Municipal (Técnico)
- **Username**: `tecnico`
- **Email**: tecnico@urbanclean.com
- **Password**: `Tecnico123!@#`
- **Permisos**: Gestionar tareas, ver dashboard de operaciones

### Administrador
- **Username**: `admin`
- **Email**: admin@urbanclean.com
- **Password**: `Admin123!@#`
- **Permisos**: Acceso completo, configuración del sistema

## Funcionalidades Principales

### Para Ciudadanos
1. Registrarse en el sistema
2. Reportar incidentes con geolocalización
3. Subir fotografías como evidencia
4. Seguir el estado de sus reportes

### Para Operadores
1. Ver dashboard de tareas priorizadas
2. Gestionar estados de tareas
3. Visualizar incidentes en mapa
4. Acceder a métricas de rendimiento

### Para Administradores
1. Configurar algoritmo de priorización
2. Gestionar usuarios y roles
3. Ver analítica operacional
4. Exportar datos del sistema

## Desarrollo Local

Si necesitas ejecutar los servicios individualmente para desarrollo:

### Backend (Spring Boot)
```bash
cd src/backend
./mvnw spring-boot:run
```

### Frontend (React)
```bash
cd src/frontend
npm install
npm run dev
```

### Base de Datos (PostgreSQL + PostGIS)
```bash
cd src/docker
docker-compose up -d postgres
```

## Testing

### Ejecutar Tests Unitarios
```bash
cd src/backend
./mvnw test
```

### Ejecutar Tests de Integración
```bash
cd src/backend
./mvnw verify
```

### Ejecutar Load Testing
```bash
cd scripts/testing
./test-performance-metrics.sh
```

## Troubleshooting

### Problemas Comunes

#### 1. Puerto ocupado
**Error**: `Bind for 0.0.0.0:3000 failed: port is already allocated`

**Solución**: Cambiar puertos en el archivo `.env`:
```bash
FRONTEND_PORT=3001
BACKEND_PORT=8081
DB_PORT=5433
```

#### 2. Permisos de Docker
**Error**: `permission denied while trying to connect to the Docker daemon`

**Solución**:
```bash
# Opción 1: Ejecutar con sudo
sudo docker-compose up -d

# Opción 2: Añadir usuario a grupo docker
sudo usermod -aG docker $USER
# Cerrar sesión y volver a iniciar
```

#### 3. Base de datos no conecta
**Error**: `Connection refused` o `password authentication failed`

**Solución**:
```bash
# Verificar que PostgreSQL esté ejecutándose
docker-compose ps postgres

# Reiniciar el contenedor de base de datos
docker-compose restart postgres

# Ver logs para más detalles
docker-compose logs postgres
```

#### 4. Frontend no carga o muestra página en blanco
**Solución**:
```bash
# Reconstruir el frontend
docker-compose build --no-cache frontend
docker-compose up -d frontend

# Verificar logs
docker-compose logs frontend
```

#### 5. Error de autenticación JWT
**Error**: `WeakKeyException` o problemas de login

**Solución**: El sistema ya está configurado con un JWT_SECRET seguro. Si modificaste la configuración, asegúrate de que el JWT_SECRET tenga al menos 512 bits (64 bytes en base64).

### Logs Útiles

```bash
# Logs del backend
docker-compose logs -f backend

# Logs del frontend  
docker-compose logs -f frontend

# Logs de la base de datos
docker-compose logs -f postgres

# Logs de todos los servicios
docker-compose logs -f
```

### Reiniciar Sistema

```bash
# Reiniciar todos los servicios
docker-compose restart

# Detener y eliminar contenedores (mantiene datos)
docker-compose down

# Detener y eliminar todo (incluyendo volúmenes de datos)
docker-compose down -v

# Iniciar de nuevo
docker-compose up -d
```

### Limpiar y Reconstruir

Si tienes problemas persistentes:

```bash
# Detener todo
docker-compose down -v

# Eliminar imágenes
docker rmi docker-backend docker-frontend

# Reconstruir sin caché
docker-compose build --no-cache

# Iniciar
docker-compose up -d
```

## Configuración Avanzada

### Variables de Entorno Disponibles

El archivo `.env` en `src/docker/` contiene todas las configuraciones:

```bash
# Base de Datos
DB_NAME=urbanclean
DB_USER=urbanclean_user
DB_PASSWORD=change_this_password_in_production
DB_PORT=5432

# Backend
BACKEND_PORT=8080
SHOW_SQL=false

# JWT (Ya configurado con valor seguro)
JWT_SECRET=<valor-seguro-512-bits>
JWT_EXPIRATION=86400000

# Frontend
FRONTEND_PORT=3000
VITE_API_URL=/api

# Geofencing (Madrid por defecto)
GEOFENCE_MIN_LAT=40.3
GEOFENCE_MAX_LAT=40.6
GEOFENCE_MIN_LON=-3.9
GEOFENCE_MAX_LON=-3.5

# Algoritmo de Priorización
ALGORITHM_WEIGHT_CATEGORY=0.40
ALGORITHM_WEIGHT_ZONE=0.35
ALGORITHM_WEIGHT_TIME=0.25

# Deduplicación
DEDUPLICATION_DISTANCE_METERS=50.0
DEDUPLICATION_TIME_WINDOW_HOURS=24
```

### Acceso a la Base de Datos

Para conectarte directamente a PostgreSQL:

```bash
# Desde línea de comandos
docker exec -it urbanclean-postgres psql -U urbanclean_user -d urbanclean

# Desde herramientas GUI (DBeaver, pgAdmin, etc.)
Host: localhost
Port: 5432
Database: urbanclean
Username: urbanclean_user
Password: change_this_password_in_production
```

## Más Información

- **Documentación completa**: [docs/](docs/)
- **Arquitectura del sistema**: [docs/architecture/](docs/architecture/)
- **Documentación de API**: http://localhost:8080/swagger-ui.html (cuando el sistema esté ejecutándose)
- **Troubleshooting avanzado**: [docs/operations/troubleshooting.md](docs/operations/troubleshooting.md)
- **Guía de testing**: [E2E_TESTING_GUIDE.md](E2E_TESTING_GUIDE.md)

## Notas Importantes

1. **Primera ejecución**: La primera vez que ejecutes el sistema, Docker descargará las imágenes base y construirá los contenedores. Esto puede tomar varios minutos.

2. **Usuarios de prueba**: Los usuarios se crean automáticamente solo si la base de datos está vacía. Si necesitas recrearlos, elimina el volumen de datos: `docker-compose down -v`

3. **Seguridad**: Las contraseñas y secretos por defecto son para desarrollo. En producción, DEBES cambiarlos en el archivo `.env`

4. **Proxy nginx**: El frontend usa nginx como proxy para las peticiones a `/api`, evitando problemas de CORS.

5. **Persistencia de datos**: Los datos de la base de datos se guardan en un volumen Docker y persisten entre reinicios.
