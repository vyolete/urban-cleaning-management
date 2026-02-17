# GitHub Actions Workflows

Este directorio contiene los workflows de CI/CD para el proyecto Urbix.

## Estructura

Los workflows de GitHub Actions se definen en archivos YAML en este directorio.
Cada archivo representa un workflow automatizado que se ejecuta en respuesta a eventos específicos.

## Workflows Disponibles

### 1. Deploy Urbix (`deploy.yml`)

Workflow de CI/CD para despliegue automático a AWS.

**Triggers:**
- Push a rama `main` (cuando cambian: frontend, backend, docker, o el workflow)
- Ejecución manual (workflow_dispatch) con opciones para seleccionar servicios

**Flujo:**
1. **Detección de cambios**: Identifica qué servicios (frontend/backend) han cambiado
2. **Build & Push Frontend**: Construye imagen Docker y la sube a AWS ECR
3. **Build & Push Backend**: Construye imagen Docker y la sube a AWS ECR
4. **Deploy**: Ejecuta Instance Refresh en Auto Scaling Group para actualizar instancias

**Configuración AWS:**
- Región: `us-west-2`
- ECR Repositories: `urbix/frontend`, `urbix/backend`
- Auto Scaling Group: `urbix-dev-asg`
- Autenticación: OIDC con rol IAM

**Secrets Requeridos:**
- `AWS_DEPLOY_ROLE_ARN`: ARN del rol IAM para despliegue

**Tags de Imágenes:**
- `{SHA}-{timestamp}`: Tag único por commit
- `latest`: Tag para la última versión

### Workflows Futuros

Los siguientes workflows se pueden agregar según necesidades:

- **CI (Continuous Integration)**: Pruebas automáticas, linting, build
- **Code Quality**: Análisis de código, cobertura de pruebas
- **Security**: Escaneo de vulnerabilidades

## Documentación

Para más información sobre GitHub Actions:
- [Documentación oficial](https://docs.github.com/en/actions)
- [Sintaxis de workflow](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions)

## Notas

- Los workflows se ejecutan en runners de GitHub
- Pueden usar secrets configurados en el repositorio
- Se pueden activar por push, pull request, schedule, o manualmente
