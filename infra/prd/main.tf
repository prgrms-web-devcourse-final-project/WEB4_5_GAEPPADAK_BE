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
  ec2_user_data_base = <<-END_OF_FILE
#!/bin/bash
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

# gnupg2 설치 (doppler CLI용)
yum install -y --allowerasing gnupg2

# 도플러 CLI 설치
curl -Ls https://cli.doppler.com/install.sh | sudo DOPPLER_INSTALL_DIR=/usr/local/bin sh

# 도커 네트워크 생성
docker network create common

# nginx-proxy-manager
docker run -d \
  --name npm_1 \
  --restart unless-stopped \
  --network common \
  -p 80:80 \
  -p 443:443 \
  -p 81:81 \
  -e TZ=Asia/Seoul \
  -v /dockerProjects/npm_1/volumes/data:/data \
  -v /dockerProjects/npm_1/volumes/etc/letsencrypt:/etc/letsencrypt \
  jc21/nginx-proxy-manager:latest

# redis 컨테이너 (도커컴포즈 맞춤)
docker run -d \
  --name redis \
  --restart always \
  --network common \
  -p 6379:6379 \
  -v /dockerProjects/redis_data:/data \
  -e TZ=Asia/Seoul \
  redis:alpine

# prd 환경에선 mysql 대신 rds 사용

echo "${var.GITHUB_ACCESS_TOKEN_1}" | docker login ghcr.io -u ${var.GITHUB_ACCESS_TOKEN_1_OWNER} --password-stdin

# app1 컨테이너 실행 (Doppler 사용)
docker run --restart always -e DOPPLER_TOKEN=${var.DOPPLER_SERVICE_TOKEN} -d --name app1 --network common -p 8080:8080 ghcr.io/prgrms-web-devcourse-final-project/team04-kkokkio-${var.env}:latest

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
  ami = "ami-0eb302fcc77c2f8bd"
  # EC2 인스턴스 유형
  instance_type = "t3.micro"
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