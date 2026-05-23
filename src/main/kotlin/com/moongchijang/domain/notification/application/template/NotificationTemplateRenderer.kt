package com.moongchijang.domain.notification.application.template

import org.springframework.stereotype.Component

@Component
class NotificationTemplateRenderer(
    private val notificationTemplateRegistry: NotificationTemplateRegistry,
) {
    fun render(templateType: NotificationTemplateType, variables: Map<String, String>): NotificationTemplateRenderResult {
        val template = notificationTemplateRegistry.getTemplateByType(templateType)
        return NotificationTemplateRenderResult(
            title = renderText(template.titleTemplate, variables),
            body = renderText(template.bodyTemplate, variables),
            deeplinkType = template.deeplinkType,
        )
    }

    private fun renderText(templateText: String, variables: Map<String, String>): String {
        val placeholders = PLACEHOLDER_PATTERN.findAll(templateText).map { it.groupValues[1] }.toSet()
        val missing = placeholders.filterNot { variables[it]?.isNotBlank() == true }
        if (missing.isNotEmpty()) {
            throw IllegalArgumentException("알림 템플릿 변수 누락: ${missing.joinToString(",")}")
        }

        var rendered = templateText
        placeholders.forEach { placeholder ->
            rendered = rendered.replace("{$placeholder}", variables.getValue(placeholder))
        }
        return rendered
    }

    companion object {
        private val PLACEHOLDER_PATTERN = Regex("\\{([^{}]+)}")
    }
}
