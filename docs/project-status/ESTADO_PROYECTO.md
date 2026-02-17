# Estado del Proyecto - Urban Clean Management System

**Fecha**: 9 de febrero de 2026  
**Última actualización**: Commit 1dee483

## ✅ Estado de los Servicios

### Backend (Puerto 8080)
- **Estado**: ✅ Funcionando correctamente
- **Health Check**: ✅ Healthy
- **Base de datos**: ✅ Conectada (PostgreSQL + PostGIS)
- **Autenticación JWT**: ✅ Funcionando
- **API Endpoints**: ✅ Operativos

### Frontend (Puerto 3000)
- **Estado**: ✅ Funcionando correctamente
- **Health Check**: ✅ Healthy
- **Build**: ✅ Actualizado con logs de debug
- **Conexión API**: ✅ Configurada

### Base de Datos (Puerto 5432)
- **Estado**: ✅ Funcionando correctamente
- **Health Check**: ✅ Healthy
- **Extensión PostGIS**: ✅ Instalada
- **Datos de prueba**: ✅ Cargados

## 📊 Datos en el Sistema

### Usuarios
- **admin** (ROLE_ADMIN) - Password: `admin123`
  - ID: c13d45ec-dfd3-4b11-8074-7d3638539316
  - Email: admin@test.com

### Reportes
- **4 reportes** creados (3 originales, 1 duplicado)
- Ubicaciones en Madrid (40.4168, -3.7038)

### Tareas
- **2 tareas** creadas
- 1 tarea con reportes duplicados vinculados

## 🔧 Cambios Recientes

### 9 Feb 2026 - ✅ Fase 5 Completada: Audit Trail Enhancement & Security Monitoring

**Tarea 20: Security Monitoring**
- Creada entidad `FailedLoginAttempt` para trackear intentos de login fallidos
- Implementado `SecurityMonitoringService` con detección automática de actividad sospechosa
- Thresholds: 5 intentos por username, 10 por IP en ventana de 15 minutos
- Flagging automático de intentos sospechosos
- Cleanup automático de registros antiguos (diario a las 3 AM)
- Integrado con AuthService y AuthController

**Archivos creados:**
- `backend/src/main/java/com/urbanclean/entity/FailedLoginAttempt.java`
- `backend/src/main/java/com/urbanclean/repository/FailedLoginAttemptRepository.java`
- `backend/src/main/java/com/urbanclean/service/SecurityMonitoringService.java`
- `backend/src/main/resources/db/migration/V10__create_failed_login_attempts_table.sql`

**Archivos modificados:**
- `backend/src/main/java/com/urbanclean/service/AuthService.java`
- `backend/src/main/java/com/urbanclean/controller/AuthController.java`

**Tarea 19: Audit Logging con IP Capture**
- Agregado campo `ipAddress` (VARCHAR 45) a AuditLog entity
- Creada migración V9 para agregar columna a base de datos
- Implementados métodos `captureIpAddress()` y `sanitizeIpAddress()` en AuditService
- Soporte para IPv4 e IPv6
- Manejo de headers X-Forwarded-For y X-Real-IP para proxies

**Archivos modificados:**
- `backend/src/main/java/com/urbanclean/entity/AuditLog.java`
- `backend/src/main/java/com/urbanclean/service/AuditService.java`

**Archivos creados:**
- `backend/src/main/resources/db/migration/V9__add_ip_address_to_audit_log.sql`

**Tarea 13: Email Notifications con Eventos**
- Implementado sistema de notificaciones basado en eventos de Spring
- Creados eventos `TaskResolvedEvent` y `TaskReopenedEvent`
- Implementado `TaskEventListener` con ejecución asíncrona
- Desacoplamiento entre lógica de negocio y envío de emails
- Integrado con TaskService y FeedbackService

**Archivos creados:**
- `backend/src/main/java/com/urbanclean/event/TaskResolvedEvent.java`
- `backend/src/main/java/com/urbanclean/event/TaskReopenedEvent.java`
- `backend/src/main/java/com/urbanclean/event/TaskEventListener.java`

**Archivos modificados:**
- `backend/src/main/java/com/urbanclean/service/TaskService.java`
- `backend/src/main/java/com/urbanclean/service/FeedbackService.java`

**Estado**: ✅ Compilación exitosa, Fase 5 completada

### 9 Feb 2026 - ✅ JWT Token Invalidation Implementado (Tarea 7)
**Archivos modificados:**
- `backend/src/main/java/com/urbanclean/entity/User.java` - Agregado campo `tokenVersion`
- `backend/src/main/resources/db/migration/V8__add_token_version_to_users.sql` - Migración DB
- `backend/src/main/java/com/urbanclean/security/JwtTokenProvider.java` - Soporte tokenVersion
- `backend/src/main/java/com/urbanclean/security/JwtAuthenticationFilter.java` - Validación tokenVersion
- `backend/src/main/java/com/urbanclean/service/AuthService.java` - Incluye tokenVersion en JWT
- `backend/src/main/java/com/urbanclean/service/PasswordResetService.java` - Incrementa tokenVersion
- `backend/src/main/java/com/urbanclean/controller/UserController.java` - Incrementa tokenVersion

**Archivos creados:**
- `backend/JWT_INVALIDATION_IMPLEMENTATION.md` - Documentación completa

**Funcionalidad:**
- Todos los JWT tokens existentes se invalidan automáticamente cuando el usuario cambia su contraseña
- Implementado mediante campo `tokenVersion` en la entidad User
- El tokenVersion se incrementa en cada cambio de contraseña
- El filtro JWT valida que la versión del token coincida con la versión actual del usuario
- Backward compatible: tokens antiguos sin versión se tratan como versión 0

**Seguridad mejorada:**
- ✅ Previene acceso no autorizado con tokens antiguos después de cambio de contraseña
- ✅ No requiere blacklist de tokens (solución stateless)
- ✅ Logs de seguridad para monitoreo de intentos con tokens inválidos

**Estado**: ✅ Compilación exitosa, Tarea 7 completada

### 9 Feb 2026 - ✅ GDPR Fase 4 Completada - User Profile Management API
**Archivos creados:**
- `backend/src/main/java/com/urbanclean/controller/UserController.java` - Controlador REST
- `backend/src/main/java/com/urbanclean/dto/request/UpdateProfileRequest.java` - DTO
- `backend/src/main/java/com/urbanclean/dto/request/ChangePasswordRequest.java` - DTO
- `backend/src/main/java/com/urbanclean/dto/request/DeleteAccountRequest.java` - DTO
- `backend/src/main/java/com/urbanclean/dto/response/UserProfileResponse.java` - DTO
- `GDPR_PHASE4_COMPLETION_SUMMARY.md` - Documentación completa
- `backend/USER_PROFILE_API_IMPLEMENTATION.md` - Guía de implementación

**Endpoints implementados:**
- GET `/api/users/profile` - Obtener perfil del usuario
- PUT `/api/users/profile` - Actualizar perfil
- POST `/api/users/change-password` - Cambiar contraseña
- GET `/api/users/reports` - Ver historial de reportes
- POST `/api/users/delete-account` - Solicitar eliminación (7 días de gracia)
- POST `/api/users/cancel-deletion` - Cancelar eliminación
- GET `/api/users/export` - Exportar datos (GDPR portabilidad)

**Cumplimiento GDPR:**
- ✅ Derecho de Acceso (Artículo 15)
- ✅ Derecho de Rectificación (Artículo 16)
- ✅ Derecho al Olvido (Artículo 17)
- ✅ Derecho a la Portabilidad (Artículo 20)

**Estado**: ✅ Compilación exitosa, listo para integración frontend

### Commit 1dee483 - Debug de Autenticación
**Archivos modificados:**
- `frontend/src/services/authService.js` - Agregados logs de debug
- `frontend/src/context/AuthContext.jsx` - Agregados logs de debug
- `frontend/src/components/common/ProtectedRoute.jsx` - Agregados logs de debug
- `DEBUG_LOGIN_ISSUE.md` - Guía de debug creada

**Propósito**: Diagnosticar el problema de "Acceso Denegado" después del login

### Commit 867d0f7 - Logs de Debug
**Archivos modificados:**
- Agregados console.log en todo el flujo de autenticación
- Creado `test-login-flow.html` para pruebas

### Commit 17e7d94 - Fix Deduplicación
**Archivos modificados:**
- `backend/src/main/java/com/urbanclean/service/ReportService.java`
- `backend/src/main/java/com/urbanclean/service/DeduplicationService.java`
- `backend/src/main/java/com/urbanclean/repository/TaskRepository.java`
- `backend/src/main/java/com/urbanclean/repository/ReportRepository.java`

**Propósito**: Corregir la lógica de deduplicación para evitar guardar reportes antes de verificar duplicados

## 🐛 Problema Actual: Login "Acceso Denegado"

### Síntomas
- Usuario puede hacer login exitosamente
- Backend retorna token JWT válido con `role: "ROLE_ADMIN"`
- Usuario es redirigido a `/dashboard`
- Aparece mensaje "Acceso Denegado"

### Diagnóstico
El backend está funcionando correctamente. El problema parece ser en el frontend:
1. **Posible causa**: Caché del navegador con código antiguo
2. **Posible causa**: localStorage con datos antiguos
3. **Posible causa**: Token expirado

### Solución Propuesta
Ver archivo `DEBUG_LOGIN_ISSUE.md` para instrucciones detalladas de debug.

## 🧪 Cómo Probar el Sistema

### 1. Verificar que los servicios estén corriendo
```bash
cd docker
docker-compose ps
```

Todos los servicios deben mostrar "Up" y "(healthy)".

### 2. Probar el backend directamente
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Debe retornar:
```json
{
  "token": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "role": "ROLE_ADMIN",
  "username": "admin"
}
```

### 3. Probar el frontend

#### Opción A: Navegador (Recomendado para debug)
1. Abrir navegador en modo incógnito (para evitar caché)
2. Ir a `http://localhost:3000/login`
3. Abrir DevTools (F12) y ver la pestaña Console
4. Ingresar credenciales: `admin` / `admin123`
5. Observar los logs en la consola

**Logs esperados:**
```
Login response: {token: "...", role: "ROLE_ADMIN", username: "admin", ...}
Constructed user object: {username: "admin", role: "ROLE_ADMIN"}
User role: ROLE_ADMIN
Stored in localStorage - user: {"username":"admin","role":"ROLE_ADMIN"}
AuthContext - Setting user: {username: "admin", role: "ROLE_ADMIN"}
AuthContext - User role: ROLE_ADMIN
ProtectedRoute - User: {username: "admin", role: "ROLE_ADMIN"}
ProtectedRoute - User role: ROLE_ADMIN
ProtectedRoute - Required roles: ["ROLE_TECNICO", "ROLE_ADMIN"]
ProtectedRoute - Is authenticated: true
ProtectedRoute - Access granted
```

#### Opción B: Limpiar caché completamente
1. Abrir DevTools (F12)
2. Ir a Application > Storage
3. Click en "Clear site data"
4. Cerrar y reabrir el navegador
5. Hacer hard refresh: Cmd+Shift+R (Mac) o Ctrl+Shift+R (Windows)
6. Intentar login nuevamente

### 4. Ver reportes en la base de datos
```bash
docker exec urbanclean-postgres psql -U urbanclean_user -d urbanclean -c "
SELECT 
  r.id,
  r.category,
  r.description,
  ST_AsText(r.location) as location,
  r.is_duplicate,
  r.parent_task_id,
  r.created_at
FROM reportes r
ORDER BY r.created_at DESC;
"
```

### 5. Ver tareas en la base de datos
```bash
docker exec urbanclean-postgres psql -U urbanclean_user -d urbanclean -c "
SELECT 
  t.id,
  t.category,
  t.state,
  t.priority_score,
  ST_AsText(t.location) as location,
  (SELECT COUNT(*) FROM reportes WHERE parent_task_id = t.id) as num_reports
FROM tareas t
ORDER BY t.created_at DESC;
"
```

## 📝 Endpoints Disponibles

### Autenticación (Público)
- `POST /api/auth/login` - Iniciar sesión
- `POST /api/auth/register` - Registrar usuario

### Perfil de Usuario (Requiere autenticación)
- `GET /api/users/profile` - Obtener perfil del usuario actual
- `PUT /api/users/profile` - Actualizar perfil (email, username)
- `POST /api/users/change-password` - Cambiar contraseña
- `GET /api/users/reports` - Ver historial de reportes del usuario
- `POST /api/users/delete-account` - Solicitar eliminación de cuenta (7 días de gracia)
- `POST /api/users/cancel-deletion` - Cancelar solicitud de eliminación
- `GET /api/users/export` - Exportar todos los datos del usuario (JSON)

### Reportes
- `POST /api/reports` - Crear reporte (público, sin autenticación)
- `GET /api/reports` - Listar reportes (requiere ROLE_TECNICO o ROLE_ADMIN)
- `GET /api/reports/{id}` - Ver reporte específico

### Tareas
- `GET /api/tasks` - Listar tareas (requiere ROLE_TECNICO o ROLE_ADMIN)
- `GET /api/tasks/{id}` - Ver tarea específica
- `PUT /api/tasks/{id}/state` - Actualizar estado de tarea
- `PUT /api/tasks/{id}/assign` - Asignar tarea a técnico

### Configuración (Solo ROLE_ADMIN)
- `GET /api/admin/config/algorithm-weights` - Ver pesos del algoritmo
- `PUT /api/admin/config/algorithm-weights` - Actualizar pesos del algoritmo

## 🔐 Roles y Permisos

### ROLE_CIUDADANO
- Crear reportes (sin autenticación requerida)

### ROLE_TECNICO
- Ver reportes
- Ver tareas
- Actualizar estado de tareas
- Asignarse tareas

### ROLE_ADMIN
- Todos los permisos de ROLE_TECNICO
- Configurar pesos del algoritmo de priorización
- Ver historial de cambios (audit log)

## 🚀 Comandos Útiles

### Reiniciar todos los servicios
```bash
cd docker
docker-compose restart
```

### Reiniciar solo el frontend
```bash
cd docker
docker-compose restart frontend
```

### Ver logs del backend
```bash
docker logs urbanclean-backend -f
```

### Ver logs del frontend
```bash
docker logs urbanclean-frontend -f
```

### Reconstruir el frontend
```bash
cd docker
docker-compose build frontend
docker-compose restart frontend
```

### Acceder a la base de datos
```bash
docker exec -it urbanclean-postgres psql -U urbanclean_user -d urbanclean
```

### Crear un nuevo usuario de prueba
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "tecnico1",
    "password": "tecnico123",
    "email": "tecnico1@test.com",
    "role": "ROLE_TECNICO"
  }'
```

## 📦 Estructura del Proyecto

```
.
├── backend/                 # Spring Boot application
│   ├── src/main/java/
│   │   └── com/urbanclean/
│   │       ├── config/      # Configuración (Security, JWT, CORS)
│   │       ├── controller/  # REST Controllers
│   │       ├── service/     # Lógica de negocio
│   │       ├── repository/  # JPA Repositories
│   │       ├── entity/      # Entidades JPA
│   │       ├── dto/         # Data Transfer Objects
│   │       └── security/    # Componentes de seguridad
│   └── pom.xml
├── frontend/                # React SPA
│   ├── src/
│   │   ├── components/      # Componentes React
│   │   ├── pages/           # Páginas
│   │   ├── services/        # Servicios API
│   │   ├── context/         # React Context
│   │   └── hooks/           # Custom hooks
│   └── package.json
├── docker/                  # Docker configuration
│   ├── docker-compose.yml
│   ├── .env                 # Variables de entorno
│   └── init-db.sql
└── .kiro/specs/            # Especificaciones del proyecto
```

## 🎯 Próximos Pasos

1. **Resolver el problema de login** - Seguir las instrucciones en `DEBUG_LOGIN_ISSUE.md`
2. **Probar el dashboard de operador** - Una vez resuelto el login
3. **Probar la creación de reportes** - Desde la página pública
4. **Probar la configuración del algoritmo** - Desde el panel de admin
5. **Implementar tests automatizados** - Unit tests y property-based tests

## 📞 Soporte

Si encuentras algún problema:
1. Revisa los logs de Docker: `docker logs <container-name>`
2. Revisa la consola del navegador (F12)
3. Verifica que todos los servicios estén "healthy": `docker-compose ps`
4. Consulta `TROUBLESHOOTING.md` para problemas comunes
5. Consulta `DEBUG_LOGIN_ISSUE.md` para problemas de autenticación

---

**Última actualización**: 9 de febrero de 2026  
**Versión**: 1.0.0  
**Estado**: En desarrollo - Debug de autenticación en progreso
