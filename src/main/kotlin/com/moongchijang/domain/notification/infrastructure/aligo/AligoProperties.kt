package com.moongchijang.domain.notification.infrastructure.aligo

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aligo")
data class AligoProperties(
    val apiKey: String = "",
    val userId: String = "",
    val senderKey: String = "",
    val templateCodeGroupBuyOpenSuccess: String = "",
    val templateCodeGroupBuyOpenFailed: String = "",
    val templateCodePickupD1Reminder: String = "",
    val templateCodePickupDayReminder: String = "",
    val sender: String = "",
)
