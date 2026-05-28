output "s3_bucket_name" {
  description = "Created S3 bucket name"
  value       = aws_s3_bucket.terraform_test_bucket.bucket
}

output "ec2_public_ip" {
  description = "Public IP of the EC2 instance"
  value       = aws_instance.app_server.public_ip
}

output "rds_endpoint" {
  description = "RDS endpoint"
  value       = aws_db_instance.main.endpoint
}

output "redis_endpoint" {
  description = "Redis endpoint"
  value       = aws_elasticache_cluster.redis.cache_nodes[0].address
}

output "ecr_repository_url" {
  value = aws_ecr_repository.app.repository_url
}

output "ses_identity_arn" {
  description = "SES domain identity ARN (null when SES is disabled)"
  value       = try(aws_ses_domain_identity.domain[0].arn, null)
}

output "ses_verification_txt_name" {
  description = "DNS TXT record name for SES identity verification"
  value       = var.ses_domain != "" ? "_amazonses.${var.ses_domain}" : null
}

output "ses_verification_txt_value" {
  description = "DNS TXT record value for SES identity verification"
  value       = try(aws_ses_domain_identity.domain[0].verification_token, null)
}

output "ses_dkim_cname_records" {
  description = "DNS CNAME records for SES DKIM verification"
  value = var.ses_domain != "" ? [
    for token in aws_ses_domain_dkim.domain[0].dkim_tokens : {
      name  = "${token}._domainkey.${var.ses_domain}"
      type  = "CNAME"
      value = "${token}.dkim.amazonses.com"
    }
  ] : []
}
