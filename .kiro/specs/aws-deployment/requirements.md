# Requirements Document: AWS Deployment with GitHub Actions

## Introduction

Este documento especifica los requisitos para el deployment del Urban Cleaning Management System en AWS utilizando GitHub Actions como plataforma de CI/CD. El sistema está 100% completo y listo para producción según el PRODUCTION_READINESS_SUMMARY.md.

**Estado Actual del Sistema**:
- ✅ 119/119 tareas completadas (100%)
- ✅ 6/6 integration tests passing
- ✅ Security Score: 9.8/10 (APPROVED)
- ✅ Configuration Score: 9.2/10 (APPROVED)
- ✅ Load Tests: 43,700+ requests, 0% error rate
- ✅ OWASP Top 10: 100% compliant

**Objetivo**: Desplegar el sistema en AWS con alta disponibilidad, seguridad y observabilidad siguiendo las mejores prácticas DevOps/SRE.

## Glossary

- **ECS**: Elastic Container Service - servicio de orquestación de contenedores
- **Fargate**: Modo serverless de ECS (sin gestión de EC2)
- **RDS**: Relational Database Service - base de datos PostgreSQL gestionada
- **ALB**: Application Load Balancer - balanceador de carga capa 7
- **CloudFront**: CDN global de AWS
- **S3**: Simple Storage Service - almacenamiento de objetos
- **Secrets Manager**: Servicio de gestión de secretos
- **CloudWatch**: Servicio de monitoreo y logs
- **SES**: Simple Email Service - servicio de correo electrónico
- **Blue/Green Deployment**: Estrategia de deployment sin downtime
- **IaC**: Infrastructure as Code - infraestructura como código

---

## MODULE 1: INFRASTRUCTURE SETUP (AWS)

### Requirement 1: Network Architecture

**User Story:** Como DevOps Engineer, quiero una arquitectura de red segura y escalable, para garantizar alta disponibilidad y aislamiento de recursos.

#### Acceptance Criteria

1. THE System SHALL deploy in a VPC with CIDR 10.0.0.0/16
2. THE System SHALL use 3 Availability Zones for high availability
3. THE System SHALL create public subnets (10.0.1.0/24, 10.0.2.0/24, 10.0.3.0/24) for ALB
4. THE System SHALL create private subnets (10.0.11.0/24, 10.0.12.0/24, 10.0.13.0/24) for ECS tasks
5. THE System SHALL create database subnets (10.0.21.0/24, 10.0.22.0/24, 10.0.23.0/24) for RDS
6. THE System SHALL configure NAT Gateways in each AZ for outbound internet access
7. THE System SHALL configure Internet Gateway for inbound traffic to ALB
8. THE System SHALL use Security Groups to restrict traffic between layers
9. THE System SHALL enable VPC Flow Logs for network monitoring
10. THE System SHALL tag all resources with Environment, Project, and ManagedBy tags

---

### Requirement 2: Database Infrastructure (RDS PostgreSQL + PostGIS)

**User Story:** Como DBA, quiero una base de datos PostgreSQL gestionada con PostGIS, para garantizar alta disponibilidad y backups automáticos.

#### Acceptance Criteria

1. THE System SHALL deploy RDS PostgreSQL 15 with PostGIS 3.3 extension
2. THE System SHALL use Multi-AZ deployment for automatic failover
3. THE System SHALL use db.t3.medium instance type (2 vCPU, 4 GB RAM) for production
4. THE System SHALL allocate 100 GB of gp3 storage with auto-scaling up to 500 GB
5. THE System SHALL enable automated backups with 7-day retention
6. THE System SHALL create daily snapshots at 03:00 UTC
7. THE System SHALL enable encryption at rest using AWS KMS
8. THE System SHALL enable encryption in transit (SSL/TLS required)
9. THE System SHALL configure parameter group with optimized PostGIS settings
10. THE System SHALL place RDS in private database subnets (no public access)
11. THE System SHALL configure Security Group to allow traffic only from ECS tasks
12. THE System SHALL enable Performance Insights for query monitoring
13. THE System SHALL enable Enhanced Monitoring with 60-second granularity
14. THE System SHALL configure CloudWatch alarms for CPU, memory, and connections

---

### Requirement 3: Container Orchestration (ECS Fargate)

**User Story:** Como DevOps Engineer, quiero desplegar contenedores sin gestionar servidores, para reducir overhead operacional.

#### Acceptance Criteria

1. THE System SHALL create an ECS Cluster named "urbanclean-prod-cluster"
2. THE System SHALL use Fargate launch type (serverless, no EC2 management)
3. THE System SHALL create ECS Task Definition for backend service
4. THE System SHALL allocate 1 vCPU and 2 GB RAM per backend task
5. THE System SHALL create ECS Task Definition for frontend service
6. THE System SHALL allocate 0.5 vCPU and 1 GB RAM per frontend task
7. THE System SHALL configure ECS Service with desired count of 2 tasks (backend)
8. THE System SHALL configure ECS Service with desired count of 2 tasks (frontend)
9. THE System SHALL enable auto-scaling based on CPU (target 70%) and memory (target 80%)
10. THE System SHALL configure health checks for each service
11. THE System SHALL use Application Load Balancer for traffic distribution
12. THE System SHALL enable ECS Exec for debugging (disabled in production)
13. THE System SHALL configure task IAM roles with least privilege
14. THE System SHALL enable Container Insights for monitoring
15. THE System SHALL configure log routing to CloudWatch Logs

---

### Requirement 4: Load Balancing (Application Load Balancer)

**User Story:** Como SRE, quiero un balanceador de carga que distribuya tráfico y maneje SSL/TLS, para garantizar alta disponibilidad y seguridad.

#### Acceptance Criteria

1. THE System SHALL create an Application Load Balancer in public subnets
2. THE System SHALL configure HTTPS listener on port 443 with SSL certificate
3. THE System SHALL redirect HTTP (port 80) traffic to HTTPS
4. THE System SHALL create target group for backend service (port 8080)
5. THE System SHALL create target group for frontend service (port 80)
6. THE System SHALL configure health check path /actuator/health for backend
7. THE System SHALL configure health check path / for frontend
8. THE System SHALL set health check interval to 30 seconds
9. THE System SHALL set unhealthy threshold to 2 consecutive failures
10. THE System SHALL set healthy threshold to 2 consecutive successes
11. THE System SHALL enable access logs to S3 bucket
12. THE System SHALL configure routing rules: /api/* → backend, /* → frontend
13. THE System SHALL enable connection draining with 300-second timeout
14. THE System SHALL configure sticky sessions for backend (cookie-based)
15. THE System SHALL enable WAF (Web Application Firewall) for DDoS protection

---

### Requirement 5: Content Delivery (CloudFront + S3)

**User Story:** Como usuario final, quiero acceso rápido a la aplicación desde cualquier ubicación, para mejorar la experiencia de usuario.

#### Acceptance Criteria

1. THE System SHALL create S3 bucket for static assets (frontend build)
2. THE System SHALL enable S3 versioning for rollback capability
3. THE System SHALL configure S3 bucket policy to allow CloudFront access only
4. THE System SHALL create CloudFront distribution with ALB as origin
5. THE System SHALL configure CloudFront to cache static assets (CSS, JS, images)
6. THE System SHALL set cache TTL: 1 hour for HTML, 1 day for CSS/JS, 7 days for images
7. THE System SHALL enable HTTPS only (redirect HTTP to HTTPS)
8. THE System SHALL use ACM certificate for custom domain
9. THE System SHALL configure custom error pages (404, 500)
10. THE System SHALL enable CloudFront access logs to S3
11. THE System SHALL configure geo-restriction if needed (optional)
12. THE System SHALL enable compression (gzip, brotli) for text assets

---

### Requirement 6: Secrets Management (AWS Secrets Manager)

**User Story:** Como Security Engineer, quiero gestionar secretos de forma segura, para evitar credenciales hardcodeadas.

#### Acceptance Criteria

1. THE System SHALL store database credentials in Secrets Manager
2. THE System SHALL store JWT secret in Secrets Manager
3. THE System SHALL store SMTP credentials (AWS SES) in Secrets Manager
4. THE System SHALL enable automatic rotation for database password (30 days)
5. THE System SHALL enable automatic rotation for JWT secret (90 days)
6. THE System SHALL configure IAM policies for ECS tasks to read secrets
7. THE System SHALL use secret ARNs in ECS task definitions (not plaintext)
8. THE System SHALL enable CloudWatch alarms for secret access failures
9. THE System SHALL audit all secret access via CloudTrail
10. THE System SHALL encrypt secrets using AWS KMS customer-managed key

---

### Requirement 7: File Storage (S3 for Uploads)

**User Story:** Como ciudadano, quiero subir fotos de reportes, para documentar incidencias.

#### Acceptance Criteria

1. THE System SHALL create S3 bucket for report photo uploads
2. THE System SHALL enable S3 versioning for file recovery
3. THE System SHALL configure lifecycle policy: delete files after 365 days
4. THE System SHALL enable S3 encryption at rest (SSE-S3)
5. THE System SHALL configure CORS policy to allow uploads from frontend
6. THE System SHALL implement pre-signed URLs for secure uploads (15-minute expiration)
7. THE System SHALL configure S3 bucket policy to deny public access
8. THE System SHALL enable S3 access logs to separate audit bucket
9. THE System SHALL configure CloudWatch metrics for bucket operations
10. THE System SHALL implement virus scanning using AWS GuardDuty or ClamAV

---

### Requirement 8: Email Service (AWS SES)

**User Story:** Como operador, quiero recibir notificaciones por email, para responder rápidamente a tareas asignadas.

#### Acceptance Criteria

1. THE System SHALL configure AWS SES in production mode (not sandbox)
2. THE System SHALL verify domain ownership (SPF, DKIM, DMARC records)
3. THE System SHALL configure SES SMTP credentials in Secrets Manager
4. THE System SHALL use SES endpoint: email-smtp.us-east-1.amazonaws.com
5. THE System SHALL configure bounce and complaint handling
6. THE System SHALL create SNS topics for bounce/complaint notifications
7. THE System SHALL monitor SES sending quota and request increase if needed
8. THE System SHALL configure SES configuration set for tracking
9. THE System SHALL enable SES event publishing to CloudWatch
10. THE System SHALL implement email rate limiting (14 emails/second default)

---

## MODULE 2: CI/CD PIPELINE (GitHub Actions)

### Requirement 9: Build Pipeline

**User Story:** Como Developer, quiero un pipeline automatizado que compile y pruebe el código, para detectar errores tempranamente.

#### Acceptance Criteria

1. THE System SHALL trigger build on push to main, develop, and feature/* branches
2. THE System SHALL trigger build on pull requests to main and develop
3. THE System SHALL use GitHub Actions workflow file: .github/workflows/build.yml
4. THE System SHALL checkout code using actions/checkout@v4
5. THE System SHALL set up Java 17 using actions/setup-java@v4
6. THE System SHALL cache Maven dependencies using actions/cache@v3
7. THE System SHALL run mvn clean verify (compile + test + integration tests)
8. THE System SHALL fail build if any test fails
9. THE System SHALL upload test reports as artifacts
10. THE System SHALL run security scan using Snyk or Trivy
11. THE System SHALL fail build if high/critical vulnerabilities found
12. THE System SHALL upload code coverage to Codecov (optional)
13. THE System SHALL send Slack notification on build failure
14. THE System SHALL complete build in < 10 minutes

---

### Requirement 10: Docker Image Build and Push

**User Story:** Como DevOps Engineer, quiero construir imágenes Docker optimizadas, para reducir tamaño y tiempo de deployment.

#### Acceptance Criteria

1. THE System SHALL build Docker images using multi-stage Dockerfile
2. THE System SHALL use Maven cache layer to speed up builds
3. THE System SHALL tag images with: latest, commit SHA, and semantic version
4. THE System SHALL push images to Amazon ECR (Elastic Container Registry)
5. THE System SHALL create ECR repositories: urbanclean-backend, urbanclean-frontend
6. THE System SHALL enable ECR image scanning on push
7. THE System SHALL configure ECR lifecycle policy: keep last 10 images
8. THE System SHALL authenticate to ECR using AWS credentials
9. THE System SHALL use GitHub OIDC provider for AWS authentication (no long-lived keys)
10. THE System SHALL fail pipeline if image scan finds critical vulnerabilities
11. THE System SHALL compress image layers to reduce size
12. THE System SHALL complete image build in < 5 minutes

---

### Requirement 11: Database Migration Pipeline

**User Story:** Como DBA, quiero ejecutar migraciones de base de datos de forma segura, para mantener el esquema actualizado.

#### Acceptance Criteria

1. THE System SHALL run Flyway migrations before deploying new application version
2. THE System SHALL connect to RDS using Secrets Manager credentials
3. THE System SHALL run migrations in a separate GitHub Actions job
4. THE System SHALL validate migrations in staging environment first
5. THE System SHALL create database backup before running migrations
6. THE System SHALL fail deployment if migrations fail
7. THE System SHALL log migration output to CloudWatch
8. THE System SHALL implement migration rollback procedure (manual)
9. THE System SHALL send notification on migration success/failure
10. THE System SHALL complete migrations in < 2 minutes

---

### Requirement 12: Deployment Pipeline (Blue/Green)

**User Story:** Como SRE, quiero deployments sin downtime, para mantener alta disponibilidad.

#### Acceptance Criteria

1. THE System SHALL use blue/green deployment strategy
2. THE System SHALL deploy to staging environment first (automatic)
3. THE System SHALL run smoke tests in staging
4. THE System SHALL require manual approval for production deployment
5. THE System SHALL deploy new ECS task definition to "green" environment
6. THE System SHALL wait for health checks to pass (5 minutes timeout)
7. THE System SHALL switch ALB target group to "green" environment
8. THE System SHALL monitor error rate for 10 minutes post-deployment
9. THE System SHALL automatically rollback if error rate > 1%
10. THE System SHALL keep "blue" environment running for 1 hour (manual rollback window)
11. THE System SHALL terminate "blue" environment after successful deployment
12. THE System SHALL send deployment notification to Slack
13. THE System SHALL tag deployment in GitHub with version number
14. THE System SHALL complete deployment in < 15 minutes

---

### Requirement 13: Rollback Strategy

**User Story:** Como SRE, quiero poder hacer rollback rápidamente, para recuperarme de deployments fallidos.

#### Acceptance Criteria

1. THE System SHALL support manual rollback via GitHub Actions workflow
2. THE System SHALL support automatic rollback on high error rate
3. THE System SHALL switch ALB target group back to "blue" environment
4. THE System SHALL keep previous 3 ECS task definitions for rollback
5. THE System SHALL keep previous 3 Docker images in ECR
6. THE System SHALL restore database from snapshot if needed (manual)
7. THE System SHALL send rollback notification to Slack
8. THE System SHALL log rollback reason and timestamp
9. THE System SHALL complete rollback in < 5 minutes
10. THE System SHALL create incident report for failed deployments

---

## MODULE 3: MONITORING & OBSERVABILITY

### Requirement 14: Application Monitoring (CloudWatch)

**User Story:** Como SRE, quiero monitorear la salud de la aplicación, para detectar problemas proactivamente.

#### Acceptance Criteria

1. THE System SHALL send application logs to CloudWatch Logs
2. THE System SHALL create log groups: /ecs/urbanclean-backend, /ecs/urbanclean-frontend
3. THE System SHALL configure log retention: 30 days
4. THE System SHALL enable CloudWatch Container Insights
5. THE System SHALL create CloudWatch dashboard with key metrics
6. THE System SHALL display metrics: CPU, memory, request count, error rate, response time
7. THE System SHALL create CloudWatch alarms for critical metrics
8. THE System SHALL send alarm notifications to SNS topic
9. THE System SHALL integrate SNS with Slack for real-time alerts
10. THE System SHALL enable X-Ray tracing for distributed tracing (optional)
11. THE System SHALL configure custom metrics from Spring Boot Actuator
12. THE System SHALL export Prometheus metrics to CloudWatch

---

### Requirement 15: Infrastructure Monitoring

**User Story:** Como SRE, quiero monitorear la infraestructura AWS, para garantizar disponibilidad y performance.

#### Acceptance Criteria

1. THE System SHALL monitor RDS metrics: CPU, memory, connections, IOPS
2. THE System SHALL monitor ECS metrics: CPU, memory, task count, deployment status
3. THE System SHALL monitor ALB metrics: request count, target response time, 5xx errors
4. THE System SHALL monitor CloudFront metrics: cache hit rate, error rate
5. THE System SHALL monitor S3 metrics: request count, 4xx/5xx errors
6. THE System SHALL create composite alarms for service health
7. THE System SHALL configure alarm actions: SNS notification, auto-scaling
8. THE System SHALL enable AWS Config for compliance monitoring
9. THE System SHALL enable AWS Trusted Advisor for cost optimization
10. THE System SHALL create monthly cost reports

---

### Requirement 16: Alerting Strategy

**User Story:** Como SRE, quiero recibir alertas relevantes, para responder a incidentes críticos.

#### Acceptance Criteria

1. THE System SHALL define alert severity levels: P1 (critical), P2 (high), P3 (medium), P4 (low)
2. THE System SHALL send P1 alerts to PagerDuty and Slack
3. THE System SHALL send P2-P4 alerts to Slack only
4. THE System SHALL configure alert thresholds based on SLOs
5. THE System SHALL alert on: error rate > 1%, response time > 2s, CPU > 80%, memory > 85%
6. THE System SHALL alert on: RDS connections > 90%, disk space > 80%
7. THE System SHALL alert on: deployment failures, health check failures
8. THE System SHALL implement alert deduplication (5-minute window)
9. THE System SHALL implement alert escalation (P2 → P1 after 30 minutes)
10. THE System SHALL create runbooks for common alerts

---

### Requirement 17: Logging Strategy

**User Story:** Como Developer, quiero logs estructurados y centralizados, para facilitar debugging.

#### Acceptance Criteria

1. THE System SHALL use JSON format for application logs
2. THE System SHALL include in logs: timestamp, level, logger, message, trace_id, user_id
3. THE System SHALL configure log levels: INFO for production, DEBUG for staging
4. THE System SHALL mask sensitive data in logs (passwords, tokens, PII)
5. THE System SHALL aggregate logs from all ECS tasks in CloudWatch
6. THE System SHALL enable CloudWatch Logs Insights for querying
7. THE System SHALL create saved queries for common investigations
8. THE System SHALL export logs to S3 for long-term retention (90 days)
9. THE System SHALL implement log sampling for high-volume endpoints (optional)
10. THE System SHALL integrate with external log analysis tool (optional: Datadog, New Relic)

---

## MODULE 4: SECURITY & COMPLIANCE

### Requirement 18: Security Best Practices

**User Story:** Como Security Engineer, quiero implementar security best practices, para proteger el sistema y los datos.

#### Acceptance Criteria

1. THE System SHALL enable AWS GuardDuty for threat detection
2. THE System SHALL enable AWS Security Hub for compliance monitoring
3. THE System SHALL enable AWS Config for resource compliance
4. THE System SHALL implement least privilege IAM policies
5. THE System SHALL enable MFA for AWS console access
6. THE System SHALL rotate IAM access keys every 90 days
7. THE System SHALL enable CloudTrail for audit logging
8. THE System SHALL encrypt all data at rest (RDS, S3, EBS)
9. THE System SHALL encrypt all data in transit (TLS 1.2+)
10. THE System SHALL implement WAF rules: rate limiting, SQL injection, XSS
11. THE System SHALL enable VPC Flow Logs for network monitoring
12. THE System SHALL implement DDoS protection using AWS Shield
13. THE System SHALL scan Docker images for vulnerabilities
14. THE System SHALL implement secrets rotation policy
15. THE System SHALL conduct quarterly security audits

---

### Requirement 19: Backup and Disaster Recovery

**User Story:** Como DBA, quiero backups automáticos y plan de disaster recovery, para garantizar continuidad del negocio.

#### Acceptance Criteria

1. THE System SHALL create automated RDS snapshots daily at 03:00 UTC
2. THE System SHALL retain RDS snapshots for 30 days
3. THE System SHALL copy RDS snapshots to secondary region (us-west-2)
4. THE System SHALL create S3 bucket replication to secondary region
5. THE System SHALL test database restore procedure monthly
6. THE System SHALL document RTO (Recovery Time Objective): 1 hour
7. THE System SHALL document RPO (Recovery Point Objective): 24 hours
8. THE System SHALL create disaster recovery runbook
9. THE System SHALL implement multi-region failover (optional)
10. THE System SHALL conduct disaster recovery drill quarterly

---

### Requirement 20: Compliance and Auditing

**User Story:** Como Compliance Officer, quiero auditar accesos y cambios, para cumplir con regulaciones.

#### Acceptance Criteria

1. THE System SHALL enable CloudTrail for all API calls
2. THE System SHALL enable CloudTrail log file validation
3. THE System SHALL store CloudTrail logs in S3 with encryption
4. THE System SHALL retain CloudTrail logs for 1 year
5. THE System SHALL enable AWS Config for resource change tracking
6. THE System SHALL create Config rules for compliance checks
7. THE System SHALL audit database access via RDS audit logs
8. THE System SHALL audit secrets access via CloudTrail
9. THE System SHALL implement GDPR compliance controls
10. THE System SHALL generate monthly compliance reports

---

## MODULE 5: COST OPTIMIZATION

### Requirement 21: Cost Management

**User Story:** Como FinOps Engineer, quiero optimizar costos de AWS, para maximizar ROI.

#### Acceptance Criteria

1. THE System SHALL use Fargate Spot for non-production environments (70% cost savings)
2. THE System SHALL use RDS Reserved Instances for production (40% cost savings)
3. THE System SHALL implement S3 lifecycle policies to move old data to Glacier
4. THE System SHALL enable S3 Intelligent-Tiering for automatic cost optimization
5. THE System SHALL use CloudFront to reduce data transfer costs
6. THE System SHALL implement auto-scaling to match demand
7. THE System SHALL schedule non-production environments (stop at night/weekends)
8. THE System SHALL enable AWS Cost Explorer for cost analysis
9. THE System SHALL create cost allocation tags for all resources
10. THE System SHALL set up billing alerts for budget overruns
11. THE System SHALL review AWS Trusted Advisor recommendations monthly
12. THE System SHALL implement cost optimization recommendations

---

## Non-Functional Requirements

### Performance Requirements

1. Application SHALL start in < 60 seconds
2. Deployment SHALL complete in < 15 minutes
3. Rollback SHALL complete in < 5 minutes
4. Database migrations SHALL complete in < 2 minutes
5. Health checks SHALL respond in < 1 second

### Availability Requirements

1. System SHALL achieve 99.9% uptime (SLA)
2. Deployments SHALL have zero downtime
3. Database SHALL have automatic failover (< 2 minutes)
4. System SHALL recover from AZ failure automatically
5. System SHALL support multi-region failover (optional)

### Scalability Requirements

1. System SHALL auto-scale from 2 to 10 ECS tasks based on load
2. System SHALL handle 10,000 concurrent users
3. System SHALL handle 100 requests/second sustained
4. System SHALL handle 500 requests/second peak
5. Database SHALL scale storage automatically up to 500 GB

### Security Requirements

1. All data SHALL be encrypted at rest and in transit
2. Secrets SHALL never be stored in code or logs
3. IAM policies SHALL follow least privilege principle
4. Security patches SHALL be applied within 7 days
5. Vulnerability scans SHALL run on every deployment

---

## Success Criteria

### Technical Success Criteria

1. ✅ All infrastructure deployed via IaC (Terraform or CloudFormation)
2. ✅ CI/CD pipeline fully automated (build, test, deploy)
3. ✅ Zero-downtime deployments working
4. ✅ Monitoring and alerting configured
5. ✅ Security best practices implemented
6. ✅ Backup and disaster recovery tested
7. ✅ Cost optimization measures in place

### Business Success Criteria

1. ✅ System available 99.9% of the time
2. ✅ Deployments complete in < 15 minutes
3. ✅ Incidents detected and alerted within 5 minutes
4. ✅ Mean Time To Recovery (MTTR) < 30 minutes
5. ✅ Monthly AWS costs within budget ($500-$1000/month estimated)

---

**Document Version**: 1.0  
**Last Updated**: 9 de febrero de 2026  
**Status**: Ready for Design Phase  
**Next Step**: Create design.md with detailed architecture diagrams and implementation plan
