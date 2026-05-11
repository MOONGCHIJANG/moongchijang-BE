package com.moongchijang.domain.search.application.dto

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import java.time.LocalDateTime

data class GroupBuyCardDto(
    val id: Long,
    val storeName: String,
    val region: String,
    val productName: String,
    val thumbnailUrl: String?,
    val price: Int,
    val currentQuantity: Int,
    val targetQuantity: Int,
    val deadline: LocalDateTime
) {
    companion object {
        fun from(groupBuy: GroupBuy) = GroupBuyCardDto(
            id = groupBuy.id,
            storeName = groupBuy.store.name,
            region = groupBuy.store.region.label,
            productName = groupBuy.productName,
            thumbnailUrl = groupBuy.thumbnailUrl,
            price = groupBuy.price,
            currentQuantity = groupBuy.currentQuantity,
            targetQuantity = groupBuy.targetQuantity,
            deadline = groupBuy.deadline
        )
    }
}
