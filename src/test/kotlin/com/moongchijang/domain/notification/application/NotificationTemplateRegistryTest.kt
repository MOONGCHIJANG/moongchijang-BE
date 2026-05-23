package com.moongchijang.domain.notification.application

import com.moongchijang.domain.notification.application.template.NotificationTemplateRegistry
import com.moongchijang.domain.notification.domain.entity.NotificationTriggerType
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class NotificationTemplateRegistryTest {

    private val registry = NotificationTemplateRegistry()

    @Test
    fun `모든 알림 트리거 타입은 템플릿 매핑 존재`() {
        NotificationTriggerType.entries.forEach { triggerType ->
            val template = registry.getTemplateByTriggerType(triggerType)
            assertNotNull(template)
        }
    }
}
