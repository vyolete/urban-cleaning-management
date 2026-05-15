---
name: project-overview
description: URBIX urban cleaning management system — purpose, tech stack, key facts for future sessions
metadata:
  type: project
---

Spring Boot 3.2 (Java 17) backend + React 18/Vite frontend + PostgreSQL/PostGIS.
Multi-country support (España default, Colombia COL, USA). JWT auth, role-based: ROLE_ADMIN, ROLE_TECNICO, ROLE_CIUDADANO.
Reports are citizen-submitted cleaning incidents (table: `reportes`), linked to Tasks (table: `tareas`) via priority algorithm.
Anonymous report submission is allowed (POST /api/reports is public).
Docker Compose in `src/docker/docker-compose.yml`; init SQL at `src/docker/init-db.sql` (runs before Spring Boot).
DataInitializer seeds default users (admin/tecnico/ciudadano) on first run.
DataSeeder (Order 2) seeds 500 Colombian city reports when reports table is empty.

**Why:** Academic TFM (Master's thesis) project — multi-country urban cleaning management.
**How to apply:** Context for suggesting features, explaining architecture choices, keeping scope aligned with academic requirements.
