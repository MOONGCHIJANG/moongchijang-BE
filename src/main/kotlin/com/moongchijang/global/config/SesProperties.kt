package com.moongchijang.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ses")
data class SesProperties(
    val enabled: Boolean = false,
    val region: String = "ap-northeast-2",
    val fromEmail: String = "",
)
