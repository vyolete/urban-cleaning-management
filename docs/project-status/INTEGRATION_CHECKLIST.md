# Frontend-Backend Integration Checklist

## ✅ Configuración Completada

### 1. Variables de Entorno
- [x] `VITE_API_URL` configurada en `.env` (http://localhost:8080/api)
- [x] Variables de mapa configuradas
- [x] Variables de geofencing configuradas

### 2. Servicios API
- [x] `api.js` - Cliente Axios base configurado
- [x] Interceptor de request para JWT
- [x] Interceptor de response para manejo de errores
- [x] Timeout configurado (10 segundos)

### 3. Servicios de Dominio
- [x] `authService.js` - Login, registro, logout, validación de roles
- [x] `reportService.js` - Envío de reportes con multipart/form-data
- [x] `taskService.js` - Gestión de tareas y estados
- [x] `configService.js` - Configuración de algoritmo (admin)

### 4. Contexto de Autenticación
- [x] `AuthContext` implementado
- [x] Almacenamiento de token en localStorage
- [x] Almacenamiento de usuario en localStorage
- [x] Hook `useAuth` disponible

### 5. Componentes de Protección
- [x] `ProtectedRoute` implementado
- [x] Validación de autenticación
- [x] Validación de roles
- [x] Redirección a login si no autenticado

### 6. Páginas Implementadas
- [x] `LoginPage` - Autenticación de usuarios
- [x] `CitizenReportPage` - Envío de reportes
- [x] `OperatorDashboard` - Gestión de tareas
- [x] `AdminConfigPage` - Configuración del algoritmo

### 7. Routing Configurado
- [x] Rutas públicas: `/login`, `/report`
- [x] Rutas protegidas: `/dashboard` (TECNICO/ADMIN), `/admin/config` (ADMIN)
- [x] Ruta por defecto: redirige a `/report`
- [x] Página 404 implementada

## 🔍 Puntos de Integración Backend

### Endpoints de Autenticación
- `POST /api/auth/login` → `authService.login()`
- `POST /api/auth/register` → `authService.register()`

### Endpoints de Reportes
- `POST /api/reports` → `reportService.submitReport()`
- `GET /api/reports` → `reportService.getAllReports()`
- `GET /api/reports/{id}` → `reportService.getReportById()`
- `GET /api/reports/my` → `reportService.getMyReports()`

### Endpoints de Tareas
- `GET /api/tasks` → `taskService.getTasks()`
- `GET /api/tasks/{id}` → `taskService.getTaskById()`
- `PATCH /api/tasks/{id}/state` → `taskService.updateTaskState()`
- `GET /api/tasks/{id}/audit-history` → `taskService.getAuditHistory()`

### Endpoints de Configuración (Admin)
- `GET /api/admin/config/algorithm-weights` → `configService.getCurrentConfig()`
- `PUT /api/admin/config/algorithm-weights` → `configService.updateWeights()`
- `GET /api/admin/config/algorithm-weights/history` → `configService.getConfigHistory()`

## 🔐 Seguridad

### JWT Token
- [x] Token almacenado en localStorage
- [x] Token enviado en header `Authorization: Bearer {token}`
- [x] Token validado en cada request
- [x] Redirección a login si token expirado (401)

### CORS
- [x] Backend debe permitir origen: `http://localhost:5173` (Vite dev server)
- [x] Backend debe permitir origen: `http://localhost:3000` (producción)

### Validación de Roles
- [x] Frontend valida roles antes de mostrar rutas
- [x] Backend valida roles en cada endpoint protegido
- [x] Doble validación (frontend + backend)

## 📝 Validaciones Frontend

### Reportes
- [x] Validación de coordenadas (rango válido)
- [x] Validación de categoría (requerida)
- [x] Validación de descripción (mínimo 10 caracteres)
- [x] Validación de foto (JPEG/PNG, máx 5MB)

### Configuración de Algoritmo
- [x] Validación de pesos (0-1)
- [x] Validación de suma de pesos (debe ser 1.0)
- [x] Validación de distancia de deduplicación (positiva)
- [x] Validación de ventana temporal (positiva)

## 🧪 Pruebas de Integración Recomendadas

### Flujo de Ciudadano
1. [ ] Acceder a `/report` sin autenticación
2. [ ] Capturar ubicación con geolocalización
3. [ ] Seleccionar categoría
4. [ ] Escribir descripción
5. [ ] Subir foto
6. [ ] Enviar reporte
7. [ ] Verificar respuesta exitosa (201)

### Flujo de Operador
1. [ ] Login con usuario TECNICO
2. [ ] Acceder a `/dashboard`
3. [ ] Ver lista de tareas ordenadas por prioridad
4. [ ] Filtrar por estado
5. [ ] Seleccionar tarea
6. [ ] Ver detalles de tarea
7. [ ] Actualizar estado (PENDIENTE → ASIGNADO → EN_PROGRESO → RESUELTO)
8. [ ] Ver historial de auditoría

### Flujo de Administrador
1. [ ] Login con usuario ADMIN
2. [ ] Acceder a `/admin/config`
3. [ ] Ver configuración actual
4. [ ] Modificar pesos del algoritmo
5. [ ] Guardar cambios
6. [ ] Verificar recalculación de prioridades
7. [ ] Ver historial de configuraciones

### Manejo de Errores
1. [ ] Login con credenciales inválidas → 401
2. [ ] Acceder a ruta protegida sin token → Redirige a login
3. [ ] Acceder a ruta de admin con rol TECNICO → 403
4. [ ] Enviar reporte con coordenadas fuera de geofencing → 400
5. [ ] Enviar reporte con foto > 5MB → 400
6. [ ] Actualizar estado con transición inválida → 400

## 🚀 Pasos para Probar Integración

### 1. Iniciar Backend
```bash
cd backend
mvn spring-boot:run
```
Backend debe estar corriendo en: http://localhost:8080

### 2. Iniciar Frontend
```bash
cd frontend
npm run dev
```
Frontend debe estar corriendo en: http://localhost:5173

### 3. Verificar Conectividad
- Abrir navegador en http://localhost:5173
- Abrir DevTools → Network
- Intentar login
- Verificar que request se envía a http://localhost:8080/api/auth/login

### 4. Verificar CORS
- Si hay error CORS, verificar configuración en `SecurityConfig.java`
- Debe permitir origen: http://localhost:5173

## 📋 Checklist de Despliegue

### Desarrollo
- [x] Frontend: http://localhost:5173
- [x] Backend: http://localhost:8080
- [x] Base de datos: localhost:5432

### Producción (Docker)
- [ ] Frontend: http://localhost:3000 (Nginx)
- [ ] Backend: http://localhost:8080 (Spring Boot)
- [ ] Base de datos: postgres:5432 (Docker network)
- [ ] Variables de entorno configuradas
- [ ] CORS configurado para dominio de producción

## ✅ Estado Actual

**Frontend**: 100% Completado
- Todas las páginas implementadas
- Todos los componentes creados
- Todos los servicios configurados
- Routing completo
- Autenticación y autorización

**Backend**: 100% Completado
- Todos los endpoints implementados
- Autenticación JWT
- Autorización RBAC
- Algoritmo de priorización
- Deduplicación
- Auditoría
- Rate limiting

**Integración**: ✅ Lista para pruebas
- Configuración correcta
- Servicios alineados con endpoints
- Validaciones implementadas
- Manejo de errores configurado

## 🎯 Próximos Pasos

1. **Tarea 23**: Configurar Docker deployment
   - Crear Dockerfiles optimizados
   - Configurar docker-compose
   - Scripts de inicialización de BD

2. **Tarea 24**: Pruebas de integración completas
   - Probar flujos end-to-end
   - Verificar todos los endpoints
   - Validar manejo de errores

3. **Tarea 25**: Validación final del sistema
