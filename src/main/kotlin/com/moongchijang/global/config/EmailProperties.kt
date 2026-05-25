package com.moongchijang.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "email")
data class EmailProperties(
    val provider: EmailProvider = EmailProvider.SES,
)

enum class EmailProvider {
    SES,
    GOOGLE,
}
