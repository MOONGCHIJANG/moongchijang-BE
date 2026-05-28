// S3 버킷 생성
resource "aws_s3_bucket" "terraform_test_bucket" {
  bucket = var.bucket_name

  tags = {
    Name    = "${var.project_name}-terraform-test"
    Project = var.project_name
  }
}

// S3 퍼블릭 액세스 차단
resource "aws_s3_bucket_public_access_block" "terraform_test_bucket" {
  bucket = aws_s3_bucket.terraform_test_bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

// S3 버전 관리 설정
resource "aws_s3_bucket_versioning" "terraform_test_bucket" {
  bucket = aws_s3_bucket.terraform_test_bucket.id

  versioning_configuration {
    status = "Enabled"
  }
}

// S3 기본 암호화 설정
resource "aws_s3_bucket_server_side_encryption_configuration" "terraform_test_bucket" {
  bucket = aws_s3_bucket.terraform_test_bucket.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

// 애플리케이션 서버 보안 그룹 설정
resource "aws_security_group" "app_sg" {
  name        = "${var.project_name}-app-sg"
  description = "Security group for app server"

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.ssh_cidr]
  }

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name    = "${var.project_name}-app-sg"
    Project = var.project_name
  }
}

// EC2 IAM 역할 설정
resource "aws_iam_role" "ec2_role" {
  name = "${var.project_name}-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

// EC2 S3 최소 권한 정책 문서
data "aws_iam_policy_document" "ec2_s3_least_privilege" {
  statement {
    sid    = "S3ObjectReadWriteDeleteForAppBucket"
    effect = "Allow"
    actions = [
      "s3:GetObject",
      "s3:PutObject",
      "s3:DeleteObject",
      "s3:AbortMultipartUpload",
    ]
    resources = ["${aws_s3_bucket.terraform_test_bucket.arn}/*"]
  }

  statement {
    sid    = "S3ListBucketForAppBucket"
    effect = "Allow"
    actions = [
      "s3:ListBucket",
    ]
    resources = [aws_s3_bucket.terraform_test_bucket.arn]
  }
}

// EC2 S3 최소 권한 정책
resource "aws_iam_policy" "ec2_s3_least_privilege" {
  name        = "${var.project_name}-ec2-s3-least-privilege"
  description = "Least-privilege S3 access for ${var.project_name} EC2 application role"
  policy      = data.aws_iam_policy_document.ec2_s3_least_privilege.json
}

// EC2 S3 최소 권한 정책 연결
resource "aws_iam_role_policy_attachment" "ec2_s3_least_privilege" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = aws_iam_policy.ec2_s3_least_privilege.arn
}

// EC2 인스턴스 프로파일 설정
resource "aws_iam_instance_profile" "ec2_profile" {
  name = "${var.project_name}-ec2-profile"
  role = aws_iam_role.ec2_role.name
}

// EC2 인스턴스 생성
resource "aws_instance" "app_server" {
  ami                    = var.ec2_ami_id
  instance_type          = var.ec2_instance_type
  key_name               = var.key_name
  vpc_security_group_ids = [aws_security_group.app_sg.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

  tags = {
    Name    = "${var.project_name}-app-server"
    Project = var.project_name
  }
}

// 기본 VPC 조회
data "aws_vpc" "default" {
  default = true
}

// 기본 서브넷 목록 조회
data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

// RDS 보안 그룹 설정
resource "aws_security_group" "rds_sg" {
  name        = "${var.project_name}-rds-sg"
  description = "Security group for RDS"

  ingress {
    description     = "MySQL from EC2"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.app_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name    = "${var.project_name}-rds-sg"
    Project = var.project_name
  }
}

// RDS 서브넷 그룹 설정
resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = data.aws_subnets.default.ids

  tags = {
    Name    = "${var.project_name}-db-subnet-group"
    Project = var.project_name
  }
}

// MySQL RDS 인스턴스 생성
resource "aws_db_instance" "main" {
  identifier             = "${var.project_name}-mysql-db"
  engine                 = "mysql"
  instance_class         = var.db_instance_class
  allocated_storage      = 20
  storage_type           = "gp3"
  db_name                = var.db_name
  username               = var.db_username
  password               = var.db_password
  publicly_accessible    = false
  skip_final_snapshot    = true
  deletion_protection    = false
  multi_az               = false
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds_sg.id]

  tags = {
    Name    = "${var.project_name}-mysql-db"
    Project = var.project_name
  }
}

// Redis 보안 그룹 설정
resource "aws_security_group" "redis_sg" {
  name        = "${var.project_name}-redis-sg"
  description = "Security group for Redis"

  ingress {
    description     = "Redis from EC2"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.app_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name    = "${var.project_name}-redis-sg"
    Project = var.project_name
  }
}

// Redis 서브넷 그룹 설정
resource "aws_elasticache_subnet_group" "redis" {
  name       = "${var.project_name}-redis-subnet-group"
  subnet_ids = data.aws_subnets.default.ids

  tags = {
    Name    = "${var.project_name}-redis-subnet-group"
    Project = var.project_name
  }
}

// Redis 클러스터 생성
resource "aws_elasticache_cluster" "redis" {
  cluster_id           = "${var.project_name}-redis"
  engine               = "redis"
  node_type            = var.redis_node_type
  num_cache_nodes      = 1
  parameter_group_name = "default.redis7"
  port                 = 6379
  subnet_group_name    = aws_elasticache_subnet_group.redis.name
  security_group_ids   = [aws_security_group.redis_sg.id]

  tags = {
    Name    = "${var.project_name}-redis"
    Project = var.project_name
  }
}

// ECR 생성
resource "aws_ecr_repository" "app" {
  name                 = "${var.project_name}-be"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name    = "${var.project_name}-be"
    Project = var.project_name
  }
}

// ECR 레포지토리에 수명 정책을 추가해서 오래된 이미지 자동으로 정리
resource "aws_ecr_lifecycle_policy" "app" {
  repository = aws_ecr_repository.app.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep last 10 images"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 10
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

// EC2가 ECR pull 할 수 있게 IAM 권한 추가
resource "aws_iam_role_policy_attachment" "ec2_ecr_readonly" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}
