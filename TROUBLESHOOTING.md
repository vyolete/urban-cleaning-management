# Guía de Solución de Problemas

## Problema: Geolocalización no funciona

### Causa
Los navegadores modernos requieren HTTPS para acceder a la API de geolocalización, excepto cuando se accede desde `localhost`. Cuando usas Docker, el frontend se sirve desde un contenedor y el navegador no lo reconoce como `localhost`.

### Soluciones

#### Opción 1: Usar ubicación manual (Recomendado para pruebas)
1. En el formulario de reporte, haz clic en "Ingresar ubicación manualmente"
2. Ingresa coordenadas de prueba:
   - **Latitud**: 40.4168 (Madrid, España)
   - **Longitud**: -3.7038

#### Opción 2: Acceder vía localhost (Desarrollo local)
En lugar de usar Docker, ejecuta el frontend localmente:

```bash
cd frontend
npm install
npm run dev
```

Luego accede a `http://localhost:5173` - el navegador permitirá geolocalización en localhost.

#### Opción 3: Configurar HTTPS en Docker (Producción)
Para producción, necesitarás:
1. Certificado SSL (Let's Encrypt)
2. Configurar Nginx con HTTPS
3. Actualizar docker-compose.yml

### Verificar permisos del navegador

1. **Chrome/Edge**:
   - Haz clic en el icono de candado/información en la barra de direcciones
   - Verifica que "Ubicación" esté permitida
   - Si está bloqueada, cámbiala a "Permitir"

2. **Firefox**:
   - Haz clic en el icono de información en la barra de direcciones
   - Ve a "Permisos" > "Acceder a tu ubicación"
   - Selecciona "Permitir"

3. **Safari**:
   - Safari > Preferencias > Sitios web > Ubicación
   - Encuentra el sitio y selecciona "Permitir"

## Problema: Error al enviar reporte

### Verificar que el backend esté funcionando

```bash
# Verificar que el contenedor esté corriendo
docker ps

# Ver logs del backend
docker logs urbanclean-backend --tail 50

# Probar el endpoint de salud
curl http://localhost:8080/actuator/health
```

### Verificar conectividad frontend-backend

```bash
# Desde el navegador, abre la consola de desarrollador (F12)
# Verifica que las peticiones a http://localhost:8080/api no den error CORS
```

### Errores comunes

#### Error: "Network Error" o "Failed to fetch"
- **Causa**: El backend no está corriendo o no es accesible
- **Solución**: Verifica que Docker esté corriendo y que el puerto 8080 esté disponible

#### Error: "CORS policy"
- **Causa**: Configuración CORS incorrecta
- **Solución**: Ya está configurado en SecurityConfig.java para permitir localhost:3000 y localhost:5173

#### Error: "Coordinates outside geofencing boundaries"
- **Causa**: Las coordenadas están fuera del área permitida
- **Solución**: Usa coordenadas dentro de Madrid:
  - Latitud: entre 40.3 y 40.6
  - Longitud: entre -3.9 y -3.5

## Problema: Foto no se carga

### Verificar formato y tamaño
- **Formatos permitidos**: JPEG, PNG
- **Tamaño máximo**: 5MB

### Verificar permisos
El directorio de uploads debe existir y tener permisos de escritura:

```bash
# En el contenedor backend
docker exec -it urbanclean-backend ls -la /uploads
```

## Problema: No puedo iniciar sesión

### Crear usuarios de prueba

```bash
# Ejecutar el script de creación de usuarios
./create-test-users.sh
```

Usuarios creados:
- **Ciudadano**: `ciudadano` / `admin123`
- **Técnico**: `tecnico` / `admin123`
- **Admin**: `admin` / `admin123`

### Verificar que la base de datos esté funcionando

```bash
# Conectar a PostgreSQL
docker exec -it urbanclean-postgres psql -U urbanclean_user -d urbanclean

# Verificar usuarios
SELECT username, role FROM usuarios;

# Salir
\q
```

## Comandos útiles

### Reiniciar todos los servicios
```bash
cd docker
docker-compose down
docker-compose up --build
```

### Ver logs en tiempo real
```bash
# Backend
docker logs -f urbanclean-backend

# Frontend
docker logs -f urbanclean-frontend

# PostgreSQL
docker logs -f urbanclean-postgres
```

### Limpiar todo y empezar de cero
```bash
cd docker
docker-compose down -v  # -v elimina los volúmenes (datos)
docker-compose up --build
```

### Verificar estado de los servicios
```bash
# Ver contenedores corriendo
docker ps

# Ver todos los contenedores (incluyendo detenidos)
docker ps -a

# Verificar salud de los contenedores
docker inspect urbanclean-backend | grep -A 10 Health
```

## Contacto y Soporte

Si los problemas persisten:
1. Revisa los logs del backend: `docker logs urbanclean-backend`
2. Revisa la consola del navegador (F12) para errores JavaScript
3. Verifica que todos los servicios estén corriendo: `docker ps`
4. Asegúrate de que los puertos 3000, 8080 y 5432 no estén siendo usados por otras aplicaciones

## Pruebas sin interfaz gráfica

Puedes probar los endpoints directamente con curl:

```bash
# Registrar usuario
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test",
    "password": "test123",
    "email": "test@example.com",
    "fullName": "Test User",
    "role": "CIUDADANO"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ciudadano",
    "password": "admin123"
  }'

# Crear reporte (necesitas el token del login)
curl -X POST http://localhost:8080/api/reports \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -F "data={\"latitude\":40.4168,\"longitude\":-3.7038,\"category\":\"BASURA_ACUMULADA\",\"description\":\"Test report\"}" \
  -F "photo=@/path/to/image.jpg"
```
