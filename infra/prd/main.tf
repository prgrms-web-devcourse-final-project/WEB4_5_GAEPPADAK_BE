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

## 변경 ## 기존 개발 환경 VPC를 데이터 소스로 가져옴 - 새로운 VPC 생성 부분을 삭제하고 추가됨
# 기존 VPC를 CIDR 블록으로 찾습니다. (기존 dev VPC가 10.0.0.0/16임을 가정)
data "aws_vpc" "existing_vpc" {
filter {
name = "cidr-block"
values = ["10.0.0.0/16"]
}
filter {
name = "tag:Name"
values = ["team04-kkokkio"]
}
filter {
name = "tag:Team"
values = ["devcos5-team04"]
}
}

resource "aws_subnet" "subnet_1" {
vpc_id                  = data.aws_vpc.existing_vpc.id
cidr_block              = "10.0.5.0/24"
availability_zone       = "${var.region}a"
map_public_ip_on_launch = true

tags = {
ResourceName = "${var.prefix}-subnet-1"
Name         = "team04-kkokkio"
Team         = "devcos5-team04"
env          = "prd"
}
}

resource "aws_subnet" "subnet_2" {
vpc_id                  = data.aws_vpc.existing_vpc.id
cidr_block              = "10.0.6.0/24"
availability_zone       = "${var.region}b"
map_public_ip_on_launch = true

tags = {
ResourceName = "${var.prefix}-subnet-2"
Name         = "team04-kkokkio"
Team         = "devcos5-team04"
env          = "prd"
}
}

resource "aws_subnet" "subnet_3" {
vpc_id                  = data.aws_vpc.existing_vpc.id
cidr_block              = "10.0.7.0/24"
availability_zone       = "${var.region}c"
map_public_ip_on_launch = true

tags = {
ResourceName = "${var.prefix}-subnet-3"
Name         = "team04-kkokkio"
Team         = "devcos5-team04"
env          = "prd"
}
}

resource "aws_subnet" "subnet_4" {
vpc_id                  = data.aws_vpc.existing_vpc.id
cidr_block              = "10.0.8.0/24"
availability_zone       = "${var.region}d"
map_public_ip_on_launch = true

tags = {
ResourceName = "${var.prefix}-subnet-4"
Name         = "team04-kkokkio"
Team         = "devcos5-team04"
env          = "prd"
}
}

# 기존 VPC에 연결된 인터넷 게이트웨이를 데이터 소스로 가져옴
data "aws_internet_gateway" "existing_igw" {
filter {
name = "tag:Name"
values = ["team04-kkokkio"]
}

filter {
name = "tag:Team"
values = ["devcos5-team04"]
}
}

resource "aws_route_table" "rt_1" {
vpc_id = data.aws_vpc.existing_vpc.id

route {
cidr_block = "0.0.0.0/0"
gateway_id = data.aws_internet_gateway.existing_igw.id
}

tags = {
ResourceName = "${var.prefix}-rt-1"
Name         = "team04-kkokkio"
Team         = "devcos5-team04"
env          = "prd"
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
name   = "${var.prefix}-sg-1"
vpc_id = data.aws_vpc.existing_vpc.id

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
from_port = 6379
to_port   = 6379
protocol  = "tcp"
self      = true
}

ingress {
from_port = 3000
to_port   = 3000
protocol  = "tcp"
cidr_blocks = ["0.0.0.0/0"] # 필요시 제한 가능
}

ingress {
from_port = 9090
to_port   = 9090
protocol  = "tcp"
cidr_blocks = ["0.0.0.0/0"]
}

ingress {
from_port = 3100
to_port   = 3100
protocol  = "tcp"
cidr_blocks = ["0.0.0.0/0"]
}

ingress {
from_port = 9100
to_port   = 9100
protocol  = "tcp"
cidr_blocks = ["0.0.0.0/0"]
}

egress {
from_port = 0
to_port   = 0
protocol  = "-1"
cidr_blocks = ["0.0.0.0/0"]
}

tags = {
ResourceName = "${var.prefix}-sg-1"
Name         = "team04-kkokkio"
Team         = "devcos5-team04"
env          = "prd"
}
}

# RDS 리소스

# RDS Subnet Group (서브넷 2개 이상 필요) - 새로 생성된 prd 서브넷 3, 4 사용
resource "aws_db_subnet_group" "rds_subnet_group" {
name = "${var.prefix}-rds-subnet-group"
subnet_ids = [aws_subnet.subnet_3.id, aws_subnet.subnet_4.id]

tags = {
Name = "${var.prefix}-rds-subnet-group"
Team = "devcos5-team04"
env  = "prd"
}
}

# RDS 보안 그룹 (EC2에서 접근 가능하도록 설정)
resource "aws_security_group" "rds_sg" {
name   = "${var.prefix}-rds-sg"
vpc_id = data.aws_vpc.existing_vpc.id

ingress {
from_port = 3306
to_port   = 3306
protocol = "tcp"
# cidr_blocks = ["10.0.0.0/16"] # EC2와 동일한 VPC 대역
security_groups = [aws_security_group.sg_1.id]
}

# 기존 팀원들의 IP 주소 규칙들을 여기에 추가

egress {
from_port = 0
to_port   = 0
protocol  = "-1"
cidr_blocks = ["0.0.0.0/0"]
}

tags = {
Name = "${var.prefix}-rds-sg"
Team = "devcos5-team04"
env  = "prd"
}
}

# RDS 인스턴스 생성 (MySQL)
resource "aws_db_instance" "mysql" {
identifier            = "${var.prefix}-mysql"
engine                = "mysql"
engine_version        = "8.0"
instance_class        = "db.t3.micro"
allocated_storage     = 20
max_allocated_storage = 100
storage_type          = "gp2"

db_name  = var.DB_NAME
username = var.DB_USERNAME
password = var.DB_PASSWORD
port     = 3306

vpc_security_group_ids = [aws_security_group.rds_sg.id]
db_subnet_group_name = aws_db_subnet_group.rds_subnet_group.name
multi_az             = false
publicly_accessible  = false
skip_final_snapshot  = true
deletion_protection  = false

tags = {
Name = "${var.prefix}-mysql"
Team = "devcos5-team04"
env  = "prd"
}
}


# EC2 설정 시작

# EC2 역할 생성
resource "aws_iam_role" "ec2_role_1" {
name = "${var.prefix}-ec2-role-1"

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
name = "${var.prefix}-instance-profile-1"
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
      - DB_HOST=${aws_db_instance.mysql.address} # RDS 엔드포인트를 환경변수로 주입
      - DB_USERNAME=${var.DB_USERNAME}
      - DB_PASSWORD=${var.DB_PASSWORD}
      - DB_NAME=${var.DB_NAME}
      - DB_PORT=${aws_db_instance.mysql.port}
    ports:
      - "8080:8080" # 외부에서 접근해야 할 경우만 포트 매핑 유지
    volumes:
      - /dockerProjects/logs:/app/logs
    networks:
      - common
    depends_on:
      redis:
        condition: service_started

  redis:
    container_name: redis
    image: redis:alpine
    restart: always
    ports:
      - "6379:6379" # 외부에서 접근해야 할 경우만 포트 매핑 유지. app1만 사용한다면 제거 고려.
    volumes:
      - /dockerProjects/redis_data:/data
    command: ["redis-server", "--notify-keyspace-events", "Ex"]
    networks:
      - common

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
      - common

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
      - common

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
      - common

  promtail:
    container_name: promtail
    image: grafana/promtail:latest
    restart: always
    volumes:
      - /dockerProjects/logs:/app/logs:ro
      - /dockerProjects/promtail-config.yml:/etc/promtail/config.yml
    command: -config.file=/etc/promtail/config.yml
    networks:
      - common
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
      - common
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
      - common

  mysql-exporter:
    container_name: mysql-exporter
    image: prom/mysqld-exporter:latest
    restart: always
    command:
      - "--mysqld.username=${var.DB_USERNAME}:${var.DB_PASSWORD}"
      - "--mysqld.address=${aws_db_instance.mysql.address}:${aws_db_instance.mysql.port}"
    networks:
      - common

networks:
  common:
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
for d in tmp/loki logs grafana_data npm_1/volumes/data npm_1/volumes/etc/letsencrypt redis_data db_data mysql_logs; do
  mkdir -p "/dockerProjects/$d" || { echo "디렉토리 생성 실패: $d"; exit 1; }
done

# mkdir -p "/dockerProjects/mysql-exporter" || { echo "디렉토리 생성 실패: mysql-exporter"; exit 1; }

chown -R 10001:10001 /dockerProjects/tmp/loki
chown -R 10001:10001 /dockerProjects/logs
chown -R 472:472 /dockerProjects/grafana_data
chmod -R 755 /dockerProjects

# Loki 설정 파일 작성
mkdir -p /dockerProjects/tmp/loki /dockerProjects/logs /dockerProjects/grafana_data
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
          firstline: '^\\[ls\\] \\['
          separator: '\\] \\[le\\]'
EOL

# Prometheus 설정 파일 작성
cat <<EOL > /dockerProjects/prometheus.yml
global:
  scrape_interval: 15s
scrape_configs:
  - job_name: "app"
    metrics_path: "/api/actuator/prometheus"
    honor_labels: true
    static_configs:
      - targets:
          - "app1:8080"
        labels:
          instance: "app"
  - job_name: "node_exporter"
    scrape_interval: 5s
    static_configs:
      - targets: ["node-exporter:9100"]
# --- MySQL Exporter 메트릭 수집 설정 추가 ---
  - job_name: "mysql" # MySQL 메트릭을 위한 새로운 Job 이름
    scrape_interval: 60s # MySQL 메트릭은 보통 1분 간격으로도 충분
    static_configs:
      - targets: ["mysql-exporter:9104"] # <--- mysqld_exporter 컨테이너의 서비스 이름과 포트를 지정 (기본 포트는 9104)
EOL

# GitHub Container Registry 로그인
echo "${var.GITHUB_ACCESS_TOKEN_1}" | docker login ghcr.io -u ${var.GITHUB_ACCESS_TOKEN_1_OWNER} --password-stdin

# app1 컨테이너 실행 (Doppler 사용)
# docker run --restart always -e DOPPLER_TOKEN=${var.DOPPLER_SERVICE_TOKEN} -d --name app1 --network common -p 8080:8080 ghcr.io/prgrms-web-devcourse-final-project/team04-kkokkio-${var.env}:latest

# Docker Compose 파일 생성 (local.docker_compose_content를 여기에 추가)
cat <<EOT_DOCKER_COMPOSE > /dockerProjects/docker-compose.yml
${local.docker_compose_content}
EOT_DOCKER_COMPOSE

# Docker Compose 실행
cd /dockerProjects
docker-compose up -d

END_OF_FILE
}
# EC2 인스턴스 생성
resource "aws_instance" "ec2_1" {
# 사용할 AMI ID
ami = "ami-05377cf8cfef186c2"
# EC2 인스턴스 유형
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
ResourceName = "${var.prefix}-ec2-1"
Name         = "team04-kkokkio"
Team         = "devcos5-team04"
env          = "prd"
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