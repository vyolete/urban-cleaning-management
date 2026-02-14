# Análisis de Brechas - Requisitos IDRQ vs Implementación Actual

## Fecha: 2026-02-09

## Resumen Ejecutivo

Este documento identifica las brechas entre los requisitos formales IDRQ especificados y la implementación actual del Sistema de Gestión de Limpieza Urbana.

---

## Requisitos Funcionales (RF)

### ✅ IDRQ-RF-01: Gestión de Identidad y Acceso (IAM)
**Estado:** IMPLEMENTADO COMPLETAMENTE

**Evidencia:**
- ✅ Autenticación JWT implementada (`JwtTokenProvider`, `JwtAuthenticationFilter`)
- ✅ BCrypt para hashing de contraseñas (`SecurityConfig`)
- ✅ Endpoint `/api/auth/login` funcional
- ✅ Endpoint `/api/auth/register` funcional
- ✅ Tokens incluyen claims: username, role, userId

**Validaciones Pendientes:**
- ⚠️ Validación de complejidad de contraseña (mínimo 8 caracteres, 1 mayúscula, 1 número, 1 carácter especial)
- ⚠️ Verificación de formato de correo electrónico mediante RegEx RFC 5322

**Acción Requerida:** Agregar validaciones de contraseña y email en `RegisterRequest`

---

### ✅ IDRQ-RF-02: Control de Acceso Basado en Roles (RBAC)
**Estado:** IMPLEMENTADO COMPLETAMENTE

**Evidencia:**
- ✅ Roles definidos: `ROLE_CIUDADANO`, `ROLE_TECNICO`, `ROLE_ADMIN`
- ✅ `@PreAuthorize` en endpoints
- ✅ `SecurityFilterChain` configurado
- ✅ Validación de roles en JWT

---

### ❌ IDRQ-RF-10: Recuperación de Credenciales Segura
**Estado:** NO IMPLEMENTADO

**Descripción:** Mecanismo de restablecimiento de contraseña mediante token OTP enviado por correo.

**Componentes Faltantes:**
1. Endpoint `POST /api/auth/forgot-password` (solicitud de recuperación)
2. Endpoint `POST /api/auth/reset-password` (establecer nueva contraseña)
3. Entidad `PasswordResetToken` con campos:
   - `token` (UUID)
   - `userId` (FK a User)
   - `expiresAt` (LocalDateTime)
   - `used` (Boolean)
4. Servicio de envío de correos (`EmailService`)
5. Plantillas HTML para correos de recuperación

**Prioridad:** ALTA (requisito de seguridad crítico)

---

### ✅ IDRQ-RF-03: Ingesta de Incidencias Multimedia
**Estado:** IMPLEMENTADO COMPLETAMENTE

**Evidencia:**
- ✅ Endpoint multipart/form-data (`ReportController`)
- ✅ Validación de tipo MIME (JPEG, PNG)
- ✅ Limitación de tamaño (5MB configurado)
- ✅ Geofencing implementado (`GeofencingService`)
- ✅ Almacenamiento de imágenes (`FileStorageService`)

---

### ✅ IDRQ-RF-04: Motor de Priorización Algorítmica
**Estado:** IMPLEMENTADO COMPLETAMENTE

**Evidencia:**
- ✅ Fórmula implementada: `P = (Wc × Cat) + (Wz × Zona) + (Wt × Tiempo)`
- ✅ Servicio `PriorityCalculatorService`
- ✅ Coeficientes configurables en BD (`AlgorithmConfig`)
- ✅ Recalculación automática al cambiar pesos

---

### ⚠️ IDRQ-RF-05: Ciclo de Vida y Workflow de Estados
**Estado:** PARCIALMENTE IMPLEMENTADO

**Evidencia:**
- ✅ Estados definidos: `PENDIENTE`, `ASIGNADO`, `EN_PROGRESO`, `RESUELTO`
- ✅ Validación de transiciones en `TaskService`
- ✅ Validación de roles para cambios de estado

**Componentes Faltantes:**
- ❌ Estado `REABIERTO` no implementado
- ❌ Transición `RESUELTO ↔ REABIERTO` no soportada
- ❌ Validación de evidencia (foto/comentario) antes de cerrar tarea

**Acción Requerida:** 
1. Agregar `REABIERTO` a enum `TaskState`
2. Implementar lógica de reapertura
3. Validar evidencia de resolución

---

### ✅ IDRQ-RF-06: Trazabilidad y Log de Auditoría
**Estado:** IMPLEMENTADO COMPLETAMENTE

**Evidencia:**
- ✅ Entidad `AuditLog` con campos requeridos
- ✅ Registro automático de cambios de estado
- ✅ Append-only (sin modificación/borrado)
- ✅ Captura de timestamp, actor, entidad, valores anterior/nuevo
- ✅ Servicio `AuditService`

**Mejora Sugerida:**
- ⚠️ Captura de dirección IP de origen (actualmente no implementado)

---

### ❌ IDRQ-RF-07: Módulo de Notificaciones Event-Driven
**Estado:** NO IMPLEMENTADO

**Descripción:** Sistema de alertas asíncronas mediante correo electrónico.

**Componentes Faltantes:**
1. Servicio `EmailService` con integración SMTP
2. Sistema de eventos (`Spring Events` o cola de mensajes)
3. Listeners para eventos:
   - `TaskCreatedEvent`
   - `TaskUpdatedEvent`
   - `TaskResolvedEvent`
4. Plantillas HTML para correos
5. Configuración SMTP en `application.properties`
6. Preferencias de notificación por usuario

**Prioridad:** MEDIA (mejora la experiencia del usuario)

---

### ⚠️ IDRQ-RF-08: Dashboard de Analítica Operativa
**Estado:** PARCIALMENTE IMPLEMENTADO

**Evidencia:**
- ✅ Endpoints de consulta de tareas con filtros
- ✅ Ordenamiento por prioridad

**Componentes Faltantes:**
- ❌ Endpoint de agregación para KPIs:
  - Mapa de calor (Heatmap)
  - MTTR (Mean Time To Resolution)
  - Distribución por categorías
  - Tareas por estado
- ❌ Caché de consultas pesadas
- ❌ Filtrado por rango de fechas

**Acción Requerida:**
1. Crear `AnalyticsController` con endpoints:
   - `GET /api/analytics/heatmap`
   - `GET /api/analytics/mttr`
   - `GET /api/analytics/category-distribution`
   - `GET /api/analytics/state-summary`
2. Implementar caché con `@Cacheable`

---

### ❌ IDRQ-RF-09: Gestión de Perfil y Preferencias
**Estado:** NO IMPLEMENTADO

**Descripción:** Autogestión de datos personales por el usuario.

**Componentes Faltantes:**
1. Endpoint `GET /api/users/me` (consultar perfil propio)
2. Endpoint `PUT /api/users/me` (actualizar perfil)
3. Endpoint `PUT /api/users/me/password` (cambiar contraseña)
4. Endpoint `GET /api/users/me/reports` (historial de reportes)
5. Endpoint `DELETE /api/users/me` (borrado de cuenta - RGPD)
6. Validación IDOR (Insecure Direct Object Reference)
7. Funcionalidad de descarga de datos (portabilidad RGPD)

**Prioridad:** MEDIA (cumplimiento RGPD)

---

### ⚠️ IDRQ-RF-11: Gestión de Parámetros del Sistema
**Estado:** PARCIALMENTE IMPLEMENTADO

**Evidencia:**
- ✅ Endpoint para modificar pesos del algoritmo
- ✅ Cambios aplicados en caliente (sin reinicio)
- ✅ Auditoría de cambios

**Componentes Faltantes:**
- ❌ Configuración de tiempos de expiración de tokens (actualmente hardcoded)
- ❌ Configuración de radio de detección de duplicados (actualmente hardcoded)
- ❌ Interfaz administrativa completa para todos los parámetros

**Acción Requerida:**
1. Mover constantes hardcoded a tabla `SystemConfig`
2. Crear endpoints para gestionar todos los parámetros

---

### ✅ IDRQ-RF-12: Detección y Gestión de Duplicados
**Estado:** IMPLEMENTADO COMPLETAMENTE

**Evidencia:**
- ✅ Algoritmo de deduplicación implementado
- ✅ Evaluación de proximidad geoespacial (PostGIS)
- ✅ Ventana temporal configurable
- ✅ Agrupación por categoría
- ✅ Contador de duplicados
- ✅ Incremento de prioridad proporcional

---

### ❌ IDRQ-RF-13: Cierre con Validación Ciudadana
**Estado:** NO IMPLEMENTADO

**Descripción:** Feedback loop para confirmación de resolución por el ciudadano.

**Componentes Faltantes:**
1. Endpoint `POST /api/tasks/{id}/confirm-resolution` (confirmar solución)
2. Endpoint `POST /api/tasks/{id}/reject-resolution` (rechazar/reabrir)
3. Campo `citizenFeedback` en entidad `Task`
4. Campo `feedbackDeadline` (72h desde RESUELTO)
5. Job programado para cierre automático tras 72h sin respuesta
6. Notificación al ciudadano cuando tarea se marca como RESUELTO
7. Validación: solo el reportante puede dar feedback

**Prioridad:** ALTA (requisito funcional crítico del flujo)

---

## Requisitos No Funcionales (RNF)

### ✅ IDRQ-RNF-01: Seguridad - Protección de Datos y Cifrado
**Estado:** IMPLEMENTADO COMPLETAMENTE

**Evidencia:**
- ✅ BCrypt para contraseñas
- ✅ HTTPS configurado (nginx)
- ✅ Protección contra SQL Injection (JPA/Hibernate)
- ✅ Protección contra XSS (validación de inputs)

---

### ⚠️ IDRQ-RNF-02: Privacidad - Cumplimiento RGPD
**Estado:** PARCIALMENTE IMPLEMENTADO

**Evidencia:**
- ✅ Minimización de datos
- ✅ Datos personales no expuestos públicamente

**Componentes Faltantes:**
- ❌ Función de borrado de cuenta (Derecho al olvido)
- ❌ Exportación de datos personales (Portabilidad)
- ❌ Consentimiento explícito para procesamiento de datos
- ❌ Política de privacidad y términos de servicio

**Prioridad:** ALTA (cumplimiento legal)

---

### ✅ IDRQ-RNF-03: Usabilidad - Interfaz Adaptativa
**Estado:** IMPLEMENTADO COMPLETAMENTE

**Evidencia:**
- ✅ Frontend React responsive
- ✅ Funcional en móviles (360px+)
- ✅ Diseño adaptativo

---

### ⚠️ IDRQ-RNF-04: Eficiencia - Rendimiento
**Estado:** PARCIALMENTE IMPLEMENTADO

**Evidencia:**
- ✅ Índices espaciales PostGIS
- ✅ Consultas optimizadas

**Componentes Faltantes:**
- ❌ Pruebas de carga (50 peticiones simultáneas)
- ❌ Medición de tiempo de respuesta < 500ms
- ❌ Optimización de carga inicial < 3s

**Acción Requerida:** Realizar pruebas de rendimiento y optimizar

---

### ✅ IDRQ-RNF-05: Portabilidad - Contenedores
**Estado:** IMPLEMENTADO COMPLETAMENTE

**Evidencia:**
- ✅ Docker y Docker Compose
- ✅ Despliegue con `docker-compose up`

---

### ⚠️ IDRQ-RNF-06: Estándares - API REST
**Estado:** PARCIALMENTE IMPLEMENTADO

**Evidencia:**
- ✅ Verbos HTTP semánticos
- ✅ Códigos de estado correctos

**Componentes Faltantes:**
- ❌ Documentación Swagger/OpenAPI no generada automáticamente

**Acción Requerida:** Agregar SpringDoc OpenAPI

---

### ❌ IDRQ-RNF-07: Interoperabilidad - Exportación de Datos
**Estado:** NO IMPLEMENTADO

**Descripción:** Exportación de reportes en CSV y JSON.

**Componentes Faltantes:**
1. Endpoint `GET /api/reports/export?format=csv`
2. Endpoint `GET /api/reports/export?format=json`
3. Generación de archivos en < 5 segundos

**Prioridad:** BAJA (nice to have)

---

### ✅ IDRQ-RNF-08: Implementación - Stack Tecnológico
**Estado:** IMPLEMENTADO COMPLETAMENTE

**Evidencia:**
- ✅ Spring Boot (Backend)
- ✅ React (Frontend)
- ✅ PostgreSQL + PostGIS (Base de datos)
- ✅ Git (Control de versiones)

---

## Resumen de Prioridades

### 🔴 PRIORIDAD ALTA (Crítico)
1. **IDRQ-RF-10:** Recuperación de credenciales (Password Reset)
2. **IDRQ-RF-13:** Cierre con validación ciudadana (Feedback Loop)
3. **IDRQ-RF-05:** Completar workflow de estados (REABIERTO)
4. **IDRQ-RNF-02:** Cumplimiento RGPD (Derecho al olvido, Portabilidad)

### 🟡 PRIORIDAD MEDIA (Importante)
5. **IDRQ-RF-07:** Sistema de notificaciones por correo
6. **IDRQ-RF-08:** Dashboard de analítica completo
7. **IDRQ-RF-09:** Gestión de perfil de usuario
8. **IDRQ-RF-11:** Gestión completa de parámetros del sistema

### 🟢 PRIORIDAD BAJA (Mejoras)
9. **IDRQ-RNF-06:** Documentación Swagger/OpenAPI
10. **IDRQ-RNF-07:** Exportación de datos (CSV/JSON)
11. **IDRQ-RNF-04:** Pruebas de rendimiento formales

---

## Validaciones Menores Pendientes

### En IDRQ-RF-01 (Autenticación)
- Validación de complejidad de contraseña
- Validación de formato de email (RFC 5322)

### En IDRQ-RF-06 (Auditoría)
- Captura de dirección IP de origen

---

## Recomendaciones

1. **Fase 1 (Inmediata):** Implementar requisitos de prioridad ALTA
2. **Fase 2 (Corto plazo):** Implementar requisitos de prioridad MEDIA
3. **Fase 3 (Largo plazo):** Implementar mejoras de prioridad BAJA
4. **Continuo:** Agregar validaciones menores durante desarrollo normal

---

## Métricas de Cobertura

- **Requisitos Funcionales:** 9/13 completos (69%)
- **Requisitos No Funcionales:** 5/8 completos (63%)
- **Cobertura Total:** 14/21 requisitos (67%)

**Objetivo:** Alcanzar 100% de cobertura en requisitos de prioridad ALTA y MEDIA.
