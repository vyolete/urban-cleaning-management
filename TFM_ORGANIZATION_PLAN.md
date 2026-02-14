# Plan de Organización del Proyecto URBIX para TFM

## Estructura Propuesta para Entrega Académica

```
URBIX-TFM/
├── README.md                           # Descripción general del proyecto TFM
├── QUICK_START.md                      # Guía rápida de instalación y ejecución
├── 
├── 📁 src/                            # Código fuente del sistema
│   ├── backend/                       # Aplicación Spring Boot
│   │   ├── src/
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── README.md
│   ├── frontend/                      # Aplicación React
│   │   ├── src/
│   │   ├── package.json
│   │   └── README.md
│   └── docker/                        # Configuración de contenedores
│       ├── docker-compose.yml
│       └── README.md
│
├── 📁 docs/                           # Documentación académica y técnica
│   ├── tfm/                          # Documentos específicos del TFM
│   │   ├── capitulo-arquitectura.md
│   │   ├── capitulo-gestion-proyecto.md
│   │   ├── capitulo-arquitectura.docx
│   │   └── capitulo-gestion-proyecto.docx
│   ├── architecture/                 # Documentación arquitectónica
│   │   ├── 01-use-case-view.md
│   │   ├── 02-logical-view.md
│   │   ├── 03-data-model-view.md
│   │   ├── 04-mvc-view.md
│   │   ├── 05-process-view.md
│   │   ├── 06-deployment-view.md
│   │   ├── 07-implementation-view.md
│   │   └── 08-design-decisions.md
│   ├── api/                          # Documentación de API
│   │   └── openapi.yaml
│   ├── security/                     # Documentación de seguridad
│   │   └── security-audit-report.md
│   └── testing/                      # Documentación de testing
│       ├── load-test-analysis.md
│       └── testing-strategy.md
│
├── 📁 diagrams/                       # Diagramas UML y arquitectónicos
│   ├── use-cases/
│   ├── sequence/
│   ├── class/
│   ├── deployment/
│   └── README.md
│
├── 📁 specs/                          # Especificaciones del proyecto
│   ├── requirements/
│   ├── design/
│   └── README.md
│
├── 📁 scripts/                        # Scripts de utilidad
│   ├── setup/
│   ├── deployment/
│   └── testing/
│
└── 📁 deliverables/                   # Entregables finales del TFM
    ├── URBIX_Sistema_Completo.zip
    ├── URBIX_Documentacion_TFM.pdf
    └── URBIX_Presentacion.pptx
```

## Categorización de Archivos Actuales

### ✅ MANTENER - Esenciales para TFM

#### Código Fuente
- `backend/` (completo) - Aplicación principal
- `frontend/` (completo) - Interfaz de usuario
- `docker/` (completo) - Containerización

#### Documentación Académica
- `docs/architecture/` - Documentación arquitectónica UML
- `diagrams/` - Diagramas técnicos
- `URBIX_Gestion_Proyecto_TFM_Capitulo.md/.docx` - Capítulo de gestión
- `docs/architecture/tfm-capitulo-arquitectura.md` - Capítulo de arquitectura

#### Especificaciones
- `.kiro/specs/` - Especificaciones técnicas del proyecto

#### Documentación Técnica
- `backend/SECURITY_AUDIT_REPORT.md`
- `backend/load-tests/LOAD_TEST_ANALYSIS.md`
- `PRODUCTION_READINESS_SUMMARY.md`

### 🔄 REORGANIZAR - Mover a ubicaciones apropiadas

#### Scripts de Utilidad
- `verify-deployment.sh` → `scripts/deployment/`
- `test-*.sh` → `scripts/testing/`
- `convert-*.py` → `scripts/utils/`

#### Documentación de Proceso
- `QUICK_START.md` → Raíz (mejorado)
- `README.md` → Raíz (reescrito para TFM)
- `TROUBLESHOOTING.md` → `docs/operations/`

### ❌ ELIMINAR - No relevantes para TFM

#### Archivos Temporales
- `~$*.docx` (archivos temporales de Word)
- `.DS_Store`
- Archivos de desarrollo temporal

#### Documentación Redundante
- `URBIX_Project_Management_Documentation.md` (versión original)
- `PROYECTO_MANAGEMENT_DOCUMENTATION_SUMMARY.md`
- `URBIX_Analysis_Summary.md`
- Múltiples versiones del mismo documento

#### Scripts de Desarrollo
- `create-test-users.sh`
- `rename-diagrams.sh`
- `export-diagrams.sh`

### 📝 CREAR NUEVOS

#### README Principal
- README.md académico explicando el proyecto TFM
- Estructura clara del repositorio
- Instrucciones de instalación y ejecución

#### Documentación de Entrega
- Guía de evaluación para tribunal
- Checklist de entregables
- Instrucciones de despliegue

## Acciones Propuestas

1. **Crear nueva estructura de directorios**
2. **Mover archivos a ubicaciones apropiadas**
3. **Eliminar archivos redundantes/temporales**
4. **Crear README académico principal**
5. **Consolidar documentación TFM**
6. **Actualizar .gitignore**
7. **Crear scripts de organización**

¿Procedo con la reorganización?