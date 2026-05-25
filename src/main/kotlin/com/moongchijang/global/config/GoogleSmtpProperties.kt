package com.moongchijang.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "google.smtp")
data class GoogleSmtpProperties(
    val host: String = "smtp.gmail.com",
    val port: Int = 587,
    val username: String = "",
    val appPassword: String = "",
    val from: String = "",
)
