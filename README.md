# Urban Cleaning Management System

Sistema colaborativo de gestión de limpieza urbana - Trabajo de Fin de Máster (TFM)

## 📋 Descripción

WebApp Full-Stack donde los ciudadanos reportan incidencias geolocalizadas de limpieza urbana y un sistema backend prioriza automáticamente estas tareas para los operarios municipales mediante un algoritmo inteligente.

## 🎯 Características Principales

- **Reportes Ciudadanos**: Interfaz para que los ciudadanos reporten incidencias con geolocalización y fotos
- **Priorización Automática**: Algoritmo configurable que calcula la urgencia de cada tarea: `P = (Wc × Categoría) + (Wz × Zona) + (Wt × Tiempo)`
- **Deduplicación Inteligente**: Detección automática de reportes duplicados por proximidad espacial y temporal
- **Dashboard Operativo**: Panel para operadores con visualización de tareas ordenadas por prioridad
- **Gestión de Estados**: Máquina de estados para el ciclo de vida de tareas (PENDIENTE → ASIGNADO → EN_PROGRESO → RESUELTO)
- **Auditoría Completa**: Registro inmutable de todos los cambios de estado
- **Control de Acceso**: Sistema RBAC con roles (Ciudadano, Técnico, Administrador)

## 🛠️ Stack Tecnológico

### Backend
- **Framework**: Spring Boot (Java 17)
- **Base de Datos**: PostgreSQL 15 + PostGIS 3.3
- **Seguridad**: JWT + Spring Security + BCrypt
- **Build**: Maven

### Frontend
- **Framework**: React 18
- **Mapas**: Leaflet / React-Leaflet
- **HTTP Client**: Axios
- **Routing**: React Router

### DevOps
- **Containerización**: Docker + Docker Compose
- **CI/CD**: GitHub Actions (próximamente)

## 📁 Estructura del Proyecto

```
urban-cleaning-management/
├── backend/              # Aplicación Spring Boot
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/urbanclean/
│   │   │   │   ├── config/
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── repository/
│   │   │   │   ├── entity/
│   │   │   │   ├── dto/
│   │   │   │   ├── security/
│   │   │   │   └── exception/
│   │   │   └── resources/
│   │   └── test/
│   └── pom.xml
├── frontend/             # Aplicación React
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── services/
│   │   ├── hooks/
│   │   ├── context/
│   │   └── utils/
│   └── package.json
├── docker/               # Configuración Docker
│   ├── docker-compose.yml
│   └── init-db.sql
└── .kiro/                # Especificaciones del proyecto
    ├── specs/
    │   └── urban-cleaning-management/
    │       ├── requirements.md
    │       ├── design.md
    │       └── tasks.md
    └── steering/
        └── urban-cleaning-project-standards.md
```

## 🚀 Inicio Rápido

### Prerrequisitos

- Java 17+
- Node.js 18+
- Docker & Docker Compose
- Maven 3.8+

### Instalación con Docker

1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/[tu-usuario]/urban-cleaning-management.git
   cd urban-cleaning-management
   ```

2. **Configurar variables de entorno**
   ```bash
   cp .env.example .env
   # Editar .env con tus configuraciones
   ```

3. **Iniciar servicios con Docker Compose**
   ```bash
   docker-compose up -d
   ```

4. **Acceder a la aplicación**
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080/api
   - PostgreSQL: localhost:5432

### Desarrollo Local

#### Backend
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

#### Frontend
```bash
cd frontend
npm install
npm start
```

## 📚 Documentación

- **[Requirements](/.kiro/specs/urban-cleaning-management/requirements.md)**: Requisitos funcionales detallados (EARS + INCOSE)
- **[Design](/.kiro/specs/urban-cleaning-management/design.md)**: Arquitectura y diseño del sistema
- **[Tasks](/.kiro/specs/urban-cleaning-management/tasks.md)**: Plan de implementación
- **[Standards](/.kiro/steering/urban-cleaning-project-standards.md)**: Estándares de desarrollo

## 🔑 Roles y Permisos

| Rol | Permisos |
|-----|----------|
| **ROLE_CIUDADANO** | Crear reportes, ver propios reportes |
| **ROLE_TECNICO** | Ver todas las tareas, actualizar estados, asignar tareas |
| **ROLE_ADMIN** | Configurar algoritmo, gestionar usuarios, acceso completo |

## 🧪 Testing

### Ejecutar Tests Unitarios
```bash
cd backend
mvn test
```

### Ejecutar Tests de Propiedades (Property-Based Testing)
```bash
cd backend
mvn test -Dtest="**/*PropertyTest"
```

## 📊 Algoritmo de Priorización

El sistema calcula la prioridad de cada tarea usando la siguiente fórmula:

```
P = (Wc × Categoría) + (Wz × Zona) + (Wt × Tiempo)
```

Donde:
- **Wc**: Peso de la categoría (configurable)
- **Categoría**: Valor de severidad de la categoría del reporte
- **Wz**: Peso de la zona (configurable)
- **Zona**: Índice de riesgo de la zona geográfica
- **Wt**: Peso del tiempo (configurable)
- **Tiempo**: Horas transcurridas desde el reporte

Los pesos son configurables por administradores y se almacenan en la base de datos.

## 🗺️ Características Geoespaciales

- **Geolocalización**: Captura automática de ubicación del navegador
- **Geofencing**: Validación de coordenadas dentro de límites configurados
- **Deduplicación Espacial**: Detección de reportes duplicados por proximidad (< X metros)
- **Visualización en Mapa**: Leaflet para mostrar tareas con marcadores priorizados
- **Consultas PostGIS**: Queries espaciales optimizadas con índices GIST

## 🔐 Seguridad

- **Autenticación**: JWT con expiración de 24 horas
- **Hashing de Contraseñas**: BCrypt con salt
- **CORS**: Configurado para orígenes autorizados
- **Headers de Seguridad**: X-Content-Type-Options, X-Frame-Options, X-XSS-Protection
- **Rate Limiting**: Protección contra fuerza bruta en endpoints de autenticación
- **Validación de Entrada**: Sanitización para prevenir inyecciones

## 🔄 Flujo de Trabajo

### Ciudadano
1. Accede a la aplicación
2. Permite geolocalización
3. Selecciona categoría de incidencia
4. Añade descripción y foto
5. Envía reporte

### Sistema
1. Valida coordenadas (geofencing)
2. Almacena reporte con foto
3. Busca duplicados (espacial + temporal)
4. Calcula prioridad
5. Crea/actualiza tarea
6. Notifica operadores

### Operador
1. Accede al dashboard
2. Ve tareas ordenadas por prioridad
3. Filtra por estado/zona
4. Asigna tarea
5. Actualiza estado (EN_PROGRESO)
6. Marca como RESUELTO

## 🔌 API Endpoints

### Autenticación
- `POST /api/auth/login` - Iniciar sesión
- `POST /api/auth/register` - Registrar usuario

### Reportes
- `POST /api/reports` - Crear reporte (multipart: JSON + foto)
- `GET /api/reports` - Listar todos los reportes (TECNICO/ADMIN)
- `GET /api/reports/{id}` - Obtener reporte por ID
- `GET /api/reports/my` - Obtener mis reportes

### Tareas
- `GET /api/tasks` - Listar tareas con filtros (estado, zona geográfica)
- `GET /api/tasks/{id}` - Obtener detalle de tarea
- `PATCH /api/tasks/{id}/state` - Actualizar estado de tarea
- `GET /api/tasks/{id}/audit-history` - Historial de auditoría

### Configuración (Admin)
- `GET /api/admin/config/algorithm-weights` - Obtener configuración actual
- `PUT /api/admin/config/algorithm-weights` - Actualizar pesos del algoritmo
- `GET /api/admin/config/algorithm-weights/history` - Historial de configuraciones

## 📈 Estado del Proyecto

🚧 **En Desarrollo Activo** - Trabajo de Fin de Máster

### Fase 1: Planificación ✅
- [x] Especificaciones completas (EARS + INCOSE)
- [x] Diseño de arquitectura con 47 propiedades de correctitud
- [x] Plan de implementación con 25 tareas

### Fase 2: Backend ✅ (100% Completado)
- [x] Estructura del proyecto e inicialización
- [x] Modelo de datos y entidades JPA
- [x] Autenticación y autorización (JWT + Spring Security)
- [x] Módulo de reportes con validación geoespacial
- [x] **Algoritmo de priorización** (P = Wc×Cat + Wz×Zona + Wt×Tiempo)
- [x] **Servicio de deduplicación** (PostGIS ST_DWithin)
- [x] Gestión de tareas y máquina de estados
- [x] Sistema de auditoría inmutable
- [x] Endpoints REST para operadores
- [x] Configuración dinámica del algoritmo
- [x] Manejo global de excepciones
- [x] Rate limiting para endpoints de autenticación

### Fase 3: Frontend ✅ (100% Completado)
- [x] Estructura del proyecto React
- [x] Capa de servicios API (Axios)
- [x] Contexto de autenticación
- [x] **Interfaz de ciudadano para reportes**
  - [x] Hook de geolocalización
  - [x] Formulario de reporte con validación
  - [x] Visualización de mapa con Leaflet
  - [x] Upload de fotos con preview
- [x] **Dashboard de operadores**
  - [x] Lista de tareas con filtros
  - [x] Mapa de tareas con marcadores priorizados
  - [x] Detalle de tarea con transiciones de estado
  - [x] Timeline de auditoría
- [x] **Panel de administración**
  - [x] Configuración de pesos del algoritmo
  - [x] Historial de configuraciones
- [x] **Páginas de autenticación**
  - [x] LoginPage con validación
  - [x] ProtectedRoute con control de acceso por roles
  - [x] Configuración de rutas completa
  - [x] Página 404 personalizada

### Fase 4: Despliegue (Pendiente)
- [ ] Dockerfiles optimizados
- [ ] Docker Compose completo
- [ ] Scripts de inicialización de BD
- [ ] Configuración de producción

### Últimos Commits
- ✅ **Checkpoint de integración Frontend-Backend completado**
- ✅ Configuración CORS actualizada para Vite dev server
- ✅ Script de prueba de integración creado
- ✅ Checklist de integración documentado
- ✅ Páginas de autenticación completas (LoginPage, ProtectedRoute, routing, 404)
- ✅ Panel de administración completo (ConfigPanel, AdminConfigPage)
- ✅ Dashboard de operadores completo (TaskList, TaskMap, TaskDetail, AuditTimeline)
- ✅ Interfaz de ciudadano completa (ReportForm, MapView, CitizenReportPage)
- ✅ Hook de geolocalización con validación de geofencing
- ✅ Capa de servicios API con interceptores JWT
- ✅ Contexto de autenticación React
- ✅ Rate limiting para endpoints de autenticación
- ✅ Manejo global de excepciones con respuestas consistentes
- ✅ Gestión de configuración del algoritmo para administradores
- ✅ Endpoints de gestión de tareas con filtros y auditoría
- ✅ Sistema de auditoría para cambios de estado
- ✅ Servicio de deduplicación con PostGIS
- ✅ Algoritmo de cálculo de prioridad implementado
- ✅ Módulo de reportes con geofencing y almacenamiento de fotos

## 👥 Autor

**[Tu Nombre]** - Trabajo de Fin de Máster

## 📄 Licencia

Este proyecto es parte de un Trabajo de Fin de Máster y está sujeto a las políticas académicas de la institución.

## 🙏 Agradecimientos

- Universidad [Nombre]
- Director/a de TFM: [Nombre]
- Comunidad Open Source

---

**Nota**: Este es un proyecto académico desarrollado como Trabajo de Fin de Máster.
