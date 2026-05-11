package com.moongchijang.domain.notification.infrastructure.aligo

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aligo")
data class AligoProperties(
    val apiKey: String,
    val userId: String,
    val senderKey: String,
    val templateCode: String,
    val sender: String
)
