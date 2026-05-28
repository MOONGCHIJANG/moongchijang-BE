package com.moongchijang.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "coolsms")
data class CoolSmsProperties(
    val apiKey: String,
    val apiSecret: String,
    val sender: String,
)