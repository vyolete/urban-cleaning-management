# Análisis de Cobertura de Requisitos - Urban Clean Management System

**Fecha**: 9 de febrero de 2026  
**Propósito**: Mapear requisitos funcionales y no funcionales contra specs implementados y pendientes

---

## 📋 Resumen Ejecutivo

Este documento analiza la cobertura de los requisitos IDRQ contra los specs del proyecto para identificar gaps y priorizar el trabajo pendiente.

---

## 1. REQUISITOS FUNCIONALES (RF)

### 1.1. Módulo de Seguridad y Acceso

#### ✅ IDRQ-RF-01: Gestión de Identidad y Acceso (IAM)
**Estado**: **COMPLETADO**

**Cobertura**:
- ✅ Autenticación JWT con Spring Security
- ✅ Hashing BCrypt para contraseñas
- ✅ Validación de complejidad de contraseña (8+ chars, mayúscula, número, especial)
- ✅ Validación RFC 5322 para emails
- ✅ Endpoint `/api/auth/login` con respuesta 200/401

**Implementado en**:
- Spec: `critical-security-feedback` (Fase 1)
- Archivos:
  - `backend/src/main/java/com/urbanclean/validation/PasswordValidator.java`
  - `backend/src/main/java/com/urbanclean/validation/EmailValidator.java`
  - `backend/src/main/java/com/urbanclean/security/JwtTokenProvider.java`
  - `backend/src/main/java/com/urbanclean/controller/AuthController.java`

---

#### ✅ IDRQ-RF-02: Control de Acceso Basado en Roles (RBAC)
**Estado**: **COMPLETADO**

**Cobertura**:
- ✅ Roles: ROLE_CIUDADANO, ROLE_TECNICO, ROLE_ADMIN
- ✅ SecurityFilterChain con validación de roles
- ✅ Claims de roles en JWT
- ✅ HTTP 403 para accesos no autorizados

**Implementado en**:
- Spec: `urban-cleaning-management` (Core)
- Archivos:
  - `backend/src/main/java/com/urbanclean/config/SecurityConfig.java`
  - `backend/src/main/java/com/urbanclean/security/JwtAuthenticationFilter.java`

---

#### ✅ IDRQ-RF-10: Recuperación de Credenciales Segura
**Estado**: **COMPLETADO**

**Cobertura**:
- ✅ Tokens OTP de un solo uso
- ✅ Expiración de 15 minutos
- ✅ Envío asíncrono de email
- ✅ Invalidación tras uso
- ✅ No confirmación de existencia de email (prevención de enumeración)

**Implementado en**:
- Spec: `critical-security-feedback` (Fase 2)
- Archivos:
  - `backend/src/main/java/com/urbanclean/service/PasswordResetService.java`
  - `backend/src/main/java/com/urbanclean/controller/PasswordResetController.java`
  - `backend/src/main/java/com/urbanclean/entity/PasswordResetToken.java`

---

### 1.2. Módulo Core: Gestión de Incidencias

#### ✅ IDRQ-RF-03: Ingesta de Incidencias Multimedia
**Estado**: **COMPLETADO**

**Cobertura**:
- ✅ Endpoint REST multipart
- ✅ Validación MIME (JPEG, PNG)
- ✅ Tamaño máximo 5 MB
- ✅ Validación geoespacial (polígono municipal)
- ✅ HTTP 201 con ID generado

**Implementado en**:
- Spec: `urban-cleaning-management` (Core)
- Archivos:
  - `backend/src/main/java/com/urbanclean/controller/ReportController.java`
  - `backend/src/main/java/com/urbanclean/service/ReportService.java`

---

#### ✅ IDRQ-RF-04: Motor de Priorización Algorítmica
**Estado**: **COMPLETADO**

**Cobertura**:
- ✅ Suma ponderada normalizada (Wc, Wz, Wt)
- ✅ Configurable en tiempo de ejecución
- ✅ Detección de duplicados espaciales
- ✅ Actualización automática de urgencia

**Implementado en**:
- Spec: `urban-cleaning-management` (Core)
- Archivos:
  - `backend/src/main/java/com/urbanclean/service/PriorityCalculatorService.java`
  - `backend/src/main/java/com/urbanclean/service/ConfigService.java`

---

#### ✅ IDRQ-RF-05: Ciclo de Vida y Workflow de Estados
**Estado**: **COMPLETADO**

**Cobertura**:
- ✅ Flujo: PENDIENTE → ASIGNADO → EN_PROGRESO → RESUELTO ↔ REABIERTO
- ✅ Validación de transiciones lógicas
- ✅ Validación de rol
- ✅ HTTP 400 para transiciones inválidas
- ✅ Auditoría de cada cambio

**Implementado en**:
- Spec: `critical-security-feedback` (Fase 3)
- Archivos:
  - `backend/src/main/java/com/urbanclean/entity/Task.java`
  - `backend/src/main/java/com/urbanclean/service/TaskService.java`
  - `backend/src/main/java/com/urbanclean/service/FeedbackService.java`

---

### 1.3. Módulo Operativo y de Soporte

#### ✅ IDRQ-RF-06: Trazabilidad y Log de Auditoría
**Estado**: **COMPLETADO**

**Cobertura**:
- ✅ Entity Listeners (JPA)
- ✅ Registro append-only
- ✅ Captura de IP de origen
- ✅ Timestamp, actor_id, entidad_afectada, valor_anterior, valor_nuevo
- ✅ Consulta por Administrador

**Implementado en**:
- Spec: `critical-security-feedback` (Fase 5)
- Archivos:
  - `backend/src/main/java/com/urbanclean/entity/AuditLog.java`
  - `backend/src/main/java/com/urbanclean/service/AuditService.java`

---

#### ✅ IDRQ-RF-07: Módulo de Notificaciones Event-Driven
**Estado**: **COMPLETADO**

**Cobertura**:
- ✅ Spring Events para notificaciones
- ✅ Envío asíncrono de emails
- ✅ Plantillas HTML
- ✅ Manejo de excepciones sin bloqueo
- ✅ Validación de preferencias de usuario
- ✅ Email cuando reporte cambia a RESUELTO

**Implementado en**:
- Spec: `critical-security-feedback` (Fase 3)
- Archivos:
  - `backend/src/main/java/com/urbanclean/event/TaskResolvedEvent.java`
  - `backend/src/main/java/com/urbanclean/event/TaskReopenedEvent.java`
  - `backend/src/main/java/com/urbanclean/event/TaskEventListener.java`
  - `backend/src/main/java/com/urbanclean/service/EmailService.java`

**Gaps**:
- ⚠️ Notificación de asignación de tarea a operador (TASK_ASSIGNED)
- ⚠️ Gestión de preferencias de notificaciones por usuario
- ⚠️ Tabla notification_failures para reintentos fallidos
- ⚠️ Enlaces de unsubscribe en emails

**Spec pendiente**: `notifications-analytics` (Requirement 1, 2)

---

#### ⚠️ IDRQ-RF-08: Dashboard de Analítica Operativa
**Estado**: **PARCIALMENTE COMPLETADO**

**Cobertura actual**:
- ✅ Endpoint de configuración de algoritmo
- ❌ Endpoints de agregación (GROUP BY, COUNT, AVG)
- ❌ Mapa de calor (Heatmap)
- ❌ MTTR (Mean Time To Resolution)
- ❌ Distribución por categorías
- ❌ Caché de consultas pesadas
- ❌ Filtrado por fechas y zonas

**Implementado en**:
- Spec: `urban-cleaning-management` (parcial)

**Gaps**:
- ❌ Dashboard completo de analytics
- ❌ Visualización de KPIs
- ❌ Heatmap geográfico

**Spec pendiente**: `notifications-analytics` (Requirements 3, 4, 5)

---

#### ✅ IDRQ-RF-09: Gestión de Perfil y Preferencias
**Estado**: **COMPLETADO**

**Cobertura**:
- ✅ CRUD de usuario con validación de user_id
- ✅ Protección IDOR
- ✅ Actualización de foto de perfil
- ✅ Cambio de contraseña
- ✅ Consulta de historial de reportes
- ✅ Descarga de historial (Portabilidad GDPR)

**Implementado en**:
- Spec: `critical-security-feedback` (Fase 4)
- Archivos:
  - `backend/src/main/java/com/urbanclean/controller/UserController.java`
  - `backend/src/main/java/com/urbanclean/service/UserDataService.java`

---

#### ✅ IDRQ-RF-11: Gestión de Parámetros del Sistema
**Estado**: **COMPLETADO**

**Cobertura**:
- ✅ Interfaz administrativa para modificar parámetros
- ✅ Coeficientes del algoritmo (Wc, Wz, Wt)
- ✅ Aplicación en caliente (Runtime)
- ✅ Validación de rangos seguros
- ✅ Auditoría de cambios

**Implementado en**:
- Spec: `urban-cleaning-management` (Core)
- Archivos:
  - `backend/src/main/java/com/urbanclean/controller/ConfigController.java`
  - `backend/src/main/java/com/urbanclean/service/ConfigService.java`

**Gaps**:
- ⚠️ Configuración de tiempos de expiración de tokens
- ⚠️ Configuración de radio de detección de duplicados

**Spec pendiente**: `enhanced-session-management` (Requirement 9)

---

#### ✅ IDRQ-RF-12: Detección y Gestión de Duplicados
**Estado**: **COMPLETADO**

**Cobertura**:
- ✅ Proximidad geoespacial (radio configurable)
- ✅ Ventana temporal
- ✅ Misma categoría
- ✅ Agrupación en Tarea Padre
- ✅ Verificación espacial con PostGIS
- ✅ Contador de ciudadanos afectados
- ✅ Aumento de prioridad proporcional

**Implementado en**:
- Spec: `urban-cleaning-management` (Core)
- Archivos:
  - `backend/src/main/java/com/urbanclean/service/DeduplicationService.java`

---

#### ✅ IDRQ-RF-13: Cierre con Validación Ciudadana
**Estado**: **COMPLETADO**

**Cobertura**:
- ✅ Notificación tras RESUELTO
- ✅ Acciones: Confirmar/Rechazar
- ✅ Cierre automático tras 72h (Silencio Administrativo)
- ✅ Validación de que solo el reportero puede reabrir
- ✅ Justificación obligatoria para reapertura

**Implementado en**:
- Spec: `critical-security-feedback` (Fase 3)
- Archivos:
  - `backend/src/main/java/com/urbanclean/service/FeedbackService.java`
  - `backend/src/main/java/com/urbanclean/controller/FeedbackController.java`

---

## 2. REQUISITOS NO FUNCIONALES (RNF)

### ✅ IDRQ-5 – RNF-01: Seguridad
**Estado**: **COMPLETADO**

**Cobertura**:
- ✅ BCrypt para hashing de contraseñas
- ✅ HTTPS con TLS 1.2+ (configuración Docker)
- ✅ JWT con firma digital
- ✅ Validación de tokens en cada petición

**Implementado en**:
- Specs: `urban-cleaning-management`, `critical-security-feedback`

**Mejoras pendientes**:
- ⚠️ Escaneos automatizados de vulnerabilidades
- ⚠️ Refresh tokens con rotación

**Spec pendiente**: `enhanced-session-management`

---

### ✅ IDRQ-6 – RNF-02: Privacidad
**Estado**: **COMPLETADO**

**Cobertura**:
- ✅ Minimización de datos
- ✅ Derecho al olvido (anonymización)
- ✅ Derecho de acceso (GET /api/users/profile)
- ✅ Derecho de rectificación (PUT /api/users/profile)
- ✅ Derecho a la portabilidad (GET /api/users/export)

**Implementado en**:
- Spec: `critical-security-feedback` (Fase 4)

---

### ⚠️ IDRQ-7 – RNF-03: Usabilidad
**Estado**: **PARCIALMENTE COMPLETADO**

**Cobertura actual**:
- ✅ Diseño responsive (frontend React)
- ❌ Pruebas formales con usuarios
- ❌ Validación de ≥95% éxito en reportes

**Implementado en**:
- Spec: `urban-cleaning-management` (Frontend)

**Gaps**:
- ❌ Testing de usabilidad formal
- ❌ Métricas de éxito de usuario

---

### ⚠️ IDRQ-8 – RNF-04: Rendimiento
**Estado**: **PARCIALMENTE COMPLETADO**

**Cobertura actual**:
- ✅ Backend con Spring Boot optimizado
- ❌ Tiempo de carga < 3 segundos (no validado)
- ❌ Respuesta backend < 500 ms (no validado)
- ❌ Pruebas de estrés formales

**Gaps**:
- ❌ Performance testing automatizado
- ❌ Load testing con 50 usuarios concurrentes
- ❌ Métricas de rendimiento documentadas

**Spec pendiente**: `documentation-export` (Requirement 6)

---

### ✅ IDRQ-9 – RNF-05: Portabilidad
**Estado**: **COMPLETADO**

**Cobertura**:
- ✅ Docker y Docker Compose
- ✅ Despliegue con un solo comando
- ✅ Independencia del entorno

**Implementado en**:
- Infraestructura: `docker/docker-compose.yml`

---

### ✅ IDRQ-12 – RNF-08: Implementación
**Estado**: **COMPLETADO**

**Cobertura**:
- ✅ Stack: Spring Boot, React, PostgreSQL
- ✅ Todo el código versionado en Git
- ✅ Revisión de dependencias (pom.xml, package.json)

**Implementado en**:
- Todo el proyecto

---

## 3. MATRIZ DE COBERTURA

| ID Requisito | Nombre | Estado | Spec Implementado | Spec Pendiente |
|--------------|--------|--------|-------------------|----------------|
| **RF-01** | IAM | ✅ Completo | critical-security-feedback | - |
| **RF-02** | RBAC | ✅ Completo | urban-cleaning-management | - |
| **RF-10** | Recuperación Credenciales | ✅ Completo | critical-security-feedback | - |
| **RF-03** | Ingesta Incidencias | ✅ Completo | urban-cleaning-management | - |
| **RF-04** | Motor Priorización | ✅ Completo | urban-cleaning-management | - |
| **RF-05** | Workflow Estados | ✅ Completo | critical-security-feedback | - |
| **RF-06** | Auditoría | ✅ Completo | critical-security-feedback | - |
| **RF-07** | Notificaciones | ⚠️ Parcial | critical-security-feedback | notifications-analytics |
| **RF-08** | Dashboard Analytics | ⚠️ Parcial | - | notifications-analytics |
| **RF-09** | Gestión Perfil | ✅ Completo | critical-security-feedback | - |
| **RF-11** | Configuración Sistema | ⚠️ Parcial | urban-cleaning-management | enhanced-session-management |
| **RF-12** | Detección Duplicados | ✅ Completo | urban-cleaning-management | - |
| **RF-13** | Validación Ciudadana | ✅ Completo | critical-security-feedback | - |
| **RNF-01** | Seguridad | ⚠️ Parcial | critical-security-feedback | enhanced-session-management |
| **RNF-02** | Privacidad | ✅ Completo | critical-security-feedback | - |
| **RNF-03** | Usabilidad | ⚠️ Parcial | urban-cleaning-management | - |
| **RNF-04** | Rendimiento | ⚠️ Parcial | - | documentation-export |
| **RNF-05** | Portabilidad | ✅ Completo | Infraestructura | - |
| **RNF-08** | Implementación | ✅ Completo | Todo el proyecto | - |

---

## 4. GAPS IDENTIFICADOS

### 4.1. Gaps Críticos (Alta Prioridad)

1. **Dashboard de Analytics Completo** (RF-08)
   - Endpoints de agregación
   - Mapa de calor (Heatmap)
   - MTTR
   - Distribución por categorías
   - **Spec**: `notifications-analytics`

2. **Sistema de Notificaciones Completo** (RF-07)
   - Notificación TASK_ASSIGNED
   - Gestión de preferencias por usuario
   - Tabla notification_failures
   - Enlaces de unsubscribe
   - **Spec**: `notifications-analytics`

3. **Gestión Avanzada de Sesiones** (RNF-01)
   - Refresh tokens con rotación
   - Multi-device session management
   - Token revocation y blacklist
   - **Spec**: `enhanced-session-management`

### 4.2. Gaps Importantes (Media Prioridad)

4. **Documentación API** (RNF-04, Developer Experience)
   - OpenAPI/Swagger automático
   - Ejemplos de código
   - Documentación de errores
   - **Spec**: `documentation-export`

5. **Exportación de Datos** (RF-08, Analytics)
   - CSV export
   - JSON export
   - Bulk export para analytics
   - **Spec**: `documentation-export`

6. **Performance Testing** (RNF-04)
   - Load testing automatizado
   - Métricas de rendimiento
   - Validación de SLAs
   - **Spec**: `documentation-export`

### 4.3. Gaps Menores (Baja Prioridad)

7. **Testing de Usabilidad** (RNF-03)
   - Pruebas con usuarios reales
   - Métricas de éxito
   - Validación de ≥95% éxito

8. **Configuración Dinámica Extendida** (RF-11)
   - Tiempos de expiración de tokens
   - Radio de detección de duplicados
   - **Spec**: `enhanced-session-management`

---

## 5. PRIORIZACIÓN DE SPECS PENDIENTES

### Prioridad 1: `notifications-analytics`
**Justificación**:
- Completa RF-07 (Notificaciones) y RF-08 (Analytics)
- Funcionalidad operativa crítica
- Mejora visibilidad y toma de decisiones
- Impacto directo en usuarios finales

**Requisitos cubiertos**:
- RF-07: Sistema de notificaciones completo
- RF-08: Dashboard de analytics
- Mejora de RNF-03 (Usabilidad)

---

### Prioridad 2: `enhanced-session-management`
**Justificación**:
- Mejora crítica de seguridad (RNF-01)
- Mejora experiencia de usuario
- Gestión multi-dispositivo
- Complementa trabajo de seguridad ya realizado

**Requisitos cubiertos**:
- RNF-01: Seguridad (refresh tokens, revocación)
- RF-11: Configuración de tokens (parcial)
- Mejora de UX con sesiones persistentes

---

### Prioridad 3: `documentation-export`
**Justificación**:
- Mejora developer experience
- Facilita integración con otros sistemas
- Validación de rendimiento (RNF-04)
- Interoperabilidad

**Requisitos cubiertos**:
- RNF-04: Performance testing
- RF-08: Exportación de datos (parcial)
- Developer experience

---

## 6. RECOMENDACIÓN FINAL

### Orden de Implementación Sugerido:

1. **`notifications-analytics`** (2-3 semanas)
   - Completa funcionalidad operativa crítica
   - Mayor impacto en usuarios finales
   - Cierra gaps de RF-07 y RF-08

2. **`enhanced-session-management`** (2-3 semanas)
   - Mejora crítica de seguridad
   - Complementa trabajo ya realizado
   - Mejora UX significativamente

3. **`documentation-export`** (1-2 semanas)
   - Mejora developer experience
   - Facilita mantenimiento
   - Valida rendimiento

### Cobertura Final Esperada:
- **Requisitos Funcionales**: 100% (13/13)
- **Requisitos No Funcionales**: 100% (6/6)
- **Specs Completados**: 4/4
- **Sistema listo para producción**: ✅

---

## 7. CONCLUSIONES

### Estado Actual:
- ✅ **10/13 RF completados** (77%)
- ✅ **4/6 RNF completados** (67%)
- ✅ **1/4 specs completados** (25%)

### Trabajo Pendiente:
- 3 specs por implementar
- ~6-8 semanas de desarrollo
- Enfoque en analytics, sesiones y documentación

### Fortalezas:
- ✅ Core funcional completo
- ✅ Seguridad robusta implementada
- ✅ GDPR compliance completo
- ✅ Infraestructura lista

### Próximo Paso Recomendado:
**Iniciar spec `notifications-analytics`** para completar funcionalidad operativa crítica.

---

**Última actualización**: 9 de febrero de 2026  
**Autor**: Análisis de cobertura de requisitos  
**Versión**: 1.0
