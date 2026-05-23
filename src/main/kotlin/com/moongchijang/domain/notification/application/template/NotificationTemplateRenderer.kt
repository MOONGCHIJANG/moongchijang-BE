package com.moongchijang.domain.notification.application.template

import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.springframework.stereotype.Component

@Component
class NotificationTemplateRenderer {
    fun render(template: NotificationTemplate, variables: Map<String, String>): NotificationTemplateRenderResult {
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
            throw CustomException(
                ErrorCode.NOTIFICATION_TEMPLATE_VARIABLE_MISSING,
                "알림 템플릿 변수 값이 누락되었습니다. missing=${missing.joinToString(",")}"
            )
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
