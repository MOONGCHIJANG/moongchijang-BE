package com.moongchijang.domain.groupbuy.infrastructure.event

import com.moongchijang.domain.groupbuy.application.event.GroupBuyIndexAction
import com.moongchijang.domain.groupbuy.application.event.GroupBuyIndexRequestedEvent
import com.moongchijang.domain.groupbuy.application.port.GroupBuyIndexingEventPublisher
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "indexing", name = ["publisher"], havingValue = "spring", matchIfMissing = true)
class SpringGroupBuyIndexingEventPublisher(
    private val eventPublisher: ApplicationEventPublisher
) : GroupBuyIndexingEventPublisher {
    override fun publishIndexRequested(groupBuyId: Long, action: GroupBuyIndexAction) {
        eventPublisher.publishEvent(GroupBuyIndexRequestedEvent(groupBuyId, action))
    }
}
