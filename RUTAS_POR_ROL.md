# Rutas por Rol de Usuario

Este documento describe las rutas de navegación y permisos para cada rol de usuario en el sistema URBIX.

## Resumen de Roles y Rutas

| Rol | Ruta Inicial | Descripción |
|-----|--------------|-------------|
| **Ciudadano** | `/report` | Página para reportar incidencias |
| **Técnico** | `/dashboard` | Dashboard operacional para gestionar tareas |
| **Administrador** | `/dashboard` | Dashboard operacional + acceso a configuración |

## Rutas Públicas

### `/login`
- **Acceso**: Público
- **Descripción**: Página de inicio de sesión
- **Redirección después del login**:
  - Ciudadano → `/report`
  - Técnico → `/dashboard`
  - Administrador → `/dashboard`

### `/report`
- **Acceso**: Público (no requiere autenticación)
- **Descripción**: Página para que los ciudadanos reporten incidencias de limpieza urbana
- **Funcionalidades**:
  - Formulario de reporte con geolocalización
  - Subida de fotografías
  - Selección de categoría de incidencia
  - Visualización en mapa

## Rutas Protegidas

### `/dashboard`
- **Acceso**: Requiere rol `ROLE_TECNICO` o `ROLE_ADMIN`
- **Descripción**: Dashboard operacional para gestionar tareas
- **Funcionalidades**:
  - Lista de tareas priorizadas
  - Mapa de incidencias
  - Actualización de estado de tareas
  - Métricas de rendimiento
- **Usuarios permitidos**:
  - ✅ Técnico
  - ✅ Administrador
  - ❌ Ciudadano (acceso denegado)

### `/admin/config`
- **Acceso**: Requiere rol `ROLE_ADMIN`
- **Descripción**: Página de configuración del sistema
- **Funcionalidades**:
  - Configuración del algoritmo de priorización
  - Gestión de pesos de categorías
  - Configuración de zonas
  - Parámetros de deduplicación
- **Usuarios permitidos**:
  - ✅ Administrador
  - ❌ Técnico (acceso denegado)
  - ❌ Ciudadano (acceso denegado)

## Comportamiento de Redirección

### Después del Login
El sistema redirige automáticamente según el rol del usuario:

```javascript
// Lógica de redirección
switch (userRole) {
  case 'ROLE_ADMIN':
    return '/dashboard';
  case 'ROLE_TECNICO':
    return '/dashboard';
  case 'ROLE_CIUDADANO':
    return '/report';
  default:
    return '/report';
}
```

### Acceso Denegado
Si un usuario intenta acceder a una ruta sin los permisos necesarios:
- Se muestra una página de "Acceso Denegado"
- Se indica el rol requerido y el rol actual del usuario
- El usuario puede volver a la página de inicio

### Ruta Raíz (`/`)
- Redirige automáticamente a `/report`
- Permite acceso rápido para ciudadanos que quieren reportar

## Credenciales de Prueba

### Ciudadano
```
Username: ciudadano
Password: Ciudadano123!@#
Ruta inicial: /report
```

### Técnico
```
Username: tecnico
Password: Tecnico123!@#
Ruta inicial: /dashboard
```

### Administrador
```
Username: admin
Password: Admin123!@#
Ruta inicial: /dashboard
```

## Flujo de Navegación por Rol

### Ciudadano
1. Login → `/report`
2. Puede reportar incidencias
3. No puede acceder a `/dashboard` ni `/admin/config`

### Técnico
1. Login → `/dashboard`
2. Puede ver y gestionar tareas
3. Puede acceder a `/report` (para ver reportes)
4. No puede acceder a `/admin/config`

### Administrador
1. Login → `/dashboard`
2. Puede ver y gestionar tareas
3. Puede acceder a `/admin/config`
4. Puede acceder a `/report`
5. Acceso completo a todas las rutas

## Notas Importantes

1. **Ruta `/report` es pública**: No requiere autenticación para permitir que cualquier ciudadano pueda reportar incidencias sin necesidad de crear una cuenta.

2. **Dashboard compartido**: Tanto técnicos como administradores usan el mismo dashboard (`/dashboard`), pero los administradores tienen acceso adicional a la configuración.

3. **Protección de rutas**: El componente `ProtectedRoute` verifica automáticamente los permisos antes de renderizar cada página protegida.

4. **Persistencia de sesión**: El token JWT se almacena en localStorage y se renueva automáticamente antes de expirar.

5. **Logout**: Al cerrar sesión, el usuario es redirigido a `/login` independientemente de su rol.

## Troubleshooting

### Problema: "Acceso Denegado" después del login
**Causa**: El usuario está intentando acceder a una ruta sin los permisos necesarios.

**Solución**: 
- Verificar que el rol del usuario es correcto
- Ciudadanos deben ir a `/report`, no a `/dashboard`
- Técnicos no pueden acceder a `/admin/config`

### Problema: Redirección incorrecta después del login
**Causa**: El sistema no está detectando correctamente el rol del usuario.

**Solución**:
1. Verificar que el backend devuelve el campo `role` en la respuesta del login
2. Verificar que el token JWT contiene el claim `role`
3. Limpiar localStorage y volver a iniciar sesión

### Problema: Página en blanco después del login
**Causa**: Error en el frontend al procesar la redirección.

**Solución**:
1. Abrir la consola del navegador (F12)
2. Verificar errores de JavaScript
3. Verificar que el frontend está actualizado: `docker-compose build --no-cache frontend`

---

**Última actualización**: Febrero 17, 2026
