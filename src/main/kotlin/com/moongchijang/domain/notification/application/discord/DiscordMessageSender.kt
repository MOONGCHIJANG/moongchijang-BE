package com.moongchijang.domain.notification.application.discord

interface DiscordMessageSender {
    fun send(channel: AdminDiscordChannel, message: String): Boolean
}
