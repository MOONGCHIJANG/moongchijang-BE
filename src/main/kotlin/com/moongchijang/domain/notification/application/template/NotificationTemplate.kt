package com.moongchijang.domain.notification.application.template

import com.moongchijang.domain.notification.domain.entity.NotificationDeeplinkType

data class NotificationTemplate(
    val type: NotificationTemplateType,
    val titleTemplate: String,
    val bodyTemplate: String,
    val deeplinkType: NotificationDeeplinkType,
)
