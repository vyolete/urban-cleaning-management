# GitHub Actions Workflows

Este directorio contiene los workflows de CI/CD para el proyecto Urbix.

## Estructura

Los workflows de GitHub Actions se definen en archivos YAML en este directorio.
Cada archivo representa un workflow automatizado que se ejecuta en respuesta a eventos específicos.

## Workflows Disponibles

Los workflows se agregarán aquí según las necesidades del proyecto:

- **CI (Continuous Integration)**: Pruebas automáticas, linting, build
- **CD (Continuous Deployment)**: Despliegue automático a entornos
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
