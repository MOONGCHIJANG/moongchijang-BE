locals {
  ses_enabled = var.ses_domain != ""
}

resource "aws_ses_domain_identity" "domain" {
  count = local.ses_enabled ? 1 : 0

  domain = var.ses_domain
}

resource "aws_ses_domain_dkim" "domain" {
  count = local.ses_enabled ? 1 : 0

  domain = var.ses_domain
}
