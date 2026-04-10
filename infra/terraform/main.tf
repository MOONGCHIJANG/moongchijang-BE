resource "aws_s3_bucket" "terraform_test_bucket" {
  bucket = var.bucket_name

  tags = {
    Name    = "${var.project_name}-terraform-test"
    Project = var.project_name
  }
}
