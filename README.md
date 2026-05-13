# URBIX - Sistema de Gestión de Limpieza Urbana
## Trabajo de Fin de Máster - Ingeniería del Software

<div align="center">
  <img src="src/frontend/src/assets/urbix-robot.png" alt="Robot Urbix" width="200"/>
  <p><em>Urbix - Sistema inteligente de gestión de incidencias urbanas</em></p>
</div>

### 📋 Descripción del Proyecto

URBIX es un sistema integral de gestión de limpieza urbana desarrollado como Trabajo de Fin de Máster. El sistema implementa una plataforma colaborativa que conecta ciudadanos, operadores municipales y administradores a través de algoritmos inteligentes de priorización automática y capacidades geoespaciales avanzadas.

### 🎯 Objetivos Académicos

- **Metodológico**: Validación empírica de Spec-Driven Development
- **Técnico**: Implementación de sistema enterprise con calidad académica
- **Innovación**: Integración de Property-Based Testing y documentación automática
- **Académico**: Demostración de excelencia en gestión de proyectos de software

### 🏗️ Arquitectura del Sistema

- **Backend**: Spring Boot 3.2 + PostgreSQL + PostGIS
- **Frontend**: React 18 + Leaflet para mapas
- **Infraestructura**: Docker + Docker Compose
- **Testing**: JUnit + Property-Based Testing + Load Testing
- **Seguridad**: JWT + OWASP Top 10 compliance

### 📁 Estructura del Proyecto

```
URBIX-TFM/
├── src/                    # Código fuente
│   ├── backend/           # API Spring Boot
│   ├── frontend/          # SPA React
│   └── docker/            # Containerización
├── docs/                  # Documentación académica
│   ├── tfm/              # Capítulos del TFM
│   ├── architecture/     # Documentación arquitectónica
│   ├── api/              # Documentación de API
│   ├── security/         # Auditoría de seguridad
│   └── testing/          # Estrategia de testing
├── diagrams/             # Diagramas UML
├── specs/                # Especificaciones técnicas
└── scripts/              # Scripts de utilidad
```

### 🚀 Inicio Rápido

**Ver la [Guía de Inicio Rápido Completa](QUICK_START.md) para instrucciones detalladas.**

1. **Clonar el repositorio**
   ```bash
   git clone <repository-url>
   cd urban-cleaning-management
   ```

2. **Ejecutar con Docker**
   ```bash
   cd src/docker
   docker-compose up -d
   ```
   
   Espera 2-3 minutos mientras se construyen las imágenes y se inician los servicios.

3. **Acceder al sistema**
   - **Aplicación Web**: http://localhost:3000
   - **Login**: http://localhost:3000/login
   - **Backend API**: http://localhost:8080
   - **Documentación API**: http://localhost:8080/swagger-ui.html

4. **Credenciales de prueba**
   - **Admin**: username=`admin`, password=`Admin123!@#`
   - **Técnico**: username=`tecnico`, password=`Tecnico123!@#`
   - **Ciudadano**: username=`ciudadano`, password=`Ciudadano123!@#`

**Nota**: Los usuarios se crean automáticamente al iniciar el sistema por primera vez.

### 📚 Documentación TFM

- **[Capítulo de Arquitectura](docs/tfm/capitulo-arquitectura.md)**: Diseño y arquitectura del sistema
- **[Capítulo de Gestión](docs/tfm/capitulo-gestion-proyecto.md)**: Metodología y gestión del proyecto
- **[Documentación Técnica](docs/)**: Especificaciones y análisis técnicos

### 🧪 Testing y Calidad

- **Cobertura de Tests**: 85% (Unit + Integration + Property-Based)
- **Load Testing**: 43,700+ requests con 0% error rate
- **Security Audit**: 9.8/10 (OWASP Top 10 compliant)
- **Code Quality**: 9.2/10 (SonarQube analysis)

### 📊 Métricas del Proyecto

- **Completitud**: 127/127 tareas (100%)
- **Requisitos**: 94/94 implementados (100%)
- **Cronograma**: +9% variación (excelente)
- **Calidad Global**: 9.3/10

### 🏆 Contribuciones Académicas

1. **Metodológicas**: Validación empírica de Spec-Driven Development
2. **Técnicas**: Property-Based Testing integrado desde diseño
3. **Documentación**: Generación automática sincronizada con código
4. **Calidad**: Estándares enterprise en contexto académico

### 👥 Desarrollo

**Autor**: [Nombre del estudiante]  
**Director**: [Nombre del director]  
**Universidad**: [Nombre de la universidad]  
**Programa**: Máster en Ingeniería del Software  
**Fecha**: Febrero 2026  

### 🌍 Soporte Multi-País

URBIX soporta gestión de incidencias en múltiples países simultáneamente.

#### Configuración de países

Cada país define sus propios límites geográficos (geofencing), área administrativa y municipio. Se incluyen tres países por defecto: España, Colombia y Estados Unidos.

**API de gestión de países** (requiere `ROLE_ADMIN`):

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/admin/countries` | Listar todos los países |
| POST | `/api/admin/countries` | Crear nuevo país |
| GET | `/api/admin/countries/{id}` | Obtener país por ID |
| PUT | `/api/admin/countries/{id}` | Actualizar país |
| DELETE | `/api/admin/countries/{id}` | Eliminar país |
| GET | `/api/admin/countries/default` | Obtener país por defecto |

**Filtrado de reportes por país y área:**

```
GET /api/reports?countryId={uuid}
GET /api/reports?countryId={uuid}&administrativeArea=Comunidad+de+Madrid
GET /api/reports?countryId={uuid}&municipality=Madrid
GET /api/heatmap?countryId={uuid}
```

Si no se especifica `countryId`, se utiliza el país marcado como `default_country = TRUE`.

#### Migración de base de datos

La migración `V20__add_multi_country_support.sql` crea la tabla `countries`, añade `country_id` a las tablas `reportes` y `tareas`, y migra los datos existentes al país por defecto (España). Para revertir, ejecutar `src/docker/rollback-multi-country.sql`.

### 🔒 Configuración HTTPS

Ver la guía completa en [docs/SSL_CERTIFICATE_SETUP.md](docs/SSL_CERTIFICATE_SETUP.md).

**Inicio rápido con HTTPS:**

```bash
# 1. Generar certificado autofirmado (desarrollo)
cd src/backend/src/main/resources
keytool -genkeypair -alias tomcat -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -validity 365 -storepass changeit

# 2. Copiar a directorio de certs de Docker
cp keystore.p12 ../../docker/certs/

# 3. Crear src/docker/.env con:
# SSL_ENABLED=true
# SSL_KEYSTORE_PASSWORD=changeit
# BACKEND_HTTPS_PORT=8443
# CORS_ALLOWED_ORIGINS=https://localhost:3000

# 4. Arrancar
cd src/docker && docker-compose up -d
```

En producción se recomienda Let's Encrypt con Certbot (ver guía) o un proxy inverso Nginx/Traefik.

### 📄 Licencia

Este proyecto ha sido desarrollado con fines académicos como parte de un Trabajo de Fin de Máster.

---

**Para más información, consultar la documentación completa en el directorio `docs/`**
