package com.moongchijang.domain.notification.infrastructure.discord

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "discord")
data class DiscordProperties(
    val enabled: Boolean = false,
    val paymentSuccessAlertEnabled: Boolean = false,
    val webhook: Webhook = Webhook(),
) {
    data class Webhook(
        val refund: String = "",
        val payment: String = "",
        val groupbuy: String = "",
        val onboarding: String = "",
    )
}
