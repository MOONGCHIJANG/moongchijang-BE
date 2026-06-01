variable "aws_region" {
  description = "AWS region"
  type        = string
}

variable "project_name" {
  description = "Project name"
  type        = string
}

variable "bucket_name" {
  description = "S3 bucket name"
  type        = string
}

variable "s3_cors_allowed_origins" {
  description = "Allowed origins for S3 presigned upload CORS"
  type        = list(string)
  default = [
    "http://localhost:3000",
    "https://moongchijang.com",
    "https://www.moongchijang.com",
  ]
}

variable "ec2_instance_type" {
  description = "EC2 instance type"
  type        = string
}

variable "ec2_ami_id" {
  description = "EC2 AMI ID (pin this value to avoid unintended instance replacement)"
  type        = string
}

variable "key_name" {
  description = "EC2 key pair name"
  type        = string
}

variable "ssh_cidr" {
  description = "CIDR block allowed for SSH access"
  type        = string
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
}

variable "db_name" {
  description = "Initial database name"
  type        = string
}

variable "db_username" {
  description = "RDS master username"
  type        = string
}

variable "db_password" {
  description = "RDS master password"
  type        = string
  sensitive   = true
}

variable "redis_node_type" {
  description = "ElastiCache Redis node type"
  type        = string
}

variable "ses_domain" {
  description = "SES sender domain (e.g. example.com). When empty, SES resources are skipped."
  type        = string
  default     = ""
}
