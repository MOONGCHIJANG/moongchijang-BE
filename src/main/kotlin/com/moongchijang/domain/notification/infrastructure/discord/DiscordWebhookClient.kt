package com.moongchijang.domain.notification.infrastructure.discord

import com.moongchijang.domain.notification.application.discord.AdminDiscordChannel
import com.moongchijang.domain.notification.application.discord.DiscordMessageSender
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class DiscordWebhookClient(
    restClientBuilder: RestClient.Builder,
    private val discordProperties: DiscordProperties,
) : DiscordMessageSender {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient: RestClient = restClientBuilder.build()

    override fun send(channel: AdminDiscordChannel, message: String): Boolean {
        if (!discordProperties.enabled) {
            log.debug("[DiscordWebhookClient] 비활성화 상태로 전송을 건너뜁니다: channel={}", channel)
            return false
        }

        val webhookUrl = resolveWebhookUrl(channel)
        if (webhookUrl.isBlank()) {
            log.warn("[DiscordWebhookClient] webhook URL이 비어있습니다.: channel={}", channel)
            return false
        }

        val payload = mapOf("content" to message)
        return try {
            restClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity()
            true
        } catch (e: Exception) {
            log.error("[DiscordWebhookClient] send failed: channel={}", channel, e)
            false
        }
    }

    private fun resolveWebhookUrl(channel: AdminDiscordChannel): String =
        when (channel) {
            AdminDiscordChannel.REFUND -> discordProperties.webhook.refund
            AdminDiscordChannel.PAYMENT -> discordProperties.webhook.payment
            AdminDiscordChannel.GROUPBUY -> discordProperties.webhook.groupbuy
            AdminDiscordChannel.ONBOARDING -> discordProperties.webhook.onboarding
        }.trim()
}
