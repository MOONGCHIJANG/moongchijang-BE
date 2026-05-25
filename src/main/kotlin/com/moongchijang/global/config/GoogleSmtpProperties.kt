package com.moongchijang.global.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "google.smtp")
data class GoogleSmtpProperties(
    val host: String = "smtp.gmail.com",
    val port: Int = 587,
    @field:NotBlank val username: String = "",
    @field:NotBlank val appPassword: String = "",
    @field:NotBlank val from: String = "",
    val connectionTimeout: Int = 5000,
    val timeout: Int = 5000,
    val writeTimeout: Int = 5000,
)
