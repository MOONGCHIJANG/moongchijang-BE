package com.moongchijang.domain.groupbuy.application.port

import com.moongchijang.domain.groupbuy.application.event.GroupBuyIndexAction

interface GroupBuyIndexingEventPublisher {
    fun publishIndexRequested(
        groupBuyId: Long,
        action: GroupBuyIndexAction = GroupBuyIndexAction.UPSERT
    )
}
