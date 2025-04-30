# 도플러 환경변수를 적용해 실행해야 합니다.
# doppler run -- terraform init
# doppler run -- terraform plan
# doppler run -- terraform apply
# doppler run -- terraform destroy

variable "prefix" {
  description = "Prefix for all resources"
  default     = "team04"
}

variable "region" {
  description = "region"
  default     = "ap-northeast-2"
}

variable "db_name" {
  description = "db name"
  default     = "kkokkio_dev"
}

variable "PASSWORD_1" {
  description = "password_1 (Provided via Doppler)"
}

variable "PASSWORD_2" {
  description = "password_2 (Provided via Doppler)"
}

variable "PASSWORD_3" {
  description = "password_3 (Provided via Doppler)"
}

variable "GITHUB_ACCESS_TOKEN_1" {
  description = "github_access_token_1, read:packages only (Provided via Doppler)"
}

variable "GITHUB_ACCESS_TOKEN_1_OWNER" {
  description = "github_access_token_1_owner (Provided via Doppler)"
}


variable "MYSQL_USER_1" {
  description = "mysql_user_1 (Provided via Doppler)"
}

variable "MYSQL_USER_2" {
  description = "mysql_user_2 (Provided via Doppler)"
}

variable "DB_ROOT_PASSWORD" {
  description = "DB_ROOT_PASSWORD (Provided via Doppler)"
}

variable "DB_NAME" {
  description = "DB_NAME (Provided via Doppler)"
}

variable "DB_USERNAME" {
  description = "DB_USERNAME (Provided via Doppler)"
}

variable "DB_PASSWORD" {
  description = "DB_PASSWORD (Provided via Doppler)"
}

