# Guía de Pruebas End-to-End (E2E)
# Urban Cleaning Management System

Esta guía describe los flujos completos de usuario que deben probarse para validar la integración del sistema.

## 📋 Prerequisitos

### 1. Iniciar el Sistema

**Opción A: Con Docker (Recomendado)**
```bash
cd docker
cp .env.example .env
# Editar .env si es necesario
docker-compose up -d --build
```

**Opción B: Desarrollo Local**
```bash
# Terminal 1 - Backend
cd backend
mvn spring-boot:run

# Terminal 2 - Frontend
cd frontend
npm run dev

# Terminal 3 - PostgreSQL (si no está en Docker)
# Asegurarse de que PostgreSQL con PostGIS esté corriendo
```

### 2. Verificar Servicios

```bash
# Backend health check
curl http://localhost:8080/actuator/health

# Frontend
curl http://localhost:3000/health  # Docker
curl http://localhost:5173/        # Vite dev

# PostgreSQL
docker-compose exec postgres pg_isready -U urbanclean_user
```

### 3. Datos de Prueba

Crear usuarios de prueba (ejecutar en PostgreSQL):

```sql
-- Usuario Ciudadano
INSERT INTO users (id, username, password_hash, email, role, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'ciudadano',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- password: admin123
    'ciudadano@test.com',
    'ROLE_CIUDADANO',
    NOW(),
    NOW()
);

-- Usuario Técnico
INSERT INTO users (id, username, password_hash, email, role, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'tecnico',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- password: admin123
    'tecnico@test.com',
    'ROLE_TECNICO',
    NOW(),
    NOW()
);

-- Usuario Administrador
INSERT INTO users (id, username, password_hash, email, role, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- password: admin123
    'admin@test.com',
    'ROLE_ADMIN',
    NOW(),
    NOW()
);
```

---

## 🧪 Flujo 1: Ciudadano - Reportar Incidencia

### Objetivo
Verificar que un ciudadano puede reportar una incidencia con geolocalización y foto.

### Pasos

1. **Acceder a la página de reportes**
   - URL: `http://localhost:3000/report` (o `http://localhost:5173/report`)
   - ✅ La página carga correctamente
   - ✅ No requiere autenticación (acceso público)

2. **Permitir geolocalización**
   - El navegador solicita permiso de ubicación
   - ✅ Aceptar permiso
   - ✅ El mapa muestra la ubicación actual
   - ✅ El marcador aparece en el mapa

3. **Completar formulario**
   - **Categoría**: Seleccionar "Basura acumulada"
   - **Descripción**: "Contenedor desbordado en la esquina"
   - **Foto**: Subir una imagen (JPEG/PNG, < 5MB)
   - ✅ Preview de la foto aparece
   - ✅ Validaciones funcionan (descripción mínimo 10 caracteres)

4. **Enviar reporte**
   - Clic en "Enviar Reporte"
   - ✅ Mensaje de éxito aparece
   - ✅ Formulario se limpia
   - ✅ Se puede enviar otro reporte

### Validaciones Backend

```bash
# Verificar que el reporte se creó
curl -X GET http://localhost:8080/api/reports \
  -H "Authorization: Bearer {token_admin}"

# Verificar que se creó una tarea
curl -X GET http://localhost:8080/api/tasks \
  -H "Authorization: Bearer {token_tecnico}"
```

### Criterios de Éxito

- ✅ Reporte creado en base de datos
- ✅ Foto almacenada en `/uploads`
- ✅ Tarea creada automáticamente con estado PENDIENTE
- ✅ Prioridad calculada correctamente
- ✅ Coordenadas validadas (dentro de geofencing)

---

## 🧪 Flujo 2: Operador - Gestionar Tareas

### Objetivo
Verificar que un operador puede ver, filtrar y actualizar el estado de las tareas.

### Pasos

1. **Login como técnico**
   - URL: `http://localhost:3000/login`
   - Usuario: `tecnico`
   - Contraseña: `admin123`
   - ✅ Login exitoso
   - ✅ Redirige a `/dashboard`

2. **Ver lista de tareas**
   - ✅ Tareas aparecen ordenadas por prioridad (descendente)
   - ✅ Se muestran: ID, ubicación, categoría, estado, prioridad
   - ✅ Badges de prioridad con colores (alta=rojo, media=amarillo, baja=verde)

3. **Filtrar tareas**
   - **Por estado**: Seleccionar "PENDIENTE"
   - ✅ Solo muestra tareas pendientes
   - **Por zona**: Seleccionar una zona
   - ✅ Solo muestra tareas de esa zona

4. **Ver mapa de tareas**
   - Cambiar a vista "Mapa" o "Dividida"
   - ✅ Marcadores aparecen en el mapa
   - ✅ Colores según prioridad
   - ✅ Popup muestra información al hacer clic

5. **Seleccionar tarea**
   - Clic en una tarea de la lista
   - ✅ Panel de detalles aparece
   - ✅ Muestra toda la información:
     - Ubicación, categoría, descripción
     - Foto del reporte
     - Estado actual
     - Prioridad
     - Contador de duplicados
     - Botones de transición de estado

6. **Actualizar estado de tarea**
   - **PENDIENTE → ASIGNADO**
     - Clic en "Asignar"
     - ✅ Estado cambia a ASIGNADO
     - ✅ Botón "Iniciar" aparece
   
   - **ASIGNADO → EN_PROGRESO**
     - Clic en "Iniciar"
     - ✅ Estado cambia a EN_PROGRESO
     - ✅ Botón "Resolver" aparece
   
   - **EN_PROGRESO → RESUELTO**
     - Clic en "Resolver"
     - ✅ Estado cambia a RESUELTO
     - ✅ Tarea desaparece de lista de pendientes

7. **Ver historial de auditoría**
   - Scroll al timeline de auditoría
   - ✅ Muestra todos los cambios de estado
   - ✅ Incluye usuario, timestamp, transición
   - ✅ Orden cronológico

### Validaciones Backend

```bash
# Verificar cambios de estado
curl -X GET http://localhost:8080/api/tasks/{task_id}/audit-history \
  -H "Authorization: Bearer {token_tecnico}"

# Verificar que no se permiten transiciones inválidas
curl -X PATCH http://localhost:8080/api/tasks/{task_id}/state \
  -H "Authorization: Bearer {token_tecnico}" \
  -H "Content-Type: application/json" \
  -d '{"newState": "RESUELTO"}'  # Desde PENDIENTE (debe fallar)
```

### Criterios de Éxito

- ✅ Máquina de estados funciona correctamente
- ✅ Transiciones inválidas son rechazadas (400)
- ✅ Auditoría registra todos los cambios
- ✅ UI se actualiza en tiempo real
- ✅ Filtros funcionan correctamente

---

## 🧪 Flujo 3: Administrador - Configurar Algoritmo

### Objetivo
Verificar que un administrador puede modificar los pesos del algoritmo de priorización.

### Pasos

1. **Login como admin**
   - URL: `http://localhost:3000/login`
   - Usuario: `admin`
   - Contraseña: `admin123`
   - ✅ Login exitoso
   - ✅ Puede acceder a `/admin/config`

2. **Ver configuración actual**
   - URL: `http://localhost:3000/admin/config`
   - ✅ Muestra pesos actuales:
     - Wc (Categoría): 0.40 (40%)
     - Wz (Zona): 0.35 (35%)
     - Wt (Tiempo): 0.25 (25%)
   - ✅ Muestra configuración de deduplicación
   - ✅ Muestra historial de configuraciones

3. **Modificar pesos**
   - Cambiar valores:
     - Wc: 0.50
     - Wz: 0.30
     - Wt: 0.20
   - ✅ Suma se calcula en tiempo real
   - ✅ Porcentajes se actualizan
   - ✅ Validación: suma debe ser 1.0

4. **Probar validación**
   - Intentar guardar con suma ≠ 1.0
   - ✅ Error mostrado: "Los pesos deben sumar 1.0"
   - ✅ No permite guardar

5. **Normalizar pesos**
   - Clic en "Normalizar"
   - ✅ Pesos se ajustan automáticamente para sumar 1.0

6. **Guardar configuración**
   - Clic en "Guardar Cambios"
   - ✅ Mensaje de éxito
   - ✅ Configuración guardada
   - ✅ Aparece en historial con timestamp

7. **Verificar recalculación**
   - Ir a dashboard de operador
   - ✅ Prioridades de tareas pendientes se recalcularon
   - ✅ Orden puede haber cambiado

### Validaciones Backend

```bash
# Verificar configuración actual
curl -X GET http://localhost:8080/api/admin/config/algorithm-weights \
  -H "Authorization: Bearer {token_admin}"

# Verificar historial
curl -X GET http://localhost:8080/api/admin/config/algorithm-weights/history \
  -H "Authorization: Bearer {token_admin}"

# Intentar actualizar sin ser admin (debe fallar con 403)
curl -X PUT http://localhost:8080/api/admin/config/algorithm-weights \
  -H "Authorization: Bearer {token_tecnico}" \
  -H "Content-Type: application/json" \
  -d '{"weightCategory": 0.5, "weightZone": 0.3, "weightTime": 0.2}'
```

### Criterios de Éxito

- ✅ Solo admin puede acceder
- ✅ Validación de suma funciona
- ✅ Configuración se guarda correctamente
- ✅ Historial se mantiene
- ✅ Recalculación de prioridades se ejecuta
- ✅ Técnicos no pueden acceder (403)

---

## 🧪 Flujo 4: Autenticación y Autorización

### Objetivo
Verificar que el sistema de autenticación y control de acceso funciona correctamente.

### Pasos

1. **Login con credenciales inválidas**
   - Usuario: `invalid`
   - Contraseña: `wrong`
   - ✅ Error 401: "Credenciales inválidas"
   - ✅ No se almacena token

2. **Login exitoso**
   - Usuario: `tecnico`
   - Contraseña: `admin123`
   - ✅ Token JWT recibido
   - ✅ Token almacenado en localStorage
   - ✅ Usuario almacenado en localStorage

3. **Acceso a rutas protegidas sin token**
   - Borrar localStorage
   - Intentar acceder a `/dashboard`
   - ✅ Redirige a `/login`
   - ✅ Guarda ubicación para redirect post-login

4. **Acceso a ruta de admin sin rol**
   - Login como `tecnico`
   - Intentar acceder a `/admin/config`
   - ✅ Muestra mensaje "Acceso Denegado"
   - ✅ No muestra contenido de admin

5. **Token expirado**
   - Esperar 24 horas (o modificar token manualmente)
   - Hacer request a API
   - ✅ Error 401
   - ✅ Redirige a login
   - ✅ Token eliminado de localStorage

6. **Logout**
   - Clic en botón de logout (si existe)
   - ✅ Token eliminado
   - ✅ Usuario eliminado
   - ✅ Redirige a login

### Validaciones Backend

```bash
# Login exitoso
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "tecnico", "password": "admin123"}'

# Login fallido
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "invalid", "password": "wrong"}'

# Acceso sin token
curl -X GET http://localhost:8080/api/tasks

# Acceso con token inválido
curl -X GET http://localhost:8080/api/tasks \
  -H "Authorization: Bearer invalid_token"
```

### Criterios de Éxito

- ✅ Login con credenciales válidas funciona
- ✅ Login con credenciales inválidas falla (401)
- ✅ Token JWT se genera correctamente
- ✅ Rutas protegidas requieren autenticación
- ✅ Control de acceso por roles funciona
- ✅ Token expirado es rechazado
- ✅ Logout limpia sesión

---

## 🧪 Flujo 5: Deduplicación de Reportes

### Objetivo
Verificar que el sistema detecta y agrupa reportes duplicados.

### Pasos

1. **Crear primer reporte**
   - Ubicación: Lat 40.4168, Lon -3.7038
   - Categoría: "Basura acumulada"
   - ✅ Reporte creado
   - ✅ Tarea creada con duplicateCount = 0

2. **Crear reporte duplicado (misma ubicación)**
   - Ubicación: Lat 40.4169, Lon -3.7039 (< 50m del primero)
   - Categoría: "Basura acumulada"
   - Dentro de 24 horas
   - ✅ Reporte creado
   - ✅ Detectado como duplicado
   - ✅ Vinculado a tarea existente
   - ✅ duplicateCount incrementado a 1

3. **Verificar en dashboard**
   - Login como técnico
   - Ver tarea en dashboard
   - ✅ Muestra "2 reportes" o badge de duplicados
   - ✅ Prioridad puede haber aumentado

4. **Crear reporte en ubicación diferente**
   - Ubicación: Lat 40.5000, Lon -3.8000 (> 50m)
   - ✅ Nueva tarea creada (no duplicado)

### Validaciones Backend

```bash
# Verificar tarea con duplicados
curl -X GET http://localhost:8080/api/tasks/{task_id} \
  -H "Authorization: Bearer {token_tecnico}"

# Debe mostrar duplicateCount > 0
```

### Criterios de Éxito

- ✅ Reportes cercanos (< 50m) se detectan como duplicados
- ✅ Reportes se vinculan a tarea existente
- ✅ Contador de duplicados se actualiza
- ✅ Reportes lejanos crean nuevas tareas
- ✅ Ventana temporal funciona (24 horas)

---

## 🧪 Flujo 6: Validaciones y Manejo de Errores

### Objetivo
Verificar que el sistema maneja correctamente errores y validaciones.

### Pasos

1. **Reporte con coordenadas fuera de geofencing**
   - Ubicación: Lat 50.0, Lon 10.0 (fuera de Madrid)
   - ✅ Error 400: "Coordenadas fuera del área de servicio"

2. **Reporte sin foto**
   - Intentar enviar sin foto
   - ✅ Error de validación: "Foto requerida"

3. **Reporte con foto muy grande**
   - Foto > 5MB
   - ✅ Error: "Foto debe ser menor a 5MB"

4. **Reporte con foto de tipo inválido**
   - Archivo .pdf o .txt
   - ✅ Error: "Solo se permiten JPEG y PNG"

5. **Descripción muy corta**
   - Descripción < 10 caracteres
   - ✅ Error: "Descripción debe tener al menos 10 caracteres"

6. **Transición de estado inválida**
   - Intentar PENDIENTE → RESUELTO (saltando estados)
   - ✅ Error 400: "Transición de estado inválida"

7. **Rate limiting en login**
   - Intentar login 6 veces en 5 minutos
   - ✅ Error 429: "Demasiados intentos"

### Criterios de Éxito

- ✅ Todas las validaciones funcionan
- ✅ Mensajes de error son claros
- ✅ Códigos HTTP correctos (400, 401, 403, 429)
- ✅ Frontend muestra errores al usuario

---

## 📊 Checklist de Validación Final

### Funcionalidad Core
- [ ] Ciudadanos pueden reportar incidencias
- [ ] Geolocalización funciona correctamente
- [ ] Fotos se suben y almacenan
- [ ] Tareas se crean automáticamente
- [ ] Prioridad se calcula correctamente
- [ ] Deduplicación funciona

### Dashboard de Operadores
- [ ] Lista de tareas se muestra ordenada
- [ ] Filtros funcionan (estado, zona)
- [ ] Mapa muestra tareas con marcadores
- [ ] Detalles de tarea se muestran
- [ ] Estados se pueden actualizar
- [ ] Máquina de estados funciona
- [ ] Auditoría registra cambios

### Panel de Administración
- [ ] Solo admin puede acceder
- [ ] Configuración actual se muestra
- [ ] Pesos se pueden modificar
- [ ] Validación de suma funciona
- [ ] Configuración se guarda
- [ ] Historial se mantiene
- [ ] Recalculación se ejecuta

### Autenticación y Seguridad
- [ ] Login funciona
- [ ] Logout funciona
- [ ] JWT se genera y valida
- [ ] Rutas protegidas requieren auth
- [ ] Control de acceso por roles
- [ ] Token expirado es rechazado
- [ ] Rate limiting funciona

### Validaciones
- [ ] Geofencing valida coordenadas
- [ ] Validación de archivos funciona
- [ ] Validación de campos requeridos
- [ ] Transiciones de estado validadas
- [ ] Mensajes de error claros

### Performance
- [ ] Página carga en < 3 segundos
- [ ] API responde en < 500ms
- [ ] Mapa renderiza sin lag
- [ ] Imágenes se cargan correctamente

### Compatibilidad
- [ ] Funciona en Chrome
- [ ] Funciona en Firefox
- [ ] Funciona en Safari
- [ ] Responsive en móvil
- [ ] Responsive en tablet

---

## 🐛 Reporte de Bugs

Si encuentras bugs durante las pruebas, documéntalos aquí:

### Template de Bug Report

```markdown
**Bug ID**: #001
**Severidad**: Alta/Media/Baja
**Flujo**: [Nombre del flujo]
**Paso**: [Número de paso]

**Descripción**:
[Descripción clara del problema]

**Pasos para reproducir**:
1. [Paso 1]
2. [Paso 2]
3. [Paso 3]

**Resultado esperado**:
[Qué debería pasar]

**Resultado actual**:
[Qué pasa realmente]

**Screenshots/Logs**:
[Si aplica]

**Ambiente**:
- Browser: [Chrome/Firefox/Safari]
- OS: [Windows/Mac/Linux]
- Versión: [Versión del sistema]
```

---

## ✅ Conclusión

Una vez completadas todas las pruebas y verificado el checklist, el sistema está listo para:
- ✅ Demostración
- ✅ Despliegue en staging
- ✅ Pruebas de usuario
- ✅ Despliegue en producción

**Fecha de pruebas**: _______________
**Probado por**: _______________
**Estado**: ⬜ Pendiente | ⬜ En Progreso | ⬜ Completado
