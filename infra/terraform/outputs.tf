output "s3_bucket_name" {
  description = "Created S3 bucket name"
  value       = aws_s3_bucket.terraform_test_bucket.bucket
}

output "ec2_public_ip" {
  description = "Public IP of the EC2 instance"
  value       = aws_instance.app_server.public_ip
}
