# Guía de Inicio Rápido - URBIX

## Requisitos Previos

- **Docker** y **Docker Compose** instalados
- **Git** para clonar el repositorio
- **Puertos disponibles**: 3000 (frontend), 8080 (backend), 5432 (database)

## Instalación y Ejecución

### 1. Clonar el Repositorio
```bash
git clone <repository-url>
cd URBIX-TFM
```

### 2. Ejecutar el Sistema Completo
```bash
cd src/docker
docker-compose up -d
```

### 3. Verificar el Despliegue
```bash
# Verificar que todos los contenedores estén ejecutándose
docker-compose ps

# Ver logs si hay problemas
docker-compose logs
```

### 4. Acceder al Sistema

- **Frontend (Ciudadanos/Operadores)**: http://localhost:3000
- **API Backend**: http://localhost:8080
- **Documentación API**: http://localhost:8080/swagger-ui.html
- **Base de Datos**: localhost:5432 (postgres/postgres)

## Usuarios de Prueba

### Ciudadano
- **Email**: ciudadano@urbanclean.com
- **Password**: Ciudadano123!@#

### Operador Municipal (Técnico)
- **Email**: tecnico@urbanclean.com
- **Password**: Tecnico123!@#

### Administrador
- **Email**: admin@urbanclean.com
- **Password**: Admin123!@#

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

### Backend (Spring Boot)
```bash
cd src/backend
./mvnw spring-boot:run
```

### Frontend (React)
```bash
cd src/frontend
npm install
npm start
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

### Ejecutar Load Testing
```bash
cd scripts/testing
./test-performance-metrics.sh
```

## Troubleshooting

### Problemas Comunes

1. **Puerto ocupado**: Cambiar puertos en docker-compose.yml
2. **Permisos de Docker**: Ejecutar con sudo o añadir usuario a grupo docker
3. **Base de datos no conecta**: Verificar que PostgreSQL esté ejecutándose

### Logs Útiles
```bash
# Logs del backend
docker-compose logs backend

# Logs del frontend  
docker-compose logs frontend

# Logs de la base de datos
docker-compose logs postgres
```

### Reiniciar Sistema
```bash
docker-compose down
docker-compose up -d
```

## Más Información

- **Documentación completa**: [docs/](docs/)
- **Arquitectura del sistema**: [docs/architecture/](docs/architecture/)
- **Troubleshooting avanzado**: [docs/operations/troubleshooting.md](docs/operations/troubleshooting.md)
