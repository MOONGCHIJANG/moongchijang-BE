package com.moongchijang.domain.notification.application.discord

import com.moongchijang.domain.notification.application.discord.event.AdminDiscordAlertRequestedEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AdminDiscordAlertEventListener(
    private val discordMessageSender: DiscordMessageSender,
) {

    @Async("notificationEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun on(event: AdminDiscordAlertRequestedEvent) {
        discordMessageSender.send(event.channel, event.message)
    }
}
