package com.moongchijang.domain.admin.application.dto

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyOrderStatus
import org.springframework.data.domain.Page
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

enum class AdminOrderStatusFilter {
    OVERDUE_48H,
    PENDING,
    CONFIRMED,
    ALL
}

data class AdminOrderPageResponse(
    val content: List<AdminOrderListItemResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int,
) {
    companion object {
        fun from(
            page: Page<GroupBuy>,
            pendingRefundCountsByGroupBuyId: Map<Long, Long>,
            now: LocalDateTime,
        ): AdminOrderPageResponse =
            AdminOrderPageResponse(
                content = page.content.map {
                    AdminOrderListItemResponse.from(
                        groupBuy = it,
                        pendingRefundCount = pendingRefundCountsByGroupBuyId[it.id] ?: 0L,
                        now = now
                    )
                },
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                number = page.number,
                size = page.size
            )
    }
}

data class AdminOrderListItemResponse(
    val orderId: Long,
    val groupBuyId: Long,
    val productName: String,
    val storeName: String,
    val achievedAt: LocalDateTime?,
    val finalQuantity: Int,
    val pendingRefundCount: Long,
    val pickupDate: LocalDate,
    val elapsedHours: Long,
    val progressRate: Int,
    val orderStatus: GroupBuyOrderStatus,
    val ownerContactedAt: LocalDateTime?,
    val orderConfirmedAt: LocalDateTime?,
    val actionable: Boolean,
) {
    companion object {
        fun from(
            groupBuy: GroupBuy,
            pendingRefundCount: Long,
            now: LocalDateTime
        ): AdminOrderListItemResponse {
            val achievedAt = groupBuy.orderAchievedAt()
            return AdminOrderListItemResponse(
                orderId = groupBuy.id,
                groupBuyId = groupBuy.id,
                productName = groupBuy.productName,
                storeName = groupBuy.store.name,
                achievedAt = achievedAt,
                finalQuantity = groupBuy.currentQuantity,
                pendingRefundCount = pendingRefundCount,
                pickupDate = groupBuy.pickupDate,
                elapsedHours = achievedAt?.let { Duration.between(it, now).toHours().coerceAtLeast(0) } ?: 0L,
                progressRate = achievementRate(groupBuy),
                orderStatus = groupBuy.orderStatus,
                ownerContactedAt = groupBuy.orderOwnerContactedAt,
                orderConfirmedAt = groupBuy.orderConfirmedAt,
                actionable = groupBuy.orderStatus == GroupBuyOrderStatus.PENDING
            )
        }

        private fun achievementRate(groupBuy: GroupBuy): Int {
            if (groupBuy.targetQuantity <= 0) {
                return 0
            }

            return groupBuy.currentQuantity * 100 / groupBuy.targetQuantity
        }
    }
}

fun GroupBuy.orderAchievedAt(): LocalDateTime? =
    achievedAt ?: updatedAt ?: createdAt
