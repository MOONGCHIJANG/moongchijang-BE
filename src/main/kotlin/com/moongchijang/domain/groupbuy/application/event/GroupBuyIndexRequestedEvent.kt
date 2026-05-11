package com.moongchijang.domain.groupbuy.application.event

data class GroupBuyIndexRequestedEvent(
    val groupBuyId: Long,
    val action: GroupBuyIndexAction = GroupBuyIndexAction.UPSERT
)

enum class GroupBuyIndexAction {
    UPSERT,
    DELETE
}
