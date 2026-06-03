package com.moongchijang.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "portone")
data class PortOneProperties(
    val storeId: String,
    val channelKey: String,
    val apiSecret: String,
    val paymentApiBaseUrl: String = "https://api.portone.io",
    val webhookSecret: String? = null,
    val webhookTimestampToleranceSeconds: Long = 300,
)
