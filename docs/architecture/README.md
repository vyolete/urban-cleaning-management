# Urban Cleaning Management System - Architecture Documentation

## Table of Contents

1. [Overview](#overview)
2. [Document Structure](#document-structure)
   - [Executive Summary](#0-executive-summary)
   - [Use Case View](#1-use-case-view)
   - [Logical View](#2-logical-view)
   - [Data Model View](#3-data-model-view)
   - [MVC Architecture View](#4-mvc-architecture-view)
   - [Process View](#5-process-view)
   - [Deployment View](#6-deployment-view)
   - [Implementation View](#7-implementation-view)
   - [Design Decisions](#8-design-decisions)
3. [Technology Stack](#technology-stack)
4. [Documentation Standards](#documentation-standards)
5. [How to Use This Documentation](#how-to-use-this-documentation)
6. [Diagram Notation](#diagram-notation)
7. [Source Code References](#source-code-references)
8. [Maintenance](#maintenance)

---

## Overview

This directory contains comprehensive architectural documentation for the Urban Cleaning Management System, generated through systematic analysis of the existing codebase. The documentation follows UML standards and is organized into seven architectural views as defined by the 4+1 architectural view model.

The Urban Cleaning Management System is a full-stack web application that enables:
- **Citizens** to report urban cleaning issues with geolocation and photos
- **Operators** to manage and resolve cleaning tasks
- **Administrators** to configure system parameters and monitor performance

All documentation is derived from actual source code analysis, ensuring accuracy and reliability for academic and technical evaluation.

## Document Structure

This documentation is organized into the following architectural views:

### 0. [Executive Summary](00-executive-summary.md)
High-level overview of the system architecture, technology stack, and key architectural decisions. **Start here** for a quick understanding of the system.

### 1. [Use Case View](01-use-case-view.md)
Describes the system's functionality from the perspective of external actors (Citizen, Operator, Administrator). Includes:
- Actor identification and roles (4 actors: Anonymous, Citizen, Operator, Administrator)
- Complete use case catalog (49 use cases)
- Detailed use case specifications for key workflows
- Use case diagrams grouped by functional area
- Activity diagrams for complex workflows

**Key Sections**:
- [Actor Identification](01-use-case-view.md#actor-identification)
- [Use Case Catalog](01-use-case-view.md#use-case-catalog)
- [Use Case Specifications](01-use-case-view.md#use-case-specifications)
- [Use Case Diagrams](01-use-case-view.md#use-case-diagrams)
- [Activity Diagrams](01-use-case-view.md#activity-diagrams)

### 2. [Logical View](02-logical-view.md)
Describes the internal structure and design of the system. Includes:
- Sequence diagrams for 10+ critical workflows (login, report submission, task management, etc.)
- Comprehensive class diagrams showing entities, DTOs, services, controllers, and repositories
- State diagrams for stateful entities (Task state machine)
- Collaboration diagrams showing component interactions
- Component roles and responsibilities

**Key Sections**:
- [Sequence Diagrams](02-logical-view.md#sequence-diagrams)
- [Class Diagrams](02-logical-view.md#class-diagrams)
- [State Diagrams](02-logical-view.md#state-diagrams)
- [Component Responsibilities](02-logical-view.md#component-responsibilities)

### 3. [Data Model View](03-data-model-view.md)
Describes the persistent data structure and relationships. Includes:
- Complete entity catalog (15+ entities)
- Entity attributes with data types and constraints
- Relationship documentation with cardinality (OneToMany, ManyToOne, etc.)
- Primary keys, foreign keys, and indexes
- Database schema diagrams (ER diagrams)
- PostGIS spatial data integration

**Key Sections**:
- [Entity Catalog](03-data-model-view.md#entity-catalog)
- [Entity Relationships](03-data-model-view.md#entity-relationships)
- [Database Schema Diagram](03-data-model-view.md#database-schema-diagram)
- [Indexes and Constraints](03-data-model-view.md#indexes-and-constraints)

### 4. [MVC Architecture View](04-mvc-view.md)
Describes the Model-View-Controller architectural pattern implementation. Includes:
- View components (React frontend: pages, components, hooks)
- Controller components (REST API: 13+ controllers)
- Model components (Entities and DTOs)
- Communication patterns between layers (HTTP, JSON, state management)
- MVC architecture diagram

**Key Sections**:
- [View Layer](04-mvc-view.md#view-layer)
- [Controller Layer](04-mvc-view.md#controller-layer)
- [Model Layer](04-mvc-view.md#model-layer)
- [Communication Patterns](04-mvc-view.md#communication-patterns)

### 5. [Process View](05-process-view.md)
Describes the system's runtime behavior and business processes. Includes:
- Main business process identification (10+ processes)
- Process flow documentation with entry/exit points
- Process criticality classification (primary vs. secondary)
- Process models showing workflow execution
- Process dependencies and integration points

**Key Sections**:
- [Business Processes](05-process-view.md#business-processes)
- [Process Flows](05-process-view.md#process-flows)
- [Process Classification](05-process-view.md#process-classification)
- [Process Dependencies](05-process-view.md#process-dependencies)

### 6. [Deployment View](06-deployment-view.md)
Describes the physical deployment architecture. Includes:
- Deployment component inventory (3 Docker containers)
- Container and service configuration (PostgreSQL, Backend, Frontend)
- Component dependencies and network topology
- Environment requirements and configuration
- Deployment diagrams showing physical/logical nodes
- Port mappings and volume mounts

**Key Sections**:
- [Deployment Components](06-deployment-view.md#deployment-components)
- [Docker Configuration](06-deployment-view.md#docker-configuration)
- [Network Topology](06-deployment-view.md#network-topology)
- [Deployment Diagram](06-deployment-view.md#deployment-diagram)

### 7. [Implementation View](07-implementation-view.md)
Describes the module structure and organization. Includes:
- Component diagram from package structure
- Backend packages (controller, service, repository, entity, security, config)
- Frontend directories (components, pages, services, hooks, context)
- Component interfaces and dependencies
- Module integration patterns (dependency injection, repository pattern, event-driven)
- External dependencies (Spring Boot, React, PostgreSQL, PostGIS, JWT, etc.)
- Component responsibilities and boundaries

**Key Sections**:
- [Package Structure](07-implementation-view.md#package-structure)
- [Component Diagram](07-implementation-view.md#component-diagram)
- [Integration Patterns](07-implementation-view.md#integration-patterns)
- [External Dependencies](07-implementation-view.md#external-dependencies)

### 8. [Design Decisions](08-design-decisions.md)
Documents key architectural and technology decisions. Includes:
- Design patterns used (Repository, MVC, Event-Driven, State Machine, Strategy)
- Technology stack rationale (Spring Boot, React, PostgreSQL+PostGIS, JWT, Docker)
- Security architecture (JWT authentication, BCrypt, RBAC, token blacklisting)
- Data persistence architecture (JPA/Hibernate, Hibernate Spatial, Flyway)
- Architectural decision justifications

**Key Sections**:
- [Design Patterns](08-design-decisions.md#design-patterns)
- [Technology Choices](08-design-decisions.md#technology-choices)
- [Security Architecture](08-design-decisions.md#security-architecture)
- [Decision Rationale](08-design-decisions.md#decision-rationale)

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.2.2
- **Language**: Java 17
- **Database**: PostgreSQL 15 + PostGIS 3.3
- **Security**: Spring Security, JWT (jjwt 0.12.3), BCrypt
- **ORM**: JPA/Hibernate with Hibernate Spatial
- **Migrations**: Flyway

### Frontend
- **Framework**: React 18
- **Build Tool**: Vite
- **Maps**: Leaflet
- **HTTP Client**: Axios
- **State Management**: React Context

### Deployment
- **Containerization**: Docker
- **Orchestration**: Docker Compose

## Documentation Standards

This documentation follows these standards:

- **UML Compliance**: All diagrams use standard UML notation and terminology
- **Code-Based Analysis**: All documented features are extracted from actual source code
- **Academic Quality**: Suitable for inclusion in a Master's Thesis (TFM)
- **Markdown Format**: All documentation in Markdown with Mermaid diagrams
- **Cross-Referenced**: Related sections are linked for easy navigation
- **Accuracy First**: Only documents verified functionality, marks ambiguous items as "implementation-dependent"

## How to Use This Documentation

### Quick Start Paths

#### For System Overview (15 minutes)
1. Read [Executive Summary](00-executive-summary.md)
2. Review [Use Case View - Actor Identification](01-use-case-view.md#actor-identification)
3. Review [MVC Architecture View](04-mvc-view.md) for high-level structure
4. Review [Deployment View - Deployment Diagram](06-deployment-view.md#deployment-diagram)

#### For Development (1-2 hours)
1. Review [Logical View - Sequence Diagrams](02-logical-view.md#sequence-diagrams) for component interactions
2. Review [Data Model View - Entity Catalog](03-data-model-view.md#entity-catalog) for database schema
3. Review [Implementation View - Package Structure](07-implementation-view.md#package-structure) for code organization
4. Review [Design Decisions](08-design-decisions.md) for patterns and rationale

#### For Deployment (30 minutes)
1. Review [Deployment View](06-deployment-view.md) for infrastructure
2. Review [Design Decisions - Technology Choices](08-design-decisions.md#technology-choices) for technology rationale
3. Review [Implementation View - External Dependencies](07-implementation-view.md#external-dependencies)

#### For Architecture Review (3-4 hours)
1. Read [Executive Summary](00-executive-summary.md)
2. Read all views in sequence (01 through 08)
3. Focus on diagrams for visual understanding
4. Review source code references for verification
5. Cross-reference related sections using provided links

#### For Thesis/Academic Use
All documentation is written in formal academic language and follows UML standards, making it suitable for direct inclusion in a Master's Thesis (TFM). Each section includes:
- Formal terminology and notation
- Source code references for verification
- Comprehensive diagrams with legends
- Cross-references between related concepts

## Diagram Notation

All diagrams in this documentation use standard UML notation:

- **Sequence Diagrams**: Show interaction flow between components over time
- **Class Diagrams**: Show static structure with classes, attributes, and relationships
- **State Diagrams**: Show state transitions for entities with state machines
- **Component Diagrams**: Show module structure and dependencies
- **Deployment Diagrams**: Show physical/logical deployment topology
- **Activity Diagrams**: Show workflow and business process flow

Diagrams are rendered using Mermaid syntax for easy integration and version control.

## Source Code References

All documented features include references to source files in the codebase:

- **Backend**: `backend/src/main/java/com/urbanclean/`
- **Frontend**: `frontend/src/`
- **Configuration**: `docker/`, `backend/pom.xml`, `frontend/package.json`

## Maintenance

This documentation should be updated when:

- New features are added to the system
- Architectural patterns change
- Technology stack is upgraded
- Deployment topology changes
- Major refactoring occurs

## Contact

For questions about this documentation or the system architecture, please refer to the project repository or contact the development team.

---

**Document Version**: 1.0  
**Last Updated**: February 2026  
**Generated From**: Urban Cleaning Management System codebase analysis
