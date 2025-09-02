# 배포 가이드

## 목차
1. [배포 개요](#배포-개요)
2. [환경 구성](#환경-구성)
3. [Docker 배포](#docker-배포)
4. [클라우드 배포](#클라우드-배포)
5. [CI/CD 파이프라인](#ci/cd-파이프라인)
6. [모니터링 및 로깅](#모니터링-및-로깅)
7. [백업 및 복구](#백업-및-복구)
8. [보안 설정](#보안-설정)
9. [성능 최적화](#성능-최적화)
10. [트러블슈팅](#트러블슈팅)

## 배포 개요

### 배포 아키텍처
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Load Balancer │    │   Web Firewall  │    │   API Gateway   │
│    (Nginx)      │────│     (WAF)       │────│                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
          │                        │                      │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Frontend CDN   │    │  Backend API    │    │  AI Service     │
│  (React Native) │    │ (Spring Boot)   │    │   (FastAPI)     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │                      │
                   ┌─────────────────┐    ┌─────────────────┐
                   │     MySQL       │    │     Redis       │
                   │   (Database)    │    │    (Cache)      │
                   └─────────────────┘    └─────────────────┘
```

### 배포 환경
- **개발 (Development)**: 로컬 개발자 환경
- **스테이징 (Staging)**: 운영 환경과 동일한 구성의 테스트 환경
- **운영 (Production)**: 실제 서비스 환경

## 환경 구성

### 시스템 요구사항

#### 최소 사양
- **CPU**: 4 Core
- **Memory**: 8GB RAM
- **Storage**: 100GB SSD
- **Network**: 1Gbps

#### 권장 사양
- **CPU**: 8 Core
- **Memory**: 16GB RAM
- **Storage**: 500GB SSD
- **Network**: 10Gbps

### 운영체제 및 소프트웨어
- **OS**: Ubuntu 22.04 LTS
- **Docker**: 24.0+
- **Docker Compose**: 2.20+
- **Nginx**: 1.22+
- **SSL**: Let's Encrypt

## Docker 배포

### 운영 환경 Docker Compose

#### docker-compose.prod.yml
```yaml
version: '3.8'

services:
  # Nginx 리버스 프록시
  nginx:
    image: nginx:1.22-alpine
    container_name: bookreview-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
      - ./nginx/logs:/var/log/nginx
    depends_on:
      - backend
      - ai-service
    networks:
      - app-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "nginx", "-t"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Spring Boot 백엔드
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: bookreview-backend
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/bookreview?useSSL=true&serverTimezone=UTC
      - SPRING_DATASOURCE_USERNAME=${DB_USERNAME}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      - SPRING_REDIS_HOST=redis
      - SPRING_REDIS_PORT=6379
      - SPRING_REDIS_PASSWORD=${REDIS_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
      - GOOGLE_OAUTH_CLIENT_ID=${GOOGLE_OAUTH_CLIENT_ID}
      - GOOGLE_OAUTH_CLIENT_SECRET=${GOOGLE_OAUTH_CLIENT_SECRET}
    volumes:
      - app-logs:/app/logs
      - app-uploads:/app/uploads
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - app-network
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 2G
          cpus: '1.0'
        reservations:
          memory: 1G
          cpus: '0.5'
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  # FastAPI AI 서비스
  ai-service:
    build:
      context: ./ai-service
      dockerfile: Dockerfile
    container_name: bookreview-ai
    environment:
      - ENVIRONMENT=production
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - REDIS_PASSWORD=${REDIS_PASSWORD}
      - LOG_LEVEL=INFO
    volumes:
      - ai-logs:/app/logs
    depends_on:
      redis:
        condition: service_healthy
    networks:
      - app-network
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 1G
          cpus: '0.5'
        reservations:
          memory: 512M
          cpus: '0.25'
    healthcheck:
      test: ["CMD", "python", "-c", "import requests; requests.get('http://localhost:8000/health')"]
      interval: 30s
      timeout: 10s
      retries: 3

  # MySQL 데이터베이스
  mysql:
    image: mysql:8.0
    container_name: bookreview-mysql
    environment:
      - MYSQL_ROOT_PASSWORD=${DB_ROOT_PASSWORD}
      - MYSQL_DATABASE=bookreview
      - MYSQL_USER=${DB_USERNAME}
      - MYSQL_PASSWORD=${DB_PASSWORD}
    volumes:
      - mysql-data:/var/lib/mysql
      - ./database/init:/docker-entrypoint-initdb.d:ro
      - ./mysql/conf.d:/etc/mysql/conf.d:ro
      - mysql-logs:/var/log/mysql
    networks:
      - app-network
    restart: unless-stopped
    command: >
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --max_connections=1000
      --innodb_buffer_pool_size=1G
      --slow_query_log=1
      --slow_query_log_file=/var/log/mysql/slow.log
      --long_query_time=2
    deploy:
      resources:
        limits:
          memory: 2G
          cpus: '1.0'
        reservations:
          memory: 1G
          cpus: '0.5'
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${DB_ROOT_PASSWORD}"]
      interval: 30s
      timeout: 10s
      retries: 5

  # Redis 캐시
  redis:
    image: redis:7.2-alpine
    container_name: bookreview-redis
    command: redis-server /etc/redis/redis.conf
    volumes:
      - redis-data:/data
      - ./redis/redis.conf:/etc/redis/redis.conf:ro
      - redis-logs:/var/log/redis
    networks:
      - app-network
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: '0.5'
        reservations:
          memory: 256M
          cpus: '0.25'
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  # 모니터링 - Prometheus
  prometheus:
    image: prom/prometheus:latest
    container_name: bookreview-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=30d'
      - '--web.enable-lifecycle'
    networks:
      - app-network
    restart: unless-stopped

  # 모니터링 - Grafana
  grafana:
    image: grafana/grafana:latest
    container_name: bookreview-grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_ADMIN_PASSWORD}
    volumes:
      - grafana-data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./monitoring/grafana/datasources:/etc/grafana/provisioning/datasources
    networks:
      - app-network
    restart: unless-stopped

volumes:
  mysql-data:
    driver: local
  redis-data:
    driver: local
  app-logs:
    driver: local
  ai-logs:
    driver: local
  mysql-logs:
    driver: local
  redis-logs:
    driver: local
  prometheus-data:
    driver: local
  grafana-data:
    driver: local

networks:
  app-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
```

### Nginx 설정

#### nginx/nginx.conf
```nginx
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log notice;
pid /var/run/nginx.pid;

events {
    worker_connections 1024;
    use epoll;
    multi_accept on;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    # 로그 형식
    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log /var/log/nginx/access.log main;

    # 성능 최적화
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;
    client_max_body_size 10M;

    # Gzip 압축
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_comp_level 6;
    gzip_types
        text/plain
        text/css
        text/xml
        text/javascript
        application/json
        application/javascript
        application/xml+rss
        application/atom+xml
        image/svg+xml;

    # 업스트림 서버 정의
    upstream backend {
        server backend:8080 max_fails=3 fail_timeout=30s;
        keepalive 32;
    }

    upstream ai-service {
        server ai-service:8000 max_fails=3 fail_timeout=30s;
        keepalive 32;
    }

    # Rate Limiting
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
    limit_req_zone $binary_remote_addr zone=ai:10m rate=2r/s;

    # HTTP to HTTPS 리다이렉트
    server {
        listen 80;
        server_name yourdomain.com www.yourdomain.com;
        return 301 https://$server_name$request_uri;
    }

    # HTTPS 메인 서버
    server {
        listen 443 ssl http2;
        server_name yourdomain.com www.yourdomain.com;

        # SSL 설정
        ssl_certificate /etc/nginx/ssl/fullchain.pem;
        ssl_certificate_key /etc/nginx/ssl/privkey.pem;
        ssl_session_timeout 1d;
        ssl_session_cache shared:SSL:50m;
        ssl_session_tickets off;

        # Modern SSL configuration
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
        ssl_prefer_server_ciphers off;

        # HSTS
        add_header Strict-Transport-Security "max-age=63072000" always;

        # 보안 헤더
        add_header X-Frame-Options DENY;
        add_header X-Content-Type-Options nosniff;
        add_header X-XSS-Protection "1; mode=block";
        add_header Referrer-Policy "strict-origin-when-cross-origin";

        # API 엔드포인트
        location /api/ {
            limit_req zone=api burst=20 nodelay;
            
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            
            proxy_connect_timeout 30s;
            proxy_send_timeout 30s;
            proxy_read_timeout 30s;
            
            proxy_buffering on;
            proxy_buffer_size 4k;
            proxy_buffers 8 4k;
        }

        # AI 서비스 엔드포인트
        location /ai/ {
            limit_req zone=ai burst=5 nodelay;
            
            proxy_pass http://ai-service/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            
            proxy_connect_timeout 60s;
            proxy_send_timeout 60s;
            proxy_read_timeout 60s;
        }

        # 정적 파일 서빙
        location /static/ {
            alias /var/www/static/;
            expires 1y;
            add_header Cache-Control "public, immutable";
        }

        # 헬스체크
        location /health {
            access_log off;
            return 200 "healthy\n";
            add_header Content-Type text/plain;
        }

        # 에러 페이지
        error_page 404 /404.html;
        error_page 500 502 503 504 /50x.html;
        
        location = /50x.html {
            root /usr/share/nginx/html;
        }
    }
}
```

### 배포 스크립트

#### deploy.sh
```bash
#!/bin/bash

set -e

# 환경 변수 설정
ENVIRONMENT=${1:-production}
COMPOSE_FILE="docker-compose.prod.yml"

echo "Starting deployment for environment: $ENVIRONMENT"

# 환경 변수 파일 확인
if [ ! -f ".env.$ENVIRONMENT" ]; then
    echo "Error: .env.$ENVIRONMENT file not found"
    exit 1
fi

# 환경 변수 로드
export $(cat .env.$ENVIRONMENT | xargs)

# Docker 이미지 빌드
echo "Building Docker images..."
docker-compose -f $COMPOSE_FILE build --no-cache

# 이전 컨테이너 정리
echo "Stopping and removing old containers..."
docker-compose -f $COMPOSE_FILE down

# 새 컨테이너 시작
echo "Starting new containers..."
docker-compose -f $COMPOSE_FILE up -d

# 헬스체크 대기
echo "Waiting for services to be healthy..."
sleep 30

# 서비스 헬스체크
check_health() {
    local service_name=$1
    local health_url=$2
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if curl -f $health_url > /dev/null 2>&1; then
            echo "$service_name is healthy"
            return 0
        fi
        echo "Attempt $attempt/$max_attempts: $service_name not ready yet..."
        sleep 10
        ((attempt++))
    done

    echo "Error: $service_name failed health check"
    return 1
}

# 각 서비스 헬스체크
check_health "Backend API" "http://localhost/api/actuator/health"
check_health "AI Service" "http://localhost/ai/health"
check_health "Nginx" "http://localhost/health"

echo "Deployment completed successfully!"

# 컨테이너 상태 확인
docker-compose -f $COMPOSE_FILE ps

# 로그 출력 (마지막 20줄)
echo -e "\n=== Recent logs ==="
docker-compose -f $COMPOSE_FILE logs --tail=20
```

### 환경 변수 설정

#### .env.production
```bash
# 데이터베이스 설정
DB_ROOT_PASSWORD=super_secure_root_password
DB_USERNAME=bookreview_user
DB_PASSWORD=secure_db_password

# Redis 설정
REDIS_PASSWORD=secure_redis_password

# JWT 설정
JWT_SECRET=your_very_long_and_secure_jwt_secret_key_here

# OAuth 설정
GOOGLE_OAUTH_CLIENT_ID=your_google_oauth_client_id
GOOGLE_OAUTH_CLIENT_SECRET=your_google_oauth_client_secret

# OpenAI API
OPENAI_API_KEY=your_openai_api_key

# 모니터링
GRAFANA_ADMIN_PASSWORD=secure_grafana_password

# SSL 인증서 설정
SSL_EMAIL=admin@yourdomain.com
DOMAIN_NAME=yourdomain.com

# 백업 설정
BACKUP_S3_BUCKET=your-backup-bucket
AWS_ACCESS_KEY_ID=your_aws_access_key
AWS_SECRET_ACCESS_KEY=your_aws_secret_key
```

## 클라우드 배포

### AWS 배포

#### ECS 서비스 정의
```json
{
  "family": "bookreview-backend",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024",
  "memory": "2048",
  "executionRoleArn": "arn:aws:iam::ACCOUNT:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::ACCOUNT:role/ecsTaskRole",
  "containerDefinitions": [
    {
      "name": "backend",
      "image": "your-registry/bookreview-backend:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        }
      ],
      "secrets": [
        {
          "name": "SPRING_DATASOURCE_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:db-password"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/bookreview-backend",
          "awslogs-region": "us-west-2",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": [
          "CMD-SHELL",
          "curl -f http://localhost:8080/actuator/health || exit 1"
        ],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

#### Terraform 인프라 코드
```hcl
# terraform/main.tf
provider "aws" {
  region = var.aws_region
}

# VPC 설정
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "bookreview-vpc"
  }
}

# 서브넷 설정
resource "aws_subnet" "private" {
  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.${count.index + 1}.0/24"
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name = "bookreview-private-${count.index + 1}"
  }
}

resource "aws_subnet" "public" {
  count                   = 2
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.${count.index + 101}.0/24"
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name = "bookreview-public-${count.index + 1}"
  }
}

# RDS MySQL 인스턴스
resource "aws_db_instance" "mysql" {
  identifier = "bookreview-mysql"
  
  engine         = "mysql"
  engine_version = "8.0"
  instance_class = "db.t3.micro"
  
  allocated_storage     = 20
  max_allocated_storage = 100
  storage_encrypted     = true
  
  db_name  = "bookreview"
  username = var.db_username
  password = var.db_password
  
  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.main.name
  
  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"
  
  skip_final_snapshot = false
  final_snapshot_identifier = "bookreview-mysql-final-snapshot"
  
  tags = {
    Name = "bookreview-mysql"
  }
}

# ECS 클러스터
resource "aws_ecs_cluster" "main" {
  name = "bookreview-cluster"

  capacity_providers = ["FARGATE"]
  default_capacity_provider_strategy {
    capacity_provider = "FARGATE"
  }

  tags = {
    Name = "bookreview-cluster"
  }
}

# Application Load Balancer
resource "aws_lb" "main" {
  name               = "bookreview-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id

  enable_deletion_protection = false

  tags = {
    Name = "bookreview-alb"
  }
}

# ECS 서비스
resource "aws_ecs_service" "backend" {
  name            = "bookreview-backend"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = 2
  launch_type     = "FARGATE"

  network_configuration {
    security_groups  = [aws_security_group.ecs_tasks.id]
    subnets          = aws_subnet.private[*].id
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = "backend"
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.main]

  tags = {
    Name = "bookreview-backend-service"
  }
}
```

### Kubernetes 배포

#### kubernetes/deployment.yaml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: bookreview-backend
  namespace: bookreview
  labels:
    app: bookreview-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: bookreview-backend
  template:
    metadata:
      labels:
        app: bookreview-backend
    spec:
      containers:
      - name: backend
        image: your-registry/bookreview-backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:mysql://mysql-service:3306/bookreview"
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: password
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        volumeMounts:
        - name: app-logs
          mountPath: /app/logs
      volumes:
      - name: app-logs
        persistentVolumeClaim:
          claimName: app-logs-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: bookreview-backend-service
  namespace: bookreview
spec:
  selector:
    app: bookreview-backend
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: ClusterIP
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: bookreview-ingress
  namespace: bookreview
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/rate-limit: "100"
spec:
  tls:
  - hosts:
    - api.yourdomain.com
    secretName: bookreview-tls
  rules:
  - host: api.yourdomain.com
    http:
      paths:
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: bookreview-backend-service
            port:
              number: 80
      - path: /ai
        pathType: Prefix
        backend:
          service:
            name: bookreview-ai-service
            port:
              number: 80
```

## CI/CD 파이프라인

### GitHub Actions 배포 워크플로우

#### .github/workflows/deploy.yml
```yaml
name: Deploy to Production

on:
  push:
    branches: [ main ]
  workflow_dispatch:

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Run tests
      run: |
        # 테스트 실행 (이전에 정의된 테스트 워크플로우 참조)
        echo "Running tests..."

  build-and-push:
    needs: test
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: [backend, ai-service]
    
    steps:
    - name: Checkout
      uses: actions/checkout@v3
    
    - name: Set up Docker Buildx
      uses: docker/setup-buildx-action@v2
    
    - name: Log in to Container Registry
      uses: docker/login-action@v2
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}
    
    - name: Extract metadata
      id: meta
      uses: docker/metadata-action@v4
      with:
        images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}-${{ matrix.service }}
        tags: |
          type=ref,event=branch
          type=ref,event=pr
          type=sha,prefix={{branch}}-
          type=raw,value=latest,enable={{is_default_branch}}
    
    - name: Build and push Docker image
      uses: docker/build-push-action@v4
      with:
        context: ./${{ matrix.service }}
        push: true
        tags: ${{ steps.meta.outputs.tags }}
        labels: ${{ steps.meta.outputs.labels }}
        cache-from: type=gha
        cache-to: type=gha,mode=max

  deploy:
    needs: build-and-push
    runs-on: ubuntu-latest
    environment: production
    
    steps:
    - name: Checkout
      uses: actions/checkout@v3
    
    - name: Configure AWS credentials
      uses: aws-actions/configure-aws-credentials@v2
      with:
        aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
        aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        aws-region: us-west-2
    
    - name: Deploy to ECS
      run: |
        # ECS 태스크 정의 업데이트
        aws ecs update-service \
          --cluster bookreview-cluster \
          --service bookreview-backend \
          --force-new-deployment
        
        aws ecs update-service \
          --cluster bookreview-cluster \
          --service bookreview-ai-service \
          --force-new-deployment
    
    - name: Wait for deployment
      run: |
        aws ecs wait services-stable \
          --cluster bookreview-cluster \
          --services bookreview-backend bookreview-ai-service
    
    - name: Verify deployment
      run: |
        # 헬스체크 수행
        curl -f https://api.yourdomain.com/health || exit 1
        curl -f https://api.yourdomain.com/ai/health || exit 1
    
    - name: Notify Slack
      if: always()
      uses: 8398a7/action-slack@v3
      with:
        status: ${{ job.status }}
        channel: '#deployments'
        webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

### GitLab CI/CD

#### .gitlab-ci.yml
```yaml
stages:
  - test
  - build
  - deploy

variables:
  DOCKER_DRIVER: overlay2
  DOCKER_TLS_CERTDIR: "/certs"

test:
  stage: test
  image: openjdk:21-jdk
  services:
    - mysql:8.0
  variables:
    MYSQL_ROOT_PASSWORD: rootpassword
    MYSQL_DATABASE: bookreview_test
  script:
    - cd backend
    - ./gradlew test
  artifacts:
    reports:
      junit: backend/build/test-results/test/*.xml
    paths:
      - backend/build/reports/tests/test/

build:backend:
  stage: build
  image: docker:latest
  services:
    - docker:dind
  script:
    - cd backend
    - docker build -t $CI_REGISTRY_IMAGE/backend:$CI_COMMIT_SHA .
    - docker push $CI_REGISTRY_IMAGE/backend:$CI_COMMIT_SHA
  only:
    - main

build:ai-service:
  stage: build
  image: docker:latest
  services:
    - docker:dind
  script:
    - cd ai-service
    - docker build -t $CI_REGISTRY_IMAGE/ai-service:$CI_COMMIT_SHA .
    - docker push $CI_REGISTRY_IMAGE/ai-service:$CI_COMMIT_SHA
  only:
    - main

deploy:production:
  stage: deploy
  image: alpine:latest
  environment:
    name: production
    url: https://api.yourdomain.com
  before_script:
    - apk add --no-cache curl docker-compose
  script:
    - export BACKEND_IMAGE=$CI_REGISTRY_IMAGE/backend:$CI_COMMIT_SHA
    - export AI_SERVICE_IMAGE=$CI_REGISTRY_IMAGE/ai-service:$CI_COMMIT_SHA
    - docker-compose -f docker-compose.prod.yml up -d
    - ./scripts/wait-for-health.sh
  only:
    - main
  when: manual
```

## 모니터링 및 로깅

### Prometheus 설정

#### monitoring/prometheus.yml
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "alert_rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'spring-boot'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']

  - job_name: 'fastapi'
    metrics_path: '/metrics'
    static_configs:
      - targets: ['ai-service:8000']

  - job_name: 'nginx'
    static_configs:
      - targets: ['nginx:9113']

  - job_name: 'mysql'
    static_configs:
      - targets: ['mysql-exporter:9104']

  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
```

### 알림 규칙

#### monitoring/alert_rules.yml
```yaml
groups:
- name: bookreview-alerts
  rules:
  - alert: HighErrorRate
    expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "High error rate detected"
      description: "Error rate is {{ $value }} errors per second"

  - alert: DatabaseDown
    expr: mysql_up == 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "MySQL database is down"
      description: "MySQL database has been down for more than 1 minute"

  - alert: HighMemoryUsage
    expr: (node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes > 0.9
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High memory usage"
      description: "Memory usage is above 90%"

  - alert: DiskSpaceLow
    expr: (node_filesystem_size_bytes - node_filesystem_free_bytes) / node_filesystem_size_bytes > 0.8
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "Disk space is running low"
      description: "Disk space usage is above 80%"
```

### ELK Stack 로깅

#### docker-compose.logging.yml
```yaml
version: '3.8'

services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.5.0
    container_name: bookreview-elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms1g -Xmx1g"
    volumes:
      - elasticsearch-data:/usr/share/elasticsearch/data
    ports:
      - "9200:9200"
    networks:
      - logging-network

  logstash:
    image: docker.elastic.co/logstash/logstash:8.5.0
    container_name: bookreview-logstash
    volumes:
      - ./logging/logstash.conf:/usr/share/logstash/pipeline/logstash.conf:ro
    ports:
      - "5044:5044"
    environment:
      - "LS_JAVA_OPTS=-Xmx512m -Xms512m"
    networks:
      - logging-network
    depends_on:
      - elasticsearch

  kibana:
    image: docker.elastic.co/kibana/kibana:8.5.0
    container_name: bookreview-kibana
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    networks:
      - logging-network
    depends_on:
      - elasticsearch

networks:
  logging-network:
    driver: bridge

volumes:
  elasticsearch-data:
    driver: local
```

## 백업 및 복구

### 데이터베이스 백업 스크립트

#### scripts/backup-database.sh
```bash
#!/bin/bash

set -e

# 설정
DB_CONTAINER="bookreview-mysql"
DB_NAME="bookreview"
DB_USER="root"
DB_PASSWORD="${DB_ROOT_PASSWORD}"
BACKUP_DIR="/backups/mysql"
S3_BUCKET="${BACKUP_S3_BUCKET}"
RETENTION_DAYS=30

# 백업 디렉토리 생성
mkdir -p $BACKUP_DIR

# 타임스탬프
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="bookreview_backup_${TIMESTAMP}.sql"
BACKUP_PATH="$BACKUP_DIR/$BACKUP_FILE"

echo "Starting database backup: $BACKUP_FILE"

# MySQL 덤프
docker exec $DB_CONTAINER mysqldump \
  -u$DB_USER \
  -p$DB_PASSWORD \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  $DB_NAME > $BACKUP_PATH

# 압축
gzip $BACKUP_PATH
BACKUP_PATH="${BACKUP_PATH}.gz"

echo "Database backup completed: $BACKUP_PATH"

# S3 업로드
if [ ! -z "$S3_BUCKET" ]; then
    echo "Uploading backup to S3..."
    aws s3 cp $BACKUP_PATH s3://$S3_BUCKET/mysql-backups/
    echo "Backup uploaded to S3"
fi

# 로컬 파일 정리 (30일 이상 된 백업 삭제)
find $BACKUP_DIR -name "*.sql.gz" -mtime +$RETENTION_DAYS -delete

echo "Backup process completed successfully"

# 백업 검증
gunzip -t $BACKUP_PATH
if [ $? -eq 0 ]; then
    echo "Backup file integrity verified"
else
    echo "Warning: Backup file may be corrupted"
    exit 1
fi
```

### 복구 스크립트

#### scripts/restore-database.sh
```bash
#!/bin/bash

set -e

BACKUP_FILE=$1
DB_CONTAINER="bookreview-mysql"
DB_NAME="bookreview"
DB_USER="root"
DB_PASSWORD="${DB_ROOT_PASSWORD}"

if [ -z "$BACKUP_FILE" ]; then
    echo "Usage: $0 <backup_file.sql.gz>"
    echo "Available backups:"
    ls -la /backups/mysql/
    exit 1
fi

echo "Starting database restore from: $BACKUP_FILE"

# 백업 파일 존재 확인
if [ ! -f "$BACKUP_FILE" ]; then
    echo "Error: Backup file not found: $BACKUP_FILE"
    exit 1
fi

# 서비스 중지 (선택사항)
read -p "Do you want to stop the application during restore? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Stopping application services..."
    docker-compose stop backend ai-service
fi

# 데이터베이스 백업 (복구 전 현재 상태 백업)
echo "Creating pre-restore backup..."
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
PRE_RESTORE_BACKUP="/backups/mysql/pre_restore_backup_${TIMESTAMP}.sql"
docker exec $DB_CONTAINER mysqldump \
  -u$DB_USER \
  -p$DB_PASSWORD \
  --single-transaction \
  $DB_NAME > $PRE_RESTORE_BACKUP
gzip $PRE_RESTORE_BACKUP

# 데이터베이스 복구
echo "Restoring database..."
if [[ $BACKUP_FILE == *.gz ]]; then
    gunzip -c $BACKUP_FILE | docker exec -i $DB_CONTAINER mysql -u$DB_USER -p$DB_PASSWORD $DB_NAME
else
    cat $BACKUP_FILE | docker exec -i $DB_CONTAINER mysql -u$DB_USER -p$DB_PASSWORD $DB_NAME
fi

echo "Database restore completed"

# 서비스 재시작
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Starting application services..."
    docker-compose start backend ai-service
    
    # 헬스체크
    echo "Waiting for services to be ready..."
    sleep 30
    curl -f http://localhost/api/actuator/health || echo "Warning: Backend health check failed"
fi

echo "Restore process completed successfully"
```

### 자동화된 백업 크론 작업

#### /etc/cron.d/bookreview-backup
```bash
# 매일 새벽 2시에 데이터베이스 백업
0 2 * * * root /opt/bookreview/scripts/backup-database.sh >> /var/log/backup.log 2>&1

# 매주 일요일 새벽 3시에 전체 시스템 백업
0 3 * * 0 root /opt/bookreview/scripts/full-backup.sh >> /var/log/backup.log 2>&1

# 매월 1일 새벽 4시에 로그 정리
0 4 1 * * root /opt/bookreview/scripts/cleanup-logs.sh >> /var/log/cleanup.log 2>&1
```

## 보안 설정

### SSL/TLS 설정

#### Let's Encrypt 자동 갱신
```bash
#!/bin/bash
# scripts/renew-ssl.sh

set -e

DOMAIN="yourdomain.com"
EMAIL="admin@yourdomain.com"
NGINX_CONTAINER="bookreview-nginx"

# Certbot으로 인증서 갱신
docker run --rm \
  -v /etc/letsencrypt:/etc/letsencrypt \
  -v /var/www/certbot:/var/www/certbot \
  certbot/certbot \
  renew --webroot --webroot-path=/var/www/certbot

# Nginx 재로드
docker exec $NGINX_CONTAINER nginx -s reload

echo "SSL certificate renewal completed"
```

### 보안 헤더 및 설정

#### security/security.conf
```nginx
# nginx/conf.d/security.conf

# Security headers
add_header X-Frame-Options "SAMEORIGIN" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header X-Content-Type-Options "nosniff" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self'; frame-ancestors 'none';" always;

# Remove server information
server_tokens off;

# Prevent access to hidden files
location ~ /\. {
    deny all;
}

# Prevent access to backup files
location ~ ~$ {
    deny all;
}

# Rate limiting
limit_req_zone $binary_remote_addr zone=general:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=login:10m rate=1r/s;

# Apply rate limits
location /api/auth/login {
    limit_req zone=login burst=3 nodelay;
    proxy_pass http://backend;
}

location / {
    limit_req zone=general burst=20 nodelay;
    proxy_pass http://backend;
}
```

### 네트워크 보안

#### iptables 규칙
```bash
#!/bin/bash
# scripts/setup-firewall.sh

# 기본 정책: 모든 트래픽 차단
iptables -P INPUT DROP
iptables -P FORWARD DROP
iptables -P OUTPUT ACCEPT

# Loopback 트래픽 허용
iptables -A INPUT -i lo -j ACCEPT

# 기존 연결 유지
iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

# HTTP/HTTPS 트래픽 허용
iptables -A INPUT -p tcp --dport 80 -j ACCEPT
iptables -A INPUT -p tcp --dport 443 -j ACCEPT

# SSH 접근 허용 (필요한 IP만)
iptables -A INPUT -p tcp -s YOUR_ADMIN_IP --dport 22 -j ACCEPT

# 모니터링 포트 (내부 네트워크만)
iptables -A INPUT -p tcp -s 10.0.0.0/8 --dport 3000 -j ACCEPT  # Grafana
iptables -A INPUT -p tcp -s 10.0.0.0/8 --dport 9090 -j ACCEPT  # Prometheus

# DDoS 보호
iptables -A INPUT -p tcp --dport 80 -m limit --limit 25/minute --limit-burst 100 -j ACCEPT
iptables -A INPUT -p tcp --dport 443 -m limit --limit 25/minute --limit-burst 100 -j ACCEPT

# 규칙 저장
iptables-save > /etc/iptables/rules.v4
```

## 성능 최적화

### JVM 튜닝

#### backend/config/jvm-prod.conf
```bash
# JVM 메모리 설정
-Xms2g
-Xmx4g
-XX:NewRatio=1
-XX:SurvivorRatio=8

# 가비지 컬렉터 설정
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=16m

# JIT 컴파일러 최적화
-XX:+TieredCompilation
-XX:TieredStopAtLevel=1

# 메모리 최적화
-XX:+UseStringDeduplication
-XX:+OptimizeStringConcat

# 모니터링
-XX:+PrintGC
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps
-Xloggc:/app/logs/gc.log
-XX:+UseGCLogFileRotation
-XX:NumberOfGCLogFiles=5
-XX:GCLogFileSize=10M

# JFR (Java Flight Recorder)
-XX:+FlightRecorder
-XX:StartFlightRecording=duration=60s,filename=/app/logs/flight.jfr

# 힙 덤프 설정
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/app/logs/

# 원격 디버깅 (개발 환경만)
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
```

### 데이터베이스 최적화

#### mysql/conf.d/performance.cnf
```ini
[mysqld]
# 기본 설정
default-storage-engine = InnoDB
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci

# 연결 설정
max_connections = 1000
max_connect_errors = 1000000
thread_cache_size = 50
table_open_cache = 4000

# InnoDB 설정
innodb_buffer_pool_size = 2G
innodb_buffer_pool_instances = 8
innodb_log_file_size = 256M
innodb_log_files_in_group = 2
innodb_flush_log_at_trx_commit = 2
innodb_flush_method = O_DIRECT
innodb_file_per_table = 1

# 쿼리 캐시 설정
query_cache_type = 1
query_cache_size = 256M
query_cache_limit = 1M

# 로깅 설정
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2
log_queries_not_using_indexes = 1

# 최적화 설정
tmp_table_size = 128M
max_heap_table_size = 128M
join_buffer_size = 256K
sort_buffer_size = 2M
read_buffer_size = 128K
read_rnd_buffer_size = 256K

# 바이너리 로그 설정
log_bin = mysql-bin
binlog_format = ROW
expire_logs_days = 7
max_binlog_size = 100M
```

## 트러블슈팅

### 일반적인 문제 해결

#### 1. 컨테이너 시작 실패
```bash
# 컨테이너 로그 확인
docker-compose logs service-name

# 컨테이너 상태 확인
docker-compose ps

# 리소스 사용량 확인
docker stats

# 포트 충돌 확인
netstat -tulpn | grep :8080
```

#### 2. 데이터베이스 연결 오류
```bash
# MySQL 컨테이너 연결 테스트
docker exec -it bookreview-mysql mysql -u root -p

# 네트워크 연결 확인
docker exec -it bookreview-backend ping mysql

# 환경 변수 확인
docker exec -it bookreview-backend env | grep SPRING_DATASOURCE
```

#### 3. 성능 문제
```bash
# 시스템 리소스 모니터링
htop
iotop
netstat -i

# 애플리케이션 메트릭 확인
curl http://localhost:8080/actuator/metrics

# 데이터베이스 성능 확인
docker exec -it bookreview-mysql mysql -u root -p -e "SHOW PROCESSLIST;"
docker exec -it bookreview-mysql mysql -u root -p -e "SHOW STATUS LIKE 'Slow_queries';"
```

### 장애 대응 절차

#### 1. 서비스 장애 시
```bash
# 1단계: 즉시 대응
./scripts/health-check.sh
docker-compose restart

# 2단계: 로그 분석
tail -f /var/log/nginx/error.log
docker-compose logs --tail=100 backend

# 3단계: 롤백
git checkout previous-stable-commit
./deploy.sh production

# 4단계: 사후 분석
# - 장애 원인 분석
# - 재발 방지 대책 수립
# - 모니터링 개선
```

#### 2. 데이터베이스 장애 시
```bash
# 1단계: 백업 데이터베이스로 전환
./scripts/switch-to-backup-db.sh

# 2단계: 데이터 복구
./scripts/restore-database.sh latest-backup.sql.gz

# 3단계: 데이터 정합성 확인
./scripts/verify-data-integrity.sh
```

### 모니터링 대시보드

#### Grafana 대시보드 설정
```json
{
  "dashboard": {
    "id": null,
    "title": "BookReview Platform Monitoring",
    "panels": [
      {
        "title": "API Response Time",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_request_duration_seconds_sum[5m]) / rate(http_request_duration_seconds_count[5m])",
            "legendFormat": "{{method}} {{uri}}"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "graph", 
        "targets": [
          {
            "expr": "rate(http_requests_total{status=~\"5..\"}[5m])",
            "legendFormat": "5xx Errors"
          }
        ]
      },
      {
        "title": "Database Connections",
        "type": "graph",
        "targets": [
          {
            "expr": "mysql_global_status_threads_connected",
            "legendFormat": "Active Connections"
          }
        ]
      }
    ]
  }
}
```

이 배포 가이드를 통해 BookReview LLM Platform을 안정적이고 확장 가능한 방식으로 운영 환경에 배포할 수 있습니다.