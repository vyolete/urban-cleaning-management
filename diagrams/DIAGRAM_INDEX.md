# Índice Completo de Diagramas Arquitectónicos
## Sistema de Gestión de Limpieza Urbana

---

## 📋 Vista General

Este documento proporciona un índice completo de todos los diagramas arquitectónicos del sistema, organizados por vista arquitectónica según el modelo 4+1 de Kruchten.

**Total de diagramas**: 52  
**Fecha de actualización**: 11 de febrero de 2026  
**Estado UML**: ✅ Validado y corregido

---

## 🎯 01. Vista de Casos de Uso (Use Case View)
**Archivos**: `01-use-case-view_diagram_*.mmd` | **Total**: 11 diagramas

### Diagramas de Casos de Uso UML ✅
| Archivo | Nombre del Diagrama | Descripción | Tipo |
|---------|-------------------|-------------|------|
| `01-use-case-view_diagram_.mmd` | **Vista Completa del Sistema** | Diagrama maestro con todos los 49 casos de uso organizados por área funcional | Casos de Uso UML |
| `01-use-case-view_diagram_1.mmd` | **Flujo de Autenticación** | Casos de uso de registro, login, recuperación de contraseña y gestión de sesiones | Casos de Uso UML |
| `01-use-case-view_diagram_2.mmd` | **Gestión de Reportes y Tareas** | Casos de uso para envío de reportes, gestión de tareas y retroalimentación ciudadana | Casos de Uso UML |
| `01-use-case-view_diagram_3.mmd` | **Configuración Administrativa** | Casos de uso exclusivos del administrador para configuración del sistema | Casos de Uso UML |
| `01-use-case-view_diagram_4.mmd` | **Perfil de Usuario y Sesiones** | Gestión de perfil personal, sesiones activas y cumplimiento GDPR | Casos de Uso UML |
| `01-use-case-view_diagram_5.mmd` | **Analíticas y Notificaciones** | Casos de uso para métricas del sistema y gestión de notificaciones | Casos de Uso UML |

### Diagramas de Actividad/Proceso ✅
| Archivo | Nombre del Diagrama | Descripción | Tipo |
|---------|-------------------|-------------|------|
| `01-use-case-view_diagram_6.mmd` | **Proceso de Envío de Reporte** | Flujo completo desde validación hasta creación de tarea con detección de duplicados | Diagrama de Actividad |
| `01-use-case-view_diagram_7.mmd` | **Cálculo de Prioridad** | Algoritmo de cálculo de prioridad con factores de categoría, zona y tiempo | Diagrama de Actividad |
| `01-use-case-view_diagram_8.mmd` | **Actualización de Estado de Tarea** | Flujo de cambio de estado con validación de máquina de estados y evidencia | Diagrama de Actividad |
| `01-use-case-view_diagram_9.mmd` | **Asignación de Tarea** | Proceso de asignación de tareas a operadores con validaciones de rol | Diagrama de Actividad |
| `01-use-case-view_diagram_10.mmd` | **Actualización de Pesos del Algoritmo** | Flujo de actualización de configuración con recálculo masivo de prioridades | Diagrama de Actividad |

---

## 🔄 02. Vista Lógica (Logical View)
**Archivos**: `02-logical-view_diagram_*.mmd` | **Total**: 15 diagramas

### Diagramas de Secuencia
| Archivo | Nombre del Diagrama | Descripción | Componentes Principales |
|---------|-------------------|-------------|------------------------|
| `02-logical-view_diagram_.mmd` | **Secuencia de Login** | Flujo completo de autenticación con JWT y gestión de sesiones | AuthController, AuthService, JwtTokenProvider |
| `02-logical-view_diagram_1.mmd` | **Secuencia de Registro** | Proceso de creación de cuenta con validaciones | AuthController, AuthService, UserRepository |
| `02-logical-view_diagram_2.mmd` | **Secuencia de Envío de Reporte** | Flujo de creación de reporte con detección de duplicados | ReportController, ReportService, DeduplicationService |
| `02-logical-view_diagram_3.mmd` | **Secuencia de Actualización de Tarea** | Cambio de estado de tarea con validaciones | TaskController, TaskService, AuditService |
| `02-logical-view_diagram_4.mmd` | **Secuencia de Cálculo de Prioridad** | Algoritmo de priorización con múltiples factores | PriorityCalculatorService, ConfigService |
| `02-logical-view_diagram_5.mmd` | **Secuencia de Asignación de Tarea** | Asignación de tarea a operador | TaskController, TaskService, NotificationService |
| `02-logical-view_diagram_6.mmd` | **Secuencia de Retroalimentación** | Confirmación/rechazo de resolución por ciudadano | FeedbackController, FeedbackService, TaskService |
| `02-logical-view_diagram_7.mmd` | **Secuencia de Configuración** | Actualización de pesos del algoritmo | ConfigController, ConfigService, TaskService |
| `02-logical-view_diagram_8.mmd` | **Secuencia de Analíticas** | Generación de métricas y reportes | AnalyticsController, AnalyticsService |
| `02-logical-view_diagram_9.mmd` | **Secuencia de Gestión de Sesiones** | Control de sesiones activas y revocación | SessionController, UserSessionService |
| `02-logical-view_diagram_10.mmd` | **Secuencia de Recuperación de Contraseña** | Flujo de reset de contraseña por email | PasswordResetController, EmailService |
| `02-logical-view_diagram_11.mmd` | **Secuencia de Notificaciones** | Sistema de notificaciones por email | NotificationService, EmailService |
| `02-logical-view_diagram_12.mmd` | **Secuencia de Auditoría** | Registro de cambios y trazabilidad | AuditService, AuditLogRepository |
| `02-logical-view_diagram_13.mmd` | **Secuencia de Gestión de Perfil** | Actualización de datos personales | UserController, UserService |
| `02-logical-view_diagram_14.mmd` | **Secuencia de Eliminación de Cuenta** | Proceso GDPR de eliminación de datos | UserController, UserDataService |

---

## 🗄️ 03. Vista del Modelo de Datos (Data Model View)
**Archivos**: `03-data-model-view_diagram_*.mmd` | **Total**: 1 diagrama

| Archivo | Nombre del Diagrama | Descripción | Entidades Principales |
|---------|-------------------|-------------|----------------------|
| `03-data-model-view_diagram_.mmd` | **Modelo Entidad-Relación Completo** | Esquema completo de base de datos con todas las entidades y relaciones | User, Report, Task, AuditLog, AlgorithmConfig, RefreshToken, UserSession |

**Entidades incluidas**: 12 entidades principales con relaciones, índices espaciales PostGIS, y campos de auditoría.

---

## 🏗️ 04. Vista MVC (MVC View)
**Archivos**: `04-mvc-view_diagram_*.mmd` | **Total**: 11 diagramas

### Diagramas de Arquitectura MVC
| Archivo | Nombre del Diagrama | Descripción | Capas Involucradas |
|---------|-------------------|-------------|-------------------|
| `04-mvc-view_diagram_.mmd` | **Flujo MVC General** | Patrón MVC completo desde React hasta PostgreSQL | View (React) → Controller → Service → Repository → DB |
| `04-mvc-view_diagram_1.mmd` | **Capa de Controladores** | Estructura de controladores REST con validación | AuthController, ReportController, TaskController |
| `04-mvc-view_diagram_2.mmd` | **Capa de Servicios** | Lógica de negocio y orquestación | AuthService, ReportService, TaskService |
| `04-mvc-view_diagram_3.mmd` | **Capa de Repositorios** | Acceso a datos con JPA y consultas espaciales | UserRepository, ReportRepository, TaskRepository |
| `04-mvc-view_diagram_4.mmd` | **Capa de Seguridad** | Filtros de seguridad y autenticación JWT | JwtAuthenticationFilter, SecurityConfig |
| `04-mvc-view_diagram_5.mmd` | **Capa de DTOs** | Objetos de transferencia de datos | Request/Response DTOs, Entity mapping |
| `04-mvc-view_diagram_6.mmd` | **Capa de Configuración** | Configuración de Spring Boot | DatabaseConfig, SecurityConfig, JwtConfig |
| `04-mvc-view_diagram_7.mmd` | **Manejo de Excepciones** | Gestión global de errores | GlobalExceptionHandler, Custom Exceptions |
| `04-mvc-view_diagram_8.mmd` | **Validación de Datos** | Validadores personalizados | EmailValidator, PasswordValidator |
| `04-mvc-view_diagram_9.mmd` | **Capa de Eventos** | Sistema de eventos asíncronos | ApplicationEventPublisher, EventListeners |
| `04-mvc-view_diagram_10.mmd` | **Integración Frontend-Backend** | Comunicación React-Spring Boot | Axios, API Services, Error Handling |

---

## ⚙️ 05. Vista de Procesos (Process View)
**Archivos**: `05-process-view_diagram_*.mmd` | **Total**: 8 diagramas

### Diagramas de Procesos de Negocio
| Archivo | Nombre del Diagrama | Descripción | Proceso Principal |
|---------|-------------------|-------------|------------------|
| `05-process-view_diagram_.mmd` | **Proceso Principal de Gestión de Reportes** | Flujo completo desde reporte ciudadano hasta resolución | Reporte → Tarea → Asignación → Resolución → Retroalimentación |
| `05-process-view_diagram_1.mmd` | **Proceso de Detección de Duplicados** | Algoritmo espacial y temporal para identificar reportes duplicados | Consultas PostGIS, Validación de distancia y tiempo |
| `05-process-view_diagram_2.mmd` | **Proceso de Priorización** | Cálculo dinámico de prioridades con múltiples factores | Categoría + Zona + Tiempo = Prioridad |
| `05-process-view_diagram_3.mmd` | **Proceso de Notificaciones** | Sistema de notificaciones por email con reintentos | Eventos → Templates → SMTP → Manejo de fallos |
| `05-process-view_diagram_4.mmd` | **Proceso de Auditoría** | Trazabilidad completa de cambios en el sistema | Captura de eventos → Registro → Consulta histórica |
| `05-process-view_diagram_5.mmd` | **Proceso de Gestión de Sesiones** | Control de sesiones multi-dispositivo | Login → Sesión → Renovación → Revocación |
| `05-process-view_diagram_6.mmd` | **Proceso de Configuración Dinámica** | Actualización de parámetros del sistema | Cambio de configuración → Recálculo → Aplicación |
| `05-process-view_diagram_7.mmd` | **Proceso de Cumplimiento GDPR** | Gestión de datos personales y eliminación | Solicitud → Validación → Anonimización → Confirmación |

---

## 🚀 06. Vista de Despliegue (Deployment View)
**Archivos**: `06-deployment-view_diagram_*.mmd` | **Total**: 4 diagramas

### Diagramas de Infraestructura
| Archivo | Nombre del Diagrama | Descripción | Componentes de Infraestructura |
|---------|-------------------|-------------|-------------------------------|
| `06-deployment-view_diagram_.mmd` | **Arquitectura de Despliegue Completa** | Vista general de la infraestructura en AWS | ECS, RDS, S3, CloudFront, Route 53 |
| `06-deployment-view_diagram_1.mmd` | **Configuración de Contenedores** | Despliegue con Docker y Docker Compose | Backend (Spring Boot), Frontend (Nginx), PostgreSQL |
| `06-deployment-view_diagram_2.mmd` | **Configuración de Red y Seguridad** | VPC, subredes, grupos de seguridad | Load Balancer, Security Groups, NAT Gateway |
| `06-deployment-view_diagram_3.mmd` | **Configuración de Base de Datos** | RDS PostgreSQL con PostGIS | Multi-AZ, Backups, Read Replicas |

---

## 💻 07. Vista de Implementación (Implementation View)
**Archivos**: `07-implementation-view_diagram_*.mmd` | **Total**: 2 diagramas

### Diagramas de Estructura de Código
| Archivo | Nombre del Diagrama | Descripción | Estructura |
|---------|-------------------|-------------|------------|
| `07-implementation-view_diagram_.mmd` | **Estructura de Paquetes Backend** | Organización del código Java/Spring Boot | com.urbanclean.{controller,service,repository,entity} |
| `07-implementation-view_diagram_1.mmd` | **Estructura de Componentes Frontend** | Organización del código React | src/{components,pages,services,hooks,context} |

---

## 📊 Resumen por Tipo de Diagrama

| Tipo de Diagrama | Cantidad | Archivos |
|------------------|----------|----------|
| **Casos de Uso UML** | 6 | `01-use-case-view_diagram_[0-5].mmd` |
| **Diagramas de Actividad** | 5 | `01-use-case-view_diagram_[6-10].mmd` |
| **Diagramas de Secuencia** | 15 | `02-logical-view_diagram_*.mmd` |
| **Modelo Entidad-Relación** | 1 | `03-data-model-view_diagram_.mmd` |
| **Diagramas MVC** | 11 | `04-mvc-view_diagram_*.mmd` |
| **Diagramas de Proceso** | 8 | `05-process-view_diagram_*.mmd` |
| **Diagramas de Despliegue** | 4 | `06-deployment-view_diagram_*.mmd` |
| **Diagramas de Implementación** | 2 | `07-implementation-view_diagram_*.mmd` |
| **TOTAL** | **52** | **Todos los archivos .mmd** |

---

## 🎯 Casos de Uso por Actor

### 👤 Anonymous User (8 casos de uso)
- UC-001: Register User
- UC-002: Login  
- UC-003: Refresh Access Token
- UC-004: Submit Report
- UC-005: Initiate Password Reset
- UC-006: Validate Reset Token
- UC-007: Complete Password Reset
- UC-008: Unsubscribe from Notifications

### 👤 Citizen - ROLE_CIUDADANO (19 casos de uso)
- UC-009: Get My Reports
- UC-010: Get User Profile
- UC-011: Update User Profile
- UC-012: Change Password
- UC-014: Request Account Deletion
- UC-015: Cancel Account Deletion
- UC-016: Export User Data
- UC-017: Confirm Task Resolution
- UC-018: Reject Task Resolution
- UC-019: Get Task Feedback
- UC-020: Get Notification Preferences
- UC-021: Update Notification Preferences
- UC-022: Logout
- UC-023: Logout from All Devices
- UC-024: Get Active Sessions
- UC-025: Get All Sessions
- UC-026: Revoke Specific Session
- UC-027: Revoke Other Sessions

### 👤 Operator - ROLE_TECNICO (10 casos de uso adicionales)
- UC-028: Get All Reports
- UC-029: Get Report by ID
- UC-030: Get All Tasks
- UC-031: Get Task by ID
- UC-032: Update Task State
- UC-033: Get Task Audit History
- UC-034: Get Task Distribution
- UC-035: Get Heatmap
- UC-036: Get MTTR
- UC-037: Get Operator Performance

### 👤 Administrator - ROLE_ADMIN (12 casos de uso exclusivos)
- UC-038: Assign Task to Operator
- UC-039: Get Algorithm Weights
- UC-040: Update Algorithm Weights
- UC-041: Get Configuration History
- UC-042: Get Token Expiration Config
- UC-043: Update Token Expiration Config
- UC-044: Get Duplicate Detection Config
- UC-045: Update Duplicate Detection Config
- UC-046: Get Notification Failures
- UC-047: Retry Failed Notification
- UC-048: Get Performance Metrics
- UC-049: Get Performance Alerts

---

## 🔧 Herramientas de Exportación

### Conversión a Imágenes
```bash
# Opción 1: Mermaid CLI (recomendado)
npm install -g @mermaid-js/mermaid-cli
mmdc -i diagrams/archivo.mmd -o diagrams/archivo.png

# Opción 2: Mermaid Live Editor
# 1. Ve a https://mermaid.live
# 2. Copia el contenido del archivo .mmd
# 3. Exporta como PNG/SVG
```

### Validación UML
```bash
# Ejecutar validación completa
./export-diagrams.sh
```

---

## 📝 Notas de Mantenimiento

- **Estado UML**: ✅ Todos los diagramas de casos de uso cumplen con UML 2.5
- **Última actualización**: 11 de febrero de 2026
- **Herramienta de validación**: `export-diagrams.sh`
- **Formato**: Mermaid (.mmd)
- **Documentación fuente**: `docs/architecture/*.md`

---

*Este índice se genera automáticamente y se actualiza con cada ejecución del script `export-diagrams.sh`*