output "s3_bucket_name" {
  description = "Created S3 bucket name"
  value       = aws_s3_bucket.terraform_test_bucket.bucket
}
