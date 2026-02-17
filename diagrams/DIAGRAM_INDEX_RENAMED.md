# Índice de Diagramas Arquitectónicos - Nombres Descriptivos
## Sistema de Gestión de Limpieza Urbana

---

## 📋 Vista General

**Total de diagramas**: 52  
**Fecha de renombrado**: $(date '+%d de %B de %Y')  
**Estado**: ✅ Renombrados con nombres descriptivos para TFM

---

## 🎯 01. Vista de Casos de Uso (Use Case View)

### Diagramas de Casos de Uso UML ✅
| Archivo Renombrado | Descripción | Actores Principales |
|-------------------|-------------|-------------------|
| `use-case-complete-system-overview.mmd` | **Vista Completa del Sistema** - Todos los 49 casos de uso organizados por área funcional | Anonymous, Citizen, Operator, Administrator |
| `use-case-authentication-flow.mmd` | **Flujo de Autenticación** - Registro, login, recuperación de contraseña | Anonymous, Authenticated User |
| `use-case-report-task-management.mmd` | **Gestión de Reportes y Tareas** - Envío de reportes, gestión de tareas, retroalimentación | Citizen, Operator, Administrator |
| `use-case-admin-configuration.mmd` | **Configuración Administrativa** - Casos de uso exclusivos del administrador | Administrator |
| `use-case-user-profile-sessions.mmd` | **Perfil de Usuario y Sesiones** - Gestión personal, sesiones, GDPR | Citizen |
| `use-case-analytics-notifications.mmd` | **Analíticas y Notificaciones** - Métricas del sistema y notificaciones | Anonymous, Citizen, Operator, Administrator |

### Diagramas de Actividad/Proceso ✅
| Archivo Renombrado | Descripción | Proceso Principal |
|-------------------|-------------|------------------|
| `activity-submit-report-process.mmd` | **Proceso de Envío de Reporte** - Flujo completo con detección de duplicados | Validación → Duplicados → Creación de Tarea |
| `activity-priority-calculation.mmd` | **Cálculo de Prioridad** - Algoritmo con factores múltiples | Categoría + Zona + Tiempo = Prioridad |
| `activity-task-state-update.mmd` | **Actualización de Estado de Tarea** - Máquina de estados con validaciones | Estado → Validación → Evidencia → Auditoría |
| `activity-task-assignment.mmd` | **Asignación de Tarea** - Proceso de asignación a operadores | Validación → Asignación → Notificación |
| `activity-algorithm-weights-update.mmd` | **Actualización de Pesos del Algoritmo** - Configuración con recálculo masivo | Configuración → Recálculo → Aplicación |

---

## 🔄 02. Vista Lógica (Logical View)

### Diagramas de Secuencia
| Archivo Renombrado | Descripción | Componentes Clave |
|-------------------|-------------|------------------|
| `sequence-login-authentication.mmd` | **Autenticación de Login** - Flujo JWT completo | AuthController, JwtTokenProvider, UserSessionService |
| `sequence-user-registration.mmd` | **Registro de Usuario** - Creación de cuenta con validaciones | AuthController, AuthService, UserRepository |
| `sequence-report-submission.mmd` | **Envío de Reporte** - Creación con detección de duplicados | ReportController, DeduplicationService, TaskService |
| `sequence-task-state-update.mmd` | **Actualización de Tarea** - Cambio de estado con auditoría | TaskController, TaskService, AuditService |
| `sequence-priority-calculation.mmd` | **Cálculo de Prioridad** - Algoritmo de priorización | PriorityCalculatorService, ConfigService |
| `sequence-task-assignment.mmd` | **Asignación de Tarea** - Asignación a operador | TaskController, TaskService, NotificationService |
| `sequence-citizen-feedback.mmd` | **Retroalimentación Ciudadana** - Confirmación/rechazo | FeedbackController, FeedbackService, TaskService |
| `sequence-system-configuration.mmd` | **Configuración del Sistema** - Actualización de parámetros | ConfigController, ConfigService, TaskService |
| `sequence-analytics-generation.mmd` | **Generación de Analíticas** - Métricas y reportes | AnalyticsController, AnalyticsService |
| `sequence-session-management.mmd` | **Gestión de Sesiones** - Control multi-dispositivo | SessionController, UserSessionService |
| `sequence-password-recovery.mmd` | **Recuperación de Contraseña** - Reset por email | PasswordResetController, EmailService |
| `sequence-email-notifications.mmd` | **Notificaciones por Email** - Sistema de notificaciones | NotificationService, EmailService |
| `sequence-audit-logging.mmd` | **Registro de Auditoría** - Trazabilidad de cambios | AuditService, AuditLogRepository |
| `sequence-profile-management.mmd` | **Gestión de Perfil** - Actualización de datos personales | UserController, UserService |
| `sequence-account-deletion-gdpr.mmd` | **Eliminación de Cuenta GDPR** - Proceso de eliminación | UserController, UserDataService |

---

## 🗄️ 03. Vista del Modelo de Datos (Data Model View)

| Archivo Renombrado | Descripción | Entidades Principales |
|-------------------|-------------|----------------------|
| `erd-complete-database-schema.mmd` | **Esquema Completo de Base de Datos** - Modelo ER con PostGIS | User, Report, Task, AuditLog, AlgorithmConfig, RefreshToken, UserSession |

---

## 🏗️ 04. Vista MVC (MVC View)

### Diagramas de Arquitectura MVC
| Archivo Renombrado | Descripción | Capa/Componente |
|-------------------|-------------|-----------------|
| `mvc-general-architecture-flow.mmd` | **Flujo General MVC** - Patrón completo React-Spring Boot | View → Controller → Service → Repository → DB |
| `mvc-controller-layer.mmd` | **Capa de Controladores** - REST Controllers con validación | AuthController, ReportController, TaskController |
| `mvc-service-layer.mmd` | **Capa de Servicios** - Lógica de negocio | AuthService, ReportService, TaskService |
| `mvc-repository-layer.mmd` | **Capa de Repositorios** - Acceso a datos JPA/PostGIS | UserRepository, ReportRepository, TaskRepository |
| `mvc-security-layer.mmd` | **Capa de Seguridad** - Filtros JWT y autenticación | JwtAuthenticationFilter, SecurityConfig |
| `mvc-dto-mapping-layer.mmd` | **Capa de DTOs** - Mapeo de objetos de transferencia | Request/Response DTOs, Entity mapping |
| `mvc-configuration-layer.mmd` | **Capa de Configuración** - Configuración Spring Boot | DatabaseConfig, SecurityConfig, JwtConfig |
| `mvc-exception-handling.mmd` | **Manejo de Excepciones** - Gestión global de errores | GlobalExceptionHandler, Custom Exceptions |
| `mvc-data-validation.mmd` | **Validación de Datos** - Validadores personalizados | EmailValidator, PasswordValidator |
| `mvc-event-system.mmd` | **Sistema de Eventos** - Eventos asíncronos | ApplicationEventPublisher, EventListeners |
| `mvc-frontend-backend-integration.mmd` | **Integración Frontend-Backend** - Comunicación React-Spring | Axios, API Services, Error Handling |

---

## ⚙️ 05. Vista de Procesos (Process View)

### Diagramas de Procesos de Negocio
| Archivo Renombrado | Descripción | Proceso Clave |
|-------------------|-------------|---------------|
| `process-main-report-management.mmd` | **Proceso Principal de Gestión** - Flujo completo de reportes | Reporte → Tarea → Asignación → Resolución |
| `process-duplicate-detection.mmd` | **Detección de Duplicados** - Algoritmo espacial y temporal | Consultas PostGIS, Validación distancia/tiempo |
| `process-priority-calculation.mmd` | **Cálculo de Priorización** - Algoritmo dinámico multi-factor | Categoría + Zona + Tiempo = Prioridad |
| `process-email-notifications.mmd` | **Notificaciones por Email** - Sistema con reintentos | Eventos → Templates → SMTP → Manejo fallos |
| `process-audit-trail.mmd` | **Rastro de Auditoría** - Trazabilidad completa | Captura → Registro → Consulta histórica |
| `process-session-management.mmd` | **Gestión de Sesiones** - Control multi-dispositivo | Login → Sesión → Renovación → Revocación |
| `process-dynamic-configuration.mmd` | **Configuración Dinámica** - Actualización de parámetros | Cambio → Recálculo → Aplicación |
| `process-gdpr-compliance.mmd` | **Cumplimiento GDPR** - Gestión de datos personales | Solicitud → Validación → Anonimización |

---

## 🚀 06. Vista de Despliegue (Deployment View)

### Diagramas de Infraestructura
| Archivo Renombrado | Descripción | Componentes de Infraestructura |
|-------------------|-------------|-------------------------------|
| `deployment-aws-complete-architecture.mmd` | **Arquitectura AWS Completa** - Infraestructura en la nube | ECS, RDS, S3, CloudFront, Route 53 |
| `deployment-docker-containers.mmd` | **Contenedores Docker** - Despliegue con Docker Compose | Backend (Spring Boot), Frontend (Nginx), PostgreSQL |
| `deployment-network-security.mmd` | **Red y Seguridad** - Configuración de red AWS | VPC, Security Groups, Load Balancer |
| `deployment-database-configuration.mmd` | **Configuración de Base de Datos** - RDS PostgreSQL con PostGIS | Multi-AZ, Backups, Read Replicas |

---

## 💻 07. Vista de Implementación (Implementation View)

### Diagramas de Estructura de Código
| Archivo Renombrado | Descripción | Estructura |
|-------------------|-------------|------------|
| `implementation-backend-package-structure.mmd` | **Estructura de Paquetes Backend** - Organización Java/Spring Boot | com.urbanclean.{controller,service,repository,entity} |
| `implementation-frontend-component-structure.mmd` | **Estructura de Componentes Frontend** - Organización React | src/{components,pages,services,hooks,context} |

---

## 📊 Resumen de Renombrado

### Convenciones de Nomenclatura
- **Prefijo por tipo**: `use-case-`, `activity-`, `sequence-`, `erd-`, `mvc-`, `process-`, `deployment-`, `implementation-`
- **Descripción clara**: Nombre descriptivo del contenido del diagrama
- **Sin números**: Eliminados los números secuenciales por nombres semánticos
- **Separadores**: Guiones medios para legibilidad
- **Longitud**: Nombres concisos pero descriptivos

### Beneficios para TFM
1. **Identificación rápida**: Nombres autoexplicativos
2. **Organización temática**: Agrupación por prefijos
3. **Referencia fácil**: Sin necesidad de consultar índices
4. **Integración directa**: Listos para incluir en documentación académica
5. **Mantenimiento**: Estructura escalable y mantenible

---

## 🔧 Comandos de Conversión

### Conversión Masiva a PNG
```bash
# Convertir todos los diagramas a PNG
for file in diagrams/*.mmd; do
    basename=$(basename "$file" .mmd)
    mmdc -i "$file" -o "diagrams/${basename}.png"
done
```

### Conversión por Categoría
```bash
# Solo casos de uso
mmdc -i diagrams/use-case-*.mmd -o diagrams/

# Solo diagramas de secuencia  
mmdc -i diagrams/sequence-*.mmd -o diagrams/

# Solo procesos
mmdc -i diagrams/process-*.mmd -o diagrams/
```

---

*Índice actualizado automáticamente el $(date '+%d de %B de %Y')*
