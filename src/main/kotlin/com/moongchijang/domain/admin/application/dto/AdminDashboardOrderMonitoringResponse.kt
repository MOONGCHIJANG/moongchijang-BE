package com.moongchijang.domain.admin.application.dto

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import org.springframework.data.domain.Page
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

data class AdminDashboardUnconfirmedOrderResponse(
    val totalUnconfirmedCount: Long,
    val overdueCount: Long,
    val hasOverdue: Boolean,
    val content: List<AdminDashboardUnconfirmedOrderItemResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int,
) {
    companion object {
        fun from(
            page: Page<GroupBuy>,
            pendingRefundCountsByGroupBuyId: Map<Long, Long>,
            totalUnconfirmedCount: Long,
            overdueCount: Long,
            now: LocalDateTime,
            overdueBefore: LocalDateTime,
        ): AdminDashboardUnconfirmedOrderResponse =
            AdminDashboardUnconfirmedOrderResponse(
                totalUnconfirmedCount = totalUnconfirmedCount,
                overdueCount = overdueCount,
                hasOverdue = overdueCount > 0,
                content = page.content.map {
                    AdminDashboardUnconfirmedOrderItemResponse.from(
                        groupBuy = it,
                        pendingRefundCount = pendingRefundCountsByGroupBuyId[it.id] ?: 0L,
                        now = now,
                        overdueBefore = overdueBefore
                    )
                },
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                number = page.number,
                size = page.size
            )
    }
}

data class AdminDashboardUnconfirmedOrderItemResponse(
    val orderId: Long,
    val groupBuyId: Long,
    val productName: String,
    val storeName: String,
    val achievedAt: LocalDateTime?,
    val finalQuantity: Int,
    val pendingRefundCount: Long,
    val pickupDate: LocalDate,
    val elapsedHours: Long,
    val overdue: Boolean,
    val progressRate: Int,
    val ownerContacted: Boolean,
    val ownerContactedAt: LocalDateTime?,
) {
    companion object {
        fun from(
            groupBuy: GroupBuy,
            pendingRefundCount: Long,
            now: LocalDateTime,
            overdueBefore: LocalDateTime,
        ): AdminDashboardUnconfirmedOrderItemResponse {
            val achievedAt = groupBuy.orderAchievedAt()
            return AdminDashboardUnconfirmedOrderItemResponse(
                orderId = groupBuy.id,
                groupBuyId = groupBuy.id,
                productName = groupBuy.productName,
                storeName = groupBuy.store.name,
                achievedAt = achievedAt,
                finalQuantity = groupBuy.currentQuantity,
                pendingRefundCount = pendingRefundCount,
                pickupDate = groupBuy.pickupDate,
                elapsedHours = achievedAt?.let { Duration.between(it, now).toHours().coerceAtLeast(0) } ?: 0L,
                overdue = achievedAt?.isBefore(overdueBefore) == true,
                progressRate = calculateAchievementRate(groupBuy),
                ownerContacted = groupBuy.orderOwnerContactedAt != null,
                ownerContactedAt = groupBuy.orderOwnerContactedAt
            )
        }

        private fun calculateAchievementRate(groupBuy: GroupBuy): Int {
            if (groupBuy.targetQuantity <= 0) {
                return 0
            }

            return groupBuy.currentQuantity * 100 / groupBuy.targetQuantity
        }
    }
}
