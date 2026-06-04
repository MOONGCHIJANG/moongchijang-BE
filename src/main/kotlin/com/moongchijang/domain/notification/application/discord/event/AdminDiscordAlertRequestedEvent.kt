package com.moongchijang.domain.notification.application.discord.event

import com.moongchijang.domain.notification.application.discord.AdminDiscordChannel

data class AdminDiscordAlertRequestedEvent(
    val channel: AdminDiscordChannel,
    val message: String,
)
