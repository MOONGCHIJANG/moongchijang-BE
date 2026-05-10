package com.moongchijang.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ses")
data class SesProperties(
    val enabled: Boolean,
    val region: String,
    val fromEmail: String,
)
