# Design Document: AWS Deployment with GitHub Actions (Cost-Optimized)

## Overview

Este documento proporciona el diseño técnico detallado para el deployment **ECONÓMICO** del Urban Cleaning Management System en AWS. El diseño prioriza **minimizar costos** manteniendo funcionalidad esencial.

**Principios de Diseño (Cost-Optimized)**:
- **Cost First**: Minimizar costos es la prioridad #1
- **Single AZ**: Usar 1 AZ en lugar de 3 (ahorro ~60%)
- **Smallest Instances**: Usar instancias mínimas viables
- **No CDN**: Eliminar CloudFront (ahorro $50-100/mes)
- **Simplified Architecture**: Reducir servicios gestionados
- **Manual Scaling**: Evitar auto-scaling para control de costos
- **Free Tier**: Maximizar uso de AWS Free Tier

**Estimación de Costos Mensuales**: $50-150/mes (vs $500-1000/mes arquitectura completa)

---

## Architecture Overview - COST-OPTIMIZED

### High-Level Architecture Diagram (Económica)

```
┌─────────────────────────────────────────────────────────────────────┐
│                          INTERNET                                    │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
                    ┌────────────────┐
                    │   Route 53     │ (DNS - OPCIONAL, puede usar IP pública)
                    └────────┬───────┘
                             │
                             ▼
┌────────────────────────────────────────────────────────────────────┐
│                    AWS REGION (us-east-1)                           │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │              VPC (10.0.0.0/16) - SINGLE AZ                    │  │
│  │                                                                │  │
│  │  ┌─────────────────────────────────────────────────────────┐ │  │
│  │  │         PUBLIC SUBNET (1 AZ: us-east-1a)                 │ │  │
│  │  │              10.0.1.0/24                                  │ │  │
│  │  │                                                           │ │  │
│  │  │  ┌──────────────────────────────────────────────────┐   │ │  │
│  │  │  │   EC2 t3.small (2 vCPU, 2GB RAM)                 │   │ │  │
│  │  │  │   - Docker Compose (Backend + Frontend)          │   │ │  │
│  │  │  │   - Nginx Reverse Proxy                          │   │ │  │
│  │  │  │   - Let's Encrypt SSL (gratis)                   │   │ │  │
│  │  │  │   - Elastic IP (gratis si está attached)         │   │ │  │
│  │  │  └──────────────────────────────────────────────────┘   │ │  │
│  │  └─────────────────────────────────────────────────────────┘ │  │
│  │                                                                │  │
│  │  ┌─────────────────────────────────────────────────────────┐ │  │
│  │  │         PRIVATE SUBNET (1 AZ: us-east-1a)                │ │  │
│  │  │              10.0.11.0/24                                 │ │  │
│  │  │                                                           │ │  │
│  │  │  ┌──────────────────────────────────────────────────┐   │ │  │
│  │  │  │   RDS PostgreSQL 15 + PostGIS                    │   │ │  │
│  │  │  │   - db.t3.micro (2 vCPU, 1GB RAM)                │   │ │  │
│  │  │  │   - Single-AZ (NO Multi-AZ)                      │   │ │  │
│  │  │  │   - 20 GB gp3 storage                            │   │ │  │
│  │  │  │   - Automated Backups (7 days)                   │   │ │  │
│  │  │  │   - NO encryption (ahorro KMS)                   │   │ │  │
│  │  │  └──────────────────────────────────────────────────┘   │ │  │
│  │  └─────────────────────────────────────────────────────────┘ │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │              SUPPORTING SERVICES (Mínimos)                    │  │
│  │                                                                │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │  │
│  │  │     S3       │  │     SES      │  │     ECR      │       │  │
│  │  │  (Uploads)   │  │   (Email)    │  │  (Images)    │       │  │
│  │  │  Free Tier   │  │  Free Tier   │  │  Free Tier   │       │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘       │  │
│  │                                                                │  │
│  │  ┌──────────────┐                                             │  │
│  │  │ CloudWatch   │  (Logs básicos, Free Tier)                 │  │
│  │  │  (Básico)    │                                             │  │
│  │  └──────────────┘                                             │  │
│  └──────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────┘

ELIMINADO para ahorrar costos:
❌ CloudFront CDN ($50-100/mes)
❌ Application Load Balancer ($16/mes + data transfer)
❌ ECS Fargate ($30-50/mes por task)
❌ NAT Gateway ($32/mes)
❌ Multi-AZ deployment (60% ahorro)
❌ Secrets Manager ($0.40/secret/mes)
❌ WAF ($5/mes + rules)
❌ Enhanced Monitoring
```

---

## Desglose de Costos Mensuales (Estimado)

### Arquitectura ECONÓMICA (Recomendada para MVP/Startup)

| Servicio | Especificación | Costo Mensual | Notas |
|----------|----------------|---------------|-------|
| **EC2 t3.small** | 2 vCPU, 2GB RAM, Single-AZ | $15.18 | On-Demand, puede usar Reserved Instance ($10/mes) |
| **RDS t3.micro** | 2 vCPU, 1GB RAM, 20GB storage | $13.14 | Single-AZ, puede usar Reserved Instance ($8/mes) |
| **EBS Volume** | 30 GB gp3 para EC2 | $2.40 | Para sistema operativo y Docker |
| **Elastic IP** | 1 IP estática | $0.00 | Gratis si está attached a EC2 running |
| **S3 Standard** | 10 GB storage + requests | $0.23 | Para uploads de fotos |
| **SES** | 1,000 emails/mes | $0.00 | Free Tier: primeros 62,000 emails gratis |
| **ECR** | 500 MB images | $0.05 | Para Docker images |
| **CloudWatch Logs** | 5 GB ingestion | $0.00 | Free Tier: primeros 5 GB gratis |
| **Data Transfer** | 10 GB outbound | $0.90 | Primeros 100 GB: $0.09/GB |
| **Backups** | RDS automated backups | $0.00 | Incluido en RDS |
| **TOTAL MENSUAL** | | **~$32/mes** | **Con Reserved Instances: ~$22/mes** |

### Comparación con Arquitectura Completa

| Componente | Arquitectura Completa | Arquitectura Económica | Ahorro |
|------------|----------------------|------------------------|--------|
| Compute | ECS Fargate (2 tasks) = $50 | EC2 t3.small = $15 | **$35** |
| Database | RDS Multi-AZ t3.medium = $120 | RDS Single-AZ t3.micro = $13 | **$107** |
| Load Balancer | ALB = $16 + data | Nginx en EC2 = $0 | **$16** |
| CDN | CloudFront = $50 | Ninguno = $0 | **$50** |
| NAT Gateway | $32 | Ninguno (EC2 en public subnet) = $0 | **$32** |
| Secrets Manager | $2 | Variables de entorno = $0 | **$2** |
| **TOTAL** | **~$270/mes** | **~$32/mes** | **$238/mes (88% ahorro)** |

---

## Componentes Detallados - Arquitectura Económica

### 1. EC2 Instance (t3.small)

**Especificaciones**:
- **Tipo**: t3.small (2 vCPU, 2 GB RAM)
- **Sistema Operativo**: Amazon Linux 2023 (gratis)
- **Storage**: 30 GB gp3 EBS ($2.40/mes)
- **Networking**: Elastic IP (gratis si attached)
- **Ubicación**: Public subnet (acceso directo a internet, no NAT Gateway)

**Software Instalado**:
```bash
# Docker + Docker Compose
sudo yum install -y docker
sudo systemctl start docker
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Nginx (reverse proxy)
sudo yum install -y nginx

# Certbot (Let's Encrypt SSL gratis)
sudo yum install -y certbot python3-certbot-nginx
```

**Docker Compose Setup**:
```yaml
version: '3.8'
services:
  backend:
    image: <account-id>.dkr.ecr.us-east-1.amazonaws.com/urbanclean-backend:latest
    ports:
      - "8080:8080"
    environment:
      - DB_HOST=${DB_HOST}
      - DB_PASSWORD=${DB_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
    restart: always
    mem_limit: 1g
    cpus: 1.0

  frontend:
    image: <account-id>.dkr.ecr.us-east-1.amazonaws.com/urbanclean-frontend:latest
    ports:
      - "3000:80"
    restart: always
    mem_limit: 512m
    cpus: 0.5
```

**Nginx Configuration**:
```nginx
server {
    listen 80;
    server_name urbanclean.example.com;
    
    # Redirect HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name urbanclean.example.com;
    
    # Let's Encrypt SSL (gratis)
    ssl_certificate /etc/letsencrypt/live/urbanclean.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/urbanclean.example.com/privkey.pem;
    
    # Backend API
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    # Frontend
    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
    }
}
```

**Ventajas**:
- ✅ Costo muy bajo ($15/mes)
- ✅ Control total del servidor
- ✅ Fácil debugging (SSH access)
- ✅ No vendor lock-in

**Desventajas**:
- ❌ Single point of failure
- ❌ Requiere gestión manual de servidor
- ❌ No auto-scaling automático
- ❌ Downtime durante deployments

---

### 2. RDS PostgreSQL (t3.micro)

**Especificaciones**:
- **Tipo**: db.t3.micro (2 vCPU, 1 GB RAM)
- **Engine**: PostgreSQL 15
- **Storage**: 20 GB gp3 (auto-scaling deshabilitado)
- **Deployment**: Single-AZ (NO Multi-AZ)
- **Backups**: Automated daily backups (7 days retention)
- **Encryption**: Deshabilitado (ahorro en KMS)

**Configuración**:
```properties
# Parameter Group optimizado para t3.micro
max_connections = 100
shared_buffers = 256MB
effective_cache_size = 768MB
maintenance_work_mem = 64MB
work_mem = 2MB
```

**Ventajas**:
- ✅ Muy económico ($13/mes)
- ✅ Backups automáticos incluidos
- ✅ PostGIS soportado
- ✅ Managed service (menos overhead)

**Desventajas**:
- ❌ Performance limitado (1 GB RAM)
- ❌ No Multi-AZ (sin failover automático)
- ❌ No encryption at rest

**Upgrade Path**:
- Cuando el tráfico crezca: t3.small ($26/mes) o t3.medium ($52/mes)
- Para producción seria: habilitar Multi-AZ (+100% costo)

---

### 3. S3 para File Uploads

**Configuración**:
- **Storage Class**: S3 Standard
- **Lifecycle Policy**: Mover a S3 Glacier después de 90 días
- **Versioning**: Deshabilitado (ahorro de storage)
- **Encryption**: SSE-S3 (gratis, no KMS)

**Estimación de Costos**:
```
Storage: 10 GB × $0.023/GB = $0.23/mes
PUT requests: 1,000 × $0.005/1000 = $0.005/mes
GET requests: 10,000 × $0.0004/1000 = $0.004/mes
TOTAL: ~$0.24/mes
```

---

### 4. AWS SES para Emails

**Free Tier**:
- Primeros 62,000 emails/mes: **GRATIS**
- Después: $0.10 por 1,000 emails

**Configuración**:
- Verificar dominio (SPF, DKIM, DMARC)
- Usar SMTP endpoint: `email-smtp.us-east-1.amazonaws.com`
- Configurar bounce/complaint handling

---

### 5. ECR para Docker Images

**Free Tier**:
- Primeros 500 MB: **GRATIS**
- Después: $0.10/GB/mes

**Lifecycle Policy**:
```json
{
  "rules": [{
    "rulePriority": 1,
    "description": "Keep last 5 images",
    "selection": {
      "tagStatus": "any",
      "countType": "imageCountMoreThan",
      "countNumber": 5
    },
    "action": {
      "type": "expire"
    }
  }]
}
```

---

### 6. CloudWatch Logs (Básico)

**Free Tier**:
- Primeros 5 GB ingestion: **GRATIS**
- Primeros 5 GB storage: **GRATIS**

**Configuración**:
- Retención: 7 días (mínimo)
- Log level: INFO (no DEBUG en producción)
- Filtrar logs innecesarios

---

## CI/CD Pipeline - Simplificado

### GitHub Actions Workflow (Económico)

```yaml
name: Deploy to EC2

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      # Build Docker images
      - name: Build images
        run: |
          docker build -t urbanclean-backend ./backend
          docker build -t urbanclean-frontend ./frontend
      
      # Push to ECR
      - name: Push to ECR
        run: |
          aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_REGISTRY
          docker tag urbanclean-backend:latest $ECR_REGISTRY/urbanclean-backend:latest
          docker push $ECR_REGISTRY/urbanclean-backend:latest
      
      # Deploy to EC2 via SSH
      - name: Deploy to EC2
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ec2-user
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            # Pull new images
            aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_REGISTRY
            docker-compose pull
            
            # Restart containers (brief downtime)
            docker-compose down
            docker-compose up -d
            
            # Health check
            sleep 10
            curl -f http://localhost:8080/actuator/health || exit 1
```

**Ventajas**:
- ✅ Simple y directo
- ✅ No costos adicionales de CI/CD
- ✅ Fácil de entender y mantener

**Desventajas**:
- ❌ Downtime durante deployment (~30 segundos)
- ❌ No blue/green deployment
- ❌ Rollback manual

---

## Monitoreo - Básico

### CloudWatch Alarms (Gratis con Free Tier)

```yaml
Alarms:
  # EC2 CPU
  - AlarmName: EC2-High-CPU
    MetricName: CPUUtilization
    Threshold: 80
    EvaluationPeriods: 2
    
  # RDS CPU
  - AlarmName: RDS-High-CPU
    MetricName: CPUUtilization
    Threshold: 80
    EvaluationPeriods: 2
    
  # RDS Connections
  - AlarmName: RDS-High-Connections
    MetricName: DatabaseConnections
    Threshold: 80
    EvaluationPeriods: 2
```

**Notificaciones**:
- SNS Topic → Email (gratis)
- Opcional: Slack webhook (gratis)

---

## Upgrade Path - Cuando Crecer

### Fase 1: MVP (Actual) - $32/mes
- EC2 t3.small + RDS t3.micro
- Single-AZ, sin redundancia
- Soporta: 100-500 usuarios concurrentes

### Fase 2: Growth - $100/mes
- EC2 t3.medium + RDS t3.small
- Agregar Application Load Balancer
- 2 EC2 instances para redundancia
- Soporta: 500-2,000 usuarios concurrentes

### Fase 3: Scale - $300/mes
- Migrar a ECS Fargate
- RDS Multi-AZ t3.medium
- CloudFront CDN
- Auto-scaling habilitado
- Soporta: 2,000-10,000 usuarios concurrentes

---

## Recomendaciones Finales

### Para Minimizar Costos:

1. **Usar Reserved Instances** (1 año commitment):
   - EC2 t3.small: $15 → $10/mes (33% ahorro)
   - RDS t3.micro: $13 → $8/mes (38% ahorro)
   - **Ahorro total: $10/mes**

2. **Apagar en horarios no productivos** (dev/staging):
   - Usar Lambda para stop/start automático
   - Ahorro: 50% en ambientes no productivos

3. **Maximizar Free Tier**:
   - SES: 62,000 emails/mes gratis
   - CloudWatch: 5 GB logs gratis
   - S3: Primeros 5 GB gratis
   - ECR: Primeros 500 MB gratis

4. **Monitorear costos**:
   - AWS Cost Explorer
   - Billing alerts ($50 threshold)
   - Tag all resources

### Costo Total Estimado:

| Escenario | Costo Mensual |
|-----------|---------------|
| **On-Demand** | $32/mes |
| **Reserved Instances (1 año)** | $22/mes |
| **Reserved Instances + Optimizaciones** | $18/mes |

---

