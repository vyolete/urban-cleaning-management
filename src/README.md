# Código Fuente - URBIX

Este directorio contiene todo el código fuente del sistema URBIX.

## Estructura

### 📁 backend/
Aplicación Spring Boot que proporciona la API REST del sistema.
- **Tecnología**: Spring Boot 3.2, PostgreSQL, PostGIS
- **Puerto**: 8080
- **Documentación**: [backend/README.md](backend/README.md)

### 📁 frontend/
Aplicación React que proporciona la interfaz de usuario.
- **Tecnología**: React 18, Leaflet, Axios
- **Puerto**: 3000
- **Documentación**: [frontend/README.md](frontend/README.md)

### 📁 docker/
Configuración de contenedores para desarrollo y despliegue.
- **Tecnología**: Docker, Docker Compose
- **Documentación**: [docker/README.md](docker/README.md)

## Inicio Rápido

```bash
# Ejecutar sistema completo
cd docker
docker-compose up -d

# Verificar estado
docker-compose ps
```

## Desarrollo Local

### Backend
```bash
cd backend
./mvnw spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm start
```

### Base de Datos
```bash
cd docker
docker-compose up -d postgres
```

## Testing

```bash
# Tests unitarios backend
cd backend
./mvnw test

# Tests frontend
cd frontend
npm test
```

## Más Información

- **Arquitectura**: [../docs/architecture/](../docs/architecture/)
- **API Documentation**: [../docs/api/](../docs/api/)
- **Guía de inicio**: [../QUICK_START.md](../QUICK_START.md)