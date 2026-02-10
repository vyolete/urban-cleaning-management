# Executive Summary - Urban Cleaning Management System Architecture

## Table of Contents

1. [Document Purpose](#document-purpose)
2. [System Overview](#system-overview)
3. [Technology Stack Summary](#technology-stack-summary)
4. [Architectural Highlights](#architectural-highlights)
5. [System Actors](#system-actors)
6. [Key Features](#key-features)
7. [Architectural Views](#architectural-views)
8. [Design Patterns](#design-patterns)
9. [Quality Attributes](#quality-attributes)
10. [Deployment Architecture](#deployment-architecture)
11. [Data Flow](#data-flow)
12. [Integration Points](#integration-points)
13. [Documentation Standards](#documentation-standards)
14. [How to Navigate This Documentation](#how-to-navigate-this-documentation)
15. [Document Status](#document-status)

---

## Document Purpose

This document provides a high-level overview of the Urban Cleaning Management System architecture. It serves as an entry point to the comprehensive architectural documentation and is suitable for stakeholders, technical reviewers, and thesis evaluators.

## System Overview

The Urban Cleaning Management System is a full-stack web application designed to manage urban cleaning operations through citizen reporting, operator task management, and administrative oversight. The system enables citizens to report urban cleaning issues with geolocation, operators to manage and resolve tasks, and administrators to configure system parameters and monitor performance.

## Technology Stack Summary

### Backend
- **Framework**: Spring Boot 3.2.2 (Java 17)
- **Database**: PostgreSQL 15 + PostGIS 3.3
- **Security**: Spring Security, JWT authentication, BCrypt password hashing
- **ORM**: JPA/Hibernate with Hibernate Spatial
- **Migrations**: Flyway

### Frontend
- **Framework**: React 18
- **Build Tool**: Vite
- **Maps**: Leaflet for geospatial visualization
- **HTTP Client**: Axios
- **State Management**: React Context

### Deployment
- **Containerization**: Docker
- **Orchestration**: Docker Compose
- **Architecture**: Three-tier (Frontend, Backend, Database)

## Architectural Highlights

### 1. Clean Architecture
The system follows clean architecture principles with clear separation between:
- **Presentation Layer**: REST controllers and React components
- **Business Layer**: Service classes with business logic
- **Data Layer**: JPA repositories and entities
- **Infrastructure Layer**: Security, configuration, and cross-cutting concerns

### 2. Geospatial Capabilities
PostGIS integration enables:
- Coordinate-based report submission
- Spatial queries for proximity detection
- Duplicate detection based on location
- Map visualization of reports and tasks
- Geofencing validation

### 3. Security-First Design
Comprehensive security implementation:
- JWT-based stateless authentication
- Role-based access control (RBAC)
- BCrypt password hashing
- Token blacklisting for logout
- Session management with device fingerprinting
- Rate limiting and security monitoring

### 4. Event-Driven Architecture
Domain events enable:
- Asynchronous notification delivery
- Decoupled component communication
- Audit trail generation
- Extensible workflow processing

### 5. Priority-Based Task Management
Configurable priority calculation algorithm:
- Category-based weighting
- Zone risk index
- Time elapsed since report
- Admin-configurable weights
- Automatic recalculation on configuration changes

## System Actors

1. **Citizen (CIUDADANO)**: Submit reports, track status, provide feedback
2. **Operator (TECNICO)**: View assigned tasks, update task status, upload evidence
3. **Administrator (ADMIN)**: Configure system parameters, manage users, view analytics

## Key Features

### For Citizens
- Report submission with photo upload
- Geolocation-based reporting
- Report status tracking
- Feedback submission

### For Operators
- Task list with priority sorting
- Map view of assigned tasks
- Task state management (assign, start, resolve, reopen)
- Evidence upload
- Audit trail viewing

### For Administrators
- Algorithm weight configuration
- User management
- Analytics and reporting
- System monitoring
- Duplicate detection management

## Architectural Views

This documentation is organized into seven architectural views following the 4+1 architectural view model:

1. **[Use Case View](01-use-case-view.md)**: Functional capabilities from actor perspective
2. **[Logical View](02-logical-view.md)**: Internal structure and component interactions
3. **[Data Model View](03-data-model-view.md)**: Persistent data structure and relationships
4. **[MVC Architecture View](04-mvc-view.md)**: Model-View-Controller pattern implementation
5. **[Process View](05-process-view.md)**: Runtime behavior and business processes
6. **[Deployment View](06-deployment-view.md)**: Physical deployment architecture
7. **[Implementation View](07-implementation-view.md)**: Module structure and organization
8. **[Design Decisions](08-design-decisions.md)**: Technology choices and rationales

## Design Patterns

The system employs several well-established design patterns:

- **Repository Pattern**: Data access abstraction via Spring Data JPA
- **MVC Pattern**: Clear separation of presentation, business, and data layers
- **Event-Driven Pattern**: Asynchronous processing via domain events
- **State Machine Pattern**: Task state transitions with validation
- **Strategy Pattern**: Configurable priority calculation algorithm
- **Dependency Injection**: Loose coupling via Spring's IoC container

## Quality Attributes

### Scalability
- Stateless backend (horizontal scaling)
- JWT authentication (no server-side sessions)
- Asynchronous processing for notifications
- Database connection pooling

### Security
- Industry-standard authentication (JWT)
- Strong password hashing (BCrypt)
- Role-based access control
- Token blacklisting
- Session management
- Rate limiting

### Maintainability
- Clean architecture with separation of concerns
- Comprehensive test coverage
- Database migrations for schema versioning
- Docker for consistent environments
- Clear package structure

### Performance
- Spatial indexes for geolocation queries
- Database indexes on frequently queried columns
- Asynchronous notification delivery
- Caching for configuration data
- Optimized SQL queries

### Reliability
- ACID transactions via PostgreSQL
- Transaction management via Spring
- Error handling and validation
- Audit logging
- Health checks and monitoring

## Deployment Architecture

The system is deployed as three Docker containers:

1. **PostgreSQL Container**: Database with PostGIS extension
2. **Backend Container**: Spring Boot application (JAR)
3. **Frontend Container**: Nginx serving React build

Containers communicate via Docker network with defined dependencies:
- Frontend depends on Backend
- Backend depends on PostgreSQL

## Data Flow

### Report Submission Flow
```
Citizen (Browser)
    ↓ HTTP POST /api/reports
Frontend (React)
    ↓ Axios API call
Backend (Spring Boot)
    ↓ ReportController → ReportService
    ↓ DeduplicationService (check duplicates)
    ↓ TaskService (create task)
    ↓ PriorityCalculatorService (calculate priority)
    ↓ EventPublisher (publish TaskCreatedEvent)
Database (PostgreSQL + PostGIS)
    ↓ Persist report and task
Notification Service (Async)
    ↓ Send email notifications
```

## Integration Points

### External Integrations
- **Email Service**: SMTP for notifications and password reset
- **File Storage**: Local filesystem for photo uploads
- **Geolocation API**: Browser Geolocation API

### Internal Integrations
- **REST API**: Frontend-backend communication
- **JDBC**: Backend-database communication
- **Event Bus**: Internal component communication

## Documentation Standards

This documentation adheres to:
- **UML Standards**: All diagrams use standard UML notation
- **Academic Quality**: Suitable for Master's Thesis (TFM) inclusion
- **Code-Based Analysis**: All features extracted from actual source code
- **Markdown Format**: Easy integration and version control
- **Cross-Referenced**: Related sections are linked
- **Accuracy First**: Only verified functionality documented

## How to Navigate This Documentation

### For System Overview
1. Read this Executive Summary
2. Review [Use Case View](01-use-case-view.md) for functional capabilities
3. Review [MVC Architecture View](04-mvc-view.md) for high-level structure

### For Development
1. Review [Logical View](02-logical-view.md) for component interactions
2. Review [Data Model View](03-data-model-view.md) for database schema
3. Review [Implementation View](07-implementation-view.md) for package structure

### For Deployment
1. Review [Deployment View](06-deployment-view.md) for infrastructure
2. Review [Design Decisions](08-design-decisions.md) for technology rationale

### For Architecture Review
1. Read all views in sequence (01 through 08)
2. Focus on diagrams for visual understanding
3. Review source code references for verification

## Document Status

**Version**: 1.0  
**Status**: Complete  
**Last Updated**: February 2026  
**Analysis Completed**: All seven architectural views documented based on codebase analysis

This documentation has been generated through systematic analysis of the Urban Cleaning Management System codebase, extracting architectural patterns, component relationships, and system behaviors directly from source code.

## Contact and Maintenance

This documentation should be updated when:
- New features are added
- Architectural patterns change
- Technology stack is upgraded
- Deployment topology changes
- Major refactoring occurs

For questions about this documentation, refer to the project repository or contact the development team.

---

**Note**: All information in this documentation is derived from actual source code analysis. Features are documented only when verified in the codebase, ensuring accuracy and reliability for academic and technical evaluation.
