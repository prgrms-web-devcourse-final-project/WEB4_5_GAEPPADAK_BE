terraform {
  // aws 라이브러리 불러옴
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
  }
}

# AWS 설정 시작
provider "aws" {
  region = var.region
}
# AWS 설정 끝

# VPC 설정 시작
resource "aws_vpc" "vpc_1" {
  cidr_block = "10.0.0.0/16"

  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    # ResourceName = "${var.prefix}-vpc-1"
    ResourceName = "team04-vpc-1"
    Name         = "team04-kkokkio"
    Team         = "devcos5-team04"
  }
}

resource "aws_subnet" "subnet_1" {
  vpc_id                  = aws_vpc.vpc_1.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "${var.region}a"
  map_public_ip_on_launch = true

  tags = {
    ResourceName = "${var.prefix}-subnet-1"
    Name         = "team04-kkokkio"
    Team         = "devcos5-team04"
    env          = "dev"
  }
}

resource "aws_subnet" "subnet_2" {
  vpc_id                  = aws_vpc.vpc_1.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "${var.region}b"
  map_public_ip_on_launch = true

  tags = {
    ResourceName = "${var.prefix}-subnet-2"
    Name         = "team04-kkokkio"
    Team         = "devcos5-team04"
    env          = "dev"
  }
}

resource "aws_subnet" "subnet_3" {
  vpc_id                  = aws_vpc.vpc_1.id
  cidr_block              = "10.0.3.0/24"
  availability_zone       = "${var.region}c"
  map_public_ip_on_launch = true

  tags = {
    ResourceName = "${var.prefix}-subnet-3"
    Name         = "team04-kkokkio"
    Team         = "devcos5-team04"
    env          = "dev"
  }
}

resource "aws_subnet" "subnet_4" {
  vpc_id                  = aws_vpc.vpc_1.id
  cidr_block              = "10.0.4.0/24"
  availability_zone       = "${var.region}d"
  map_public_ip_on_launch = true

  tags = {
    ResourceName = "${var.prefix}-subnet-4"
    Name         = "team04-kkokkio"
    Team         = "devcos5-team04"
    env          = "dev"
  }
}

resource "aws_internet_gateway" "igw_1" {
  vpc_id = aws_vpc.vpc_1.id

  tags = {
    ResourceName = "${var.prefix}-igw-1"
    Name         = "team04-kkokkio"
    Team         = "devcos5-team04"
  }
}

resource "aws_route_table" "rt_1" {
  vpc_id = aws_vpc.vpc_1.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw_1.id
  }

  tags = {
    ResourceName = "${var.prefix}-rt-1"
    Name         = "team04-kkokkio"
    Team         = "devcos5-team04"
    env          = "dev"
  }
}

resource "aws_route_table_association" "association_1" {
  subnet_id      = aws_subnet.subnet_1.id
  route_table_id = aws_route_table.rt_1.id
}

resource "aws_route_table_association" "association_2" {
  subnet_id      = aws_subnet.subnet_2.id
  route_table_id = aws_route_table.rt_1.id
}

resource "aws_route_table_association" "association_3" {
  subnet_id      = aws_subnet.subnet_3.id
  route_table_id = aws_route_table.rt_1.id
}

resource "aws_route_table_association" "association_4" {
  subnet_id      = aws_subnet.subnet_4.id
  route_table_id = aws_route_table.rt_1.id
}

resource "aws_security_group" "sg_1" {
  # name   = "${var.prefix}-sg-1"
  name   = "team04-sg-1"
  vpc_id = aws_vpc.vpc_1.id

  ingress {
    from_port = 22
    to_port   = 22
    protocol  = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port = 80
    to_port   = 80
    protocol  = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port = 81
    to_port   = 81
    protocol  = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port = 443
    to_port   = 443
    protocol  = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port = 8080
    to_port   = 8080
    protocol  = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port = 3306
    to_port   = 3306
    protocol  = "tcp"
    self      = true
  }

  ingress {
    from_port = 6379
    to_port   = 6379
    protocol  = "tcp"
    self      = true
  }

  ingress {
    from_port = 3000
    to_port   = 3000
    protocol  = "tcp"
    cidr_blocks = ["0.0.0.0/0"] # 그라파나 필요시 특정 관리자 제한 가능
  }

  ingress {
    from_port = 9090
    to_port   = 9090
    protocol  = "tcp"
    self      = true # Prometheus는 EC2 인스턴스 내부에서만 접근
  }

  ingress {
    from_port = 3100
    to_port   = 3100
    protocol  = "tcp"
    self      = true # Loki는 EC2 인스턴스 내부에서만 접근
  }

  ingress {
    from_port = 9100
    to_port   = 9100
    protocol  = "tcp"
    self      = true # Node Exporter는 EC2 인스턴스 내부에서만 접근
  }

  egress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    # ResourceName = "${var.prefix}-sg-1"
    ResourceName = "team04-sg-1"
    Name         = "team04-kkokkio"
    Team         = "devcos5-team04"
    env          = "dev"
  }
}

# EC2 설정 시작

# EC2 역할 생성
resource "aws_iam_role" "ec2_role_1" {
  # name = "${var.prefix}-ec2-role-1"
  name = "team04-ec2-role-1"

  # 이 역할에 대한 신뢰 정책 설정. EC2 서비스가 이 역할을 가정할 수 있도록 설정
  assume_role_policy = <<EOF
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Sid": "",
        "Action": "sts:AssumeRole",
        "Principal": {
            "Service": "ec2.amazonaws.com"
        },
        "Effect": "Allow"
      }
    ]
  }
EOF
}

# EC2 역할에 AmazonS3FullAccess 정책을 부착
resource "aws_iam_role_policy_attachment" "s3_full_access" {
  role       = aws_iam_role.ec2_role_1.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

# EC2 역할에 AmazonEC2RoleforSSM 정책을 부착
resource "aws_iam_role_policy_attachment" "ec2_ssm" {
  role       = aws_iam_role.ec2_role_1.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2RoleforSSM"
}

# IAM 인스턴스 프로파일 생성
resource "aws_iam_instance_profile" "instance_profile_1" {
  # name = "${var.prefix}-instance-profile-1"
  name = "team04-instance-profile-1"
  role = aws_iam_role.ec2_role_1.name
}

locals {
  docker_compose_content = <<-EOT
version: '3.8'
services:
  app1:
    container_name: app1
    image: ghcr.io/prgrms-web-devcourse-final-project/team04-kkokkio-${var.env}:latest
    restart: always
    environment:
      - DOPPLER_TOKEN=${var.DOPPLER_SERVICE_TOKEN}
      - DB_HOST=mysql
      - DB_USERNAME=${var.DB_USERNAME}
      - DB_PASSWORD=${var.DB_PASSWORD}
      - DB_NAME=${var.DB_NAME}
      - DB_PORT=${var.DB_PORT}
    ports:
      - "8080:8080" # 외부에서 접근해야 할 경우만 포트 매핑 유지
    volumes:
      - /dockerProjects/logs:/app/logs
    networks:
      - common_internal
    depends_on:
      redis:
        condition: service_started
      mysql:
        condition: service_healthy

  redis:
    container_name: redis
    image: redis:alpine
    restart: always
    ports:
      - "6379:6379"
    volumes:
      - /dockerProjects/redis_data:/data
    command: ["redis-server", "--notify-keyspace-events", "Ex"]
    networks:
      - common_internal

  npm_1:
    container_name: npm_1
    image: jc21/nginx-proxy-manager:latest
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
      - "81:81"
    environment:
      - TZ=UTC
    volumes:
      - /dockerProjects/npm_1/volumes/data:/data
      - /dockerProjects/npm_1/volumes/etc/letsencrypt:/etc/letsencrypt
    networks:
      - common_internal

  prometheus:
    container_name: prometheus
    image: prom/prometheus:latest
    restart: always
    ports:
      - "9090:9090"
    volumes:
      - /dockerProjects/prometheus.yml:/etc/prometheus/prometheus.yml
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
    networks:
      - common_internal

  loki:
    container_name: loki
    image: grafana/loki:latest
    restart: always
    ports:
      - "3100:3100"
    volumes:
      - /dockerProjects/loki-config.yaml:/etc/loki/local-config.yaml
      - /dockerProjects/tmp/loki:/tmp/loki
    command: -config.file=/etc/loki/local-config.yaml
    networks:
      - common_internal

  promtail:
    container_name: promtail
    image: grafana/promtail:latest
    restart: always
    volumes:
      - /dockerProjects/logs:/app/logs:ro
      - /dockerProjects/promtail-config.yml:/etc/promtail/config.yml
    command: -config.file=/etc/promtail/config.yml
    networks:
      - common_internal
    depends_on:
      loki:
        condition: service_started

  grafana:
    container_name: grafana
    image: grafana/grafana:latest
    restart: always
    ports:
      - "3000:3000"
    volumes:
      - /dockerProjects/grafana_data:/var/lib/grafana
    environment:
      - GF_SECURITY_ADMIN_USER=${var.DB_USERNAME}
      - GF_SECURITY_ADMIN_PASSWORD=${var.DB_PASSWORD}
    networks:
      - common_internal
    depends_on:
      prometheus:
        condition: service_started
      loki:
        condition: service_started

  node-exporter:
    container_name: node-exporter
    image: prom/node-exporter:latest
    restart: always
    ports:
      - "9100:9100"
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    command:
      - '--path.procfs=/host/proc'
      - '--path.sysfs=/host/sys'
      - '--collector.filesystem.ignored-mount-points=^/(sys|proc|dev|host|etc)($$|/)'
    networks:
      - common_internal

  mysql: # MySQL 컨테이너 서비스 정의 추가
    container_name: mysql
    image: mysql:8.0
    restart: always
    ports:
      - "${var.DB_PORT}:${var.DB_PORT}"
    volumes:
      - /dockerProjects/db_data:/var/lib/mysql
      - /dockerProjects/mysql_logs:/logs # MySQL 일반 로그를 호스트에 저장
      # MySQL 초기화 스크립트를 볼륨 마운트
      - /dockerProjects/mysql_init:/docker-entrypoint-initdb.d
    environment:
      MYSQL_ROOT_PASSWORD: ${var.DB_ROOT_PASSWORD}
      MYSQL_DATABASE: ${var.DB_NAME}
      MYSQL_USER: ${var.DB_USERNAME}
      MYSQL_PASSWORD: ${var.DB_PASSWORD}
      TZ: UTC
    command: ["mysqld", "--general-log=1", "--general-log-file=/logs/general.log"] # /logs 볼륨에 로그 저장
    networks:
      - common_internal
    healthcheck: # MySQL 컨테이너 헬스체크 추가
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u${var.DB_USERNAME}", "-p${var.DB_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 10

  mysql-exporter:
    container_name: mysql-exporter
    image: prom/mysqld-exporter:latest
    restart: always
    command:
      - "--mysqld.username=${var.DB_USERNAME}:${var.DB_PASSWORD}"
      - "--mysqld.address=mysql:${var.DB_PORT}"
    networks:
      - common_internal
    healthcheck: # MySQL exporter 컨테이너 헬스체크 추가
      test: ["CMD", "curl", "-f", "http://mysql-exporter:9104/metrics"]
      interval: 10s
      timeout: 5s
      retries: 5
    depends_on: # mysql-exporter는 mysql 컨테이너에 의존성을 가짐
      mysql:
        condition: service_healthy

networks:
  common_internal:
    name: common
    driver: bridge

volumes:
  prometheus_data:
  grafana_data:
EOT
}

locals {
  ec2_user_data_base = <<-END_OF_FILE
#!/bin/bash

# EC2 인스턴스 OS 타임존을 UTC로 설정
ln -sf /usr/share/zoneinfo/UTC /etc/localtime
echo "UTC" > /etc/timezone

# 가상 메모리 4GB 설정
sudo dd if=/dev/zero of=/swapfile bs=128M count=32
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
sudo sh -c 'echo "/swapfile swap swap defaults 0 0" >> /etc/fstab'

캐시 및 목록 업데이트를 먼저 수행하여 설치 안정성 확보
sudo yum update -y

# 도커 설치 및 실행/활성화
yum install docker -y
systemctl enable docker
systemctl start docker

# docker-compose 설치
curl -L "https://github.com/docker/compose/releases/download/v2.24.5/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
# docker-compose 명령을 /usr/bin에서 바로 실행되도록 심볼릭 링크 생성
ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose


# gnupg2 설치 (doppler CLI용)
yum install -y --allowerasing gnupg2

# 도플러 CLI 설치
curl -Ls https://cli.doppler.com/install.sh | sudo DOPPLER_INSTALL_DIR=/usr/local/bin sh

# 프로젝트 디렉토리 및 권한 생성
for d in tmp/loki logs grafana_data npm_1/volumes/data npm_1/volumes/etc/letsencrypt redis_data db_data mysql_logs mysql_init; do
  mkdir -p "/dockerProjects/$d" || { echo "디렉토리 생성 실패: $d"; exit 1; }
done

chown -R 10001:10001 /dockerProjects/tmp/loki
chown -R 10001:10001 /dockerProjects/logs
chown -R 472:472 /dockerProjects/grafana_data
chown -R 999:999 /dockerProjects/db_data # MySQL 데이터 볼륨 소유자
chown -R 999:999 /dockerProjects/mysql_logs # MySQL 로그 볼륨 소유자
chown -R 999:999 /dockerProjects/mysql_init # MySQL 초기화 스크립트 볼륨 소유자

chmod -R 755 /dockerProjects
chmod 777 /dockerProjects/logs

# MySQL 초기화 SQL 스크립트 생성
cat <<EOF > /dockerProjects/mysql_init/init_db.sql
CREATE USER '${var.MYSQL_USER_1}'@'127.0.0.1' IDENTIFIED WITH caching_sha2_password BY '${var.PASSWORD_3}';
CREATE USER '${var.MYSQL_USER_1}'@'172.18.%.%' IDENTIFIED WITH caching_sha2_password BY '${var.PASSWORD_2}';
CREATE USER '${var.MYSQL_USER_2}'@'%' IDENTIFIED WITH caching_sha2_password BY '${var.PASSWORD_1}';

GRANT ALL PRIVILEGES ON *.* TO '${var.MYSQL_USER_1}'@'127.0.0.1';
GRANT ALL PRIVILEGES ON *.* TO '${var.MYSQL_USER_1}'@'172.18.%.%';
GRANT ALL PRIVILEGES ON *.* TO '${var.MYSQL_USER_2}'@'%';

CREATE DATABASE IF NOT EXISTS ${var.DB_NAME};

FLUSH PRIVILEGES;
EOF

chmod 644 /dockerProjects/mysql_init/init_db.sql # 초기화 스크립트 권한 설정


# Loki 설정 파일 작성
cat <<EOL > /dockerProjects/loki-config.yaml
auth_enabled: false
server:
  http_listen_port: 3100
common:
  ring:
    instance_addr: 127.0.0.1
    kvstore:
      store: inmemory
  replication_factor: 1
  path_prefix: /tmp/loki
schema_config:
  configs:
    - from: 2020-05-15
      store: tsdb
      object_store: filesystem
      schema: v13
      index:
        prefix: index_
        period: 24h
query_range:
  results_cache:
    cache:
      embedded_cache:
        enabled: true
        max_size_mb: 100
storage_config:
  filesystem:
    directory: /tmp/loki/chunks
limits_config:
  volume_enabled: true
  ingestion_rate_mb: 10
  ingestion_burst_size_mb: 20
  per_stream_rate_limit: 5MB
  per_stream_rate_limit_burst: 10MB
EOL

# Promtail 설정 파일 작성
cat <<EOL > /dockerProjects/promtail-config.yml
server:
  http_listen_port: 9080
  grpc_listen_port: 0
positions:
  filename: /tmp/positions.yaml
clients:
  - url: http://loki:3100/loki/api/v1/push
scrape_configs:
  - job_name: app-logs
    static_configs:
      - targets: ["localhost"]
        labels:
          job: app
          __path__: /app/logs/*.log
    pipeline_stages:
      - multiline:
          firstline: '^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3} \['
EOL

# Prometheus 설정 파일 작성
cat <<EOL > /dockerProjects/prometheus.yml
global:
  scrape_interval: 15s
  # evaluation_interval: 15s # rule evaluation 주기

scrape_configs:
  - job_name: "app"
    metrics_path: "/api/actuator/prometheus"
    honor_labels: true # ← 우리가 붙인 tag 우선
    static_configs:
      - targets:
          - "app1:8080"
        labels:
          instance: "app"
  - job_name: "mysql_exporter"
    metrics_path: "/metrics"
    static_configs:
      - targets: ["mysql-exporter:9104"]
  - job_name: "node_exporter"
    scrape_interval: 5s
    static_configs:
      - targets: ["node-exporter:9100"]
EOL

# GitHub Container Registry 로그인
echo "${var.GITHUB_ACCESS_TOKEN_1}" | docker login ghcr.io -u ${var.GITHUB_ACCESS_TOKEN_1_OWNER} --password-stdin

# Docker Compose 파일 생성 (local.docker_compose_content를 여기에 추가)
cat <<EOT_DOCKER_COMPOSE > /dockerProjects/docker-compose.yml
${local.docker_compose_content}
EOT_DOCKER_COMPOSE

# Docker Compose 실행
cd /dockerProjects
docker-compose up -d

END_OF_FILE
}

#최신 AMI를 찾는 스크립트
data "aws_ami" "latest_amazon_linux" {
  most_recent = true
  owners = ["amazon"]

  filter {
    name = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }

  filter {
    name = "architecture"
    values = ["x86_64"]
  }

  filter {
    name = "virtualization-type"
    values = ["hvm"]
  }

  filter {
    name = "root-device-type"
    values = ["ebs"]
  }
}

# EC2 인스턴스 생성
resource "aws_instance" "ec2_1" {
  # 사용할 AMI ID
  # ami 최신버전이 갱신돼 최신 ami를 받으면 기존 서버가 destroy됨
  # 현재 서버가 destroy되는걸 막기 위해 직접 설정
  # ami = data.aws_ami.latest_amazon_linux.id

  # 초기 개발용 t3.small ami. 이후 사양 추가를 위해 변경
  # ami = "ami-0eb302fcc77c2f8bd"
  ami = "ami-05377cf8cfef186c2"
  # EC2 인스턴스 유형
  # instance_type = "t3.micro"
  instance_type = "t3.small"
  # 사용할 서브넷 ID
  subnet_id = aws_subnet.subnet_2.id
  # 적용할 보안 그룹 ID
  vpc_security_group_ids = [aws_security_group.sg_1.id]
  # 퍼블릭 IP 연결 설정
  associate_public_ip_address = true

  # 인스턴스에 IAM 역할 연결
  iam_instance_profile = aws_iam_instance_profile.instance_profile_1.name

  # 인스턴스에 태그 설정
  tags = {
    # ResourceName = "${var.prefix}-ec2-1"
    ResourceName = "team04-ec2-1"
    Name         = "team04-kkokkio"
    Team         = "devcos5-team04"
    env          = "dev"
  }

  # 루트 볼륨 설정
  root_block_device {
    volume_type = "gp3"
    volume_size = 12 # 볼륨 크기를 12GB로 설정
  }

  user_data = <<-EOF
${local.ec2_user_data_base}
EOF
}