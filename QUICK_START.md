# 🚀 Guía de Inicio Rápido
# Urban Cleaning Management System

## ⚡ Inicio Rápido (5 minutos)

### Paso 1: Iniciar el Sistema con Docker

```bash
# 1. Ir al directorio docker
cd docker

# 2. Copiar variables de entorno
cp .env.example .env

# 3. Iniciar todos los servicios
docker-compose up -d --build

# 4. Esperar a que los servicios estén listos (30-60 segundos)
# Ver logs en tiempo real:
docker-compose logs -f
```

### Paso 2: Verificar que Todo Funciona

```bash
# Ejecutar script de verificación
cd ..
./verify-deployment.sh
```

Si todo está bien, verás ✅ en verde para cada servicio.

### Paso 3: Acceder a la Aplicación

Abre tu navegador en:
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **Health Check**: http://localhost:8080/actuator/health

---

## 🧪 Probar el Sistema (Sin Usuarios)

Como el sistema está recién instalado, **NO hay usuarios creados**. Tienes dos opciones:

### Opción A: Crear Usuarios Manualmente (Recomendado)

```bash
# 1. Conectar a PostgreSQL
docker-compose exec postgres psql -U urbanclean_user -d urbanclean

# 2. Copiar y pegar estos comandos en psql:
```

```sql
-- Usuario Ciudadano (username: ciudadano, password: admin123)
INSERT INTO users (id, username, password_hash, email, role, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'ciudadano',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ciudadano@test.com',
    'ROLE_CIUDADANO',
    NOW(),
    NOW()
);

-- Usuario Técnico (username: tecnico, password: admin123)
INSERT INTO users (id, username, password_hash, email, role, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'tecnico',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'tecnico@test.com',
    'ROLE_TECNICO',
    NOW(),
    NOW()
);

-- Usuario Admin (username: admin, password: admin123)
INSERT INTO users (id, username, password_hash, email, role, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'admin@test.com',
    'ROLE_ADMIN',
    NOW(),
    NOW()
);

-- Salir de psql
\q
```

### Opción B: Usar el Endpoint de Registro

```bash
# Registrar un usuario ciudadano
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ciudadano",
    "password": "admin123",
    "email": "ciudadano@test.com",
    "role": "ROLE_CIUDADANO"
  }'
```

---

## 🎮 Flujos de Prueba

### 1️⃣ Probar como Ciudadano (Reportar Incidencia)

1. **Ir a la página de reportes**: http://localhost:3000/report
2. **Permitir geolocalización** cuando el navegador lo solicite
3. **Completar el formulario**:
   - Categoría: Selecciona "Basura acumulada"
   - Descripción: "Contenedor desbordado en la esquina" (mínimo 10 caracteres)
   - Foto: Sube cualquier imagen JPG o PNG (< 5MB)
4. **Clic en "Enviar Reporte"**
5. ✅ Deberías ver un mensaje de éxito

### 2️⃣ Probar como Operador (Gestionar Tareas)

1. **Ir a login**: http://localhost:3000/login
2. **Credenciales**:
   - Usuario: `tecnico`
   - Contraseña: `admin123`
3. **Explorar el dashboard**:
   - Ver lista de tareas ordenadas por prioridad
   - Filtrar por estado (PENDIENTE, ASIGNADO, etc.)
   - Ver mapa con marcadores
   - Seleccionar una tarea para ver detalles
4. **Cambiar estado de una tarea**:
   - Clic en "Asignar" (PENDIENTE → ASIGNADO)
   - Clic en "Iniciar" (ASIGNADO → EN_PROGRESO)
   - Clic en "Resolver" (EN_PROGRESO → RESUELTO)
5. **Ver historial de auditoría** en el timeline

### 3️⃣ Probar como Admin (Configurar Algoritmo)

1. **Ir a login**: http://localhost:3000/login
2. **Credenciales**:
   - Usuario: `admin`
   - Contraseña: `admin123`
3. **Ir a configuración**: http://localhost:3000/admin/config
4. **Modificar pesos del algoritmo**:
   - Cambiar valores (deben sumar 1.0)
   - Clic en "Normalizar" si no suman 1.0
   - Clic en "Guardar Cambios"
5. ✅ Ver que aparece en el historial

---

## 🧪 Pruebas Automatizadas

### Probar API con Script

```bash
# Ejecutar pruebas de API
./test-api-endpoints.sh
```

Este script prueba:
- ✅ Login con credenciales válidas/inválidas
- ✅ Acceso a endpoints protegidos
- ✅ Control de acceso por roles
- ✅ Gestión de tareas
- ✅ Configuración de admin
- ✅ Validaciones

### Probar Integración

```bash
# Ejecutar pruebas de integración
./test-integration.sh
```

---

## 📱 Probar en Diferentes Navegadores

El sistema funciona en:
- ✅ Chrome/Chromium
- ✅ Firefox
- ✅ Safari
- ✅ Edge

**Nota**: La geolocalización requiere HTTPS en producción, pero funciona en localhost.

---

## 🐛 Solución de Problemas Comunes

### Problema 1: "Backend no responde"

```bash
# Ver logs del backend
docker-compose logs backend

# Reiniciar backend
docker-compose restart backend

# Verificar que PostgreSQL está listo
docker-compose exec postgres pg_isready -U urbanclean_user
```

### Problema 2: "Frontend no carga"

```bash
# Ver logs del frontend
docker-compose logs frontend

# Reiniciar frontend
docker-compose restart frontend

# Verificar que Nginx está sirviendo
curl http://localhost:3000/health
```

### Problema 3: "No puedo hacer login"

```bash
# Verificar que los usuarios existen
docker-compose exec postgres psql -U urbanclean_user -d urbanclean -c "SELECT username, role FROM users;"

# Si no hay usuarios, crearlos con los comandos SQL de arriba
```

### Problema 4: "Error de CORS"

```bash
# Verificar configuración CORS en backend
# El backend debe permitir: http://localhost:3000

# Reiniciar backend después de cambios
docker-compose restart backend
```

### Problema 5: "Geolocalización no funciona"

- Asegúrate de permitir el acceso cuando el navegador lo solicite
- En Chrome: Clic en el candado → Permisos del sitio → Ubicación → Permitir
- Si estás en HTTPS, debe funcionar automáticamente

---

## 🔄 Reiniciar Todo desde Cero

Si algo sale mal y quieres empezar de nuevo:

```bash
# 1. Detener y eliminar todo (⚠️ ELIMINA DATOS)
cd docker
docker-compose down -v

# 2. Reconstruir e iniciar
docker-compose up -d --build

# 3. Esperar 30-60 segundos

# 4. Crear usuarios de nuevo (ver Opción A arriba)
```

---

## 📊 Verificar que Todo Funciona

### Checklist Rápido

- [ ] Backend responde en http://localhost:8080/actuator/health
- [ ] Frontend carga en http://localhost:3000
- [ ] PostgreSQL acepta conexiones
- [ ] Puedo hacer login con `admin` / `admin123`
- [ ] Puedo crear un reporte como ciudadano
- [ ] Puedo ver tareas como técnico
- [ ] Puedo cambiar configuración como admin

Si todos tienen ✅, ¡el sistema funciona perfectamente!

---

## 📚 Documentación Adicional

Para pruebas más detalladas, consulta:
- `E2E_TESTING_GUIDE.md` - Guía completa de pruebas E2E
- `INTEGRATION_CHECKLIST.md` - Checklist de integración
- `docker/README.md` - Guía completa de Docker

---

## 🆘 ¿Necesitas Ayuda?

1. Revisa los logs: `docker-compose logs -f`
2. Verifica el estado: `docker-compose ps`
3. Consulta `E2E_TESTING_GUIDE.md` para más detalles
4. Revisa `SYSTEM_VALIDATION.md` para entender el sistema

---

## 🎉 ¡Listo!

Si llegaste hasta aquí y todo funciona, ¡felicidades! El sistema está completamente operativo.

**Próximos pasos**:
- Explorar todas las funcionalidades
- Probar diferentes flujos de usuario
- Revisar el código fuente
- Personalizar según tus necesidades
