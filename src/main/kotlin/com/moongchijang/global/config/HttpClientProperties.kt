package com.moongchijang.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "http-client")
data class HttpClientProperties(
    val connectTimeoutMs: Long = 2_000,
    val readTimeoutMs: Long = 5_000,
)
