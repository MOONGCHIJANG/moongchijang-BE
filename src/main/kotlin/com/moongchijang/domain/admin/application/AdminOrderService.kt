package com.moongchijang.domain.admin.application

import com.moongchijang.domain.admin.application.dto.AdminOrderDetailResponse
import com.moongchijang.domain.admin.application.dto.AdminOrderPageResponse
import com.moongchijang.domain.admin.application.dto.AdminOrderStatusFilter
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyOrderStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class AdminOrderService(
    private val groupBuyRepository: GroupBuyRepository,
    private val participationRepository: ParticipationRepository,
    private val clock: Clock,
) {

    fun getOrders(
        status: AdminOrderStatusFilter,
        pageable: Pageable
    ): AdminOrderPageResponse {
        val now = LocalDateTime.now(clock)
        val page = groupBuyRepository.findAdminOrderPage(
            groupBuyStatus = GroupBuyStatus.ACHIEVED,
            orderStatuses = status.toOrderStatuses(),
            overdueBefore = status.overdueBefore(now),
            pageable = pageable
        )
        val groupBuyIds = page.content.map { it.id }
        val pendingRefundCountsByGroupBuyId = if (groupBuyIds.isEmpty()) {
            emptyMap()
        } else {
            participationRepository.countPendingRefundsByGroupBuyIdIn(groupBuyIds)
                .associate { it.groupBuyId to it.pendingRefundCount }
        }

        return AdminOrderPageResponse.from(page, pendingRefundCountsByGroupBuyId, now)
    }

    fun getOrderDetail(orderId: Long): AdminOrderDetailResponse {
        val now = LocalDateTime.now(clock)
        val groupBuy = groupBuyRepository.findAdminOrderDetailById(orderId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_NOT_FOUND) }

        return AdminOrderDetailResponse.from(
            groupBuy = groupBuy,
            pendingRefundCount = countPendingRefunds(groupBuy.id),
            now = now
        )
    }

    @Transactional
    fun markOwnerContacted(orderId: Long): AdminOrderDetailResponse {
        val now = LocalDateTime.now(clock)
        val groupBuy = findOrderForUpdate(orderId)
        validatePendingOrder(groupBuy)
        groupBuy.markOrderOwnerContacted(now)

        return AdminOrderDetailResponse.from(groupBuy, countPendingRefunds(groupBuy.id), now)
    }

    @Transactional
    fun confirmOrder(orderId: Long): AdminOrderDetailResponse {
        val now = LocalDateTime.now(clock)
        val groupBuy = findOrderForUpdate(orderId)
        validatePendingOrder(groupBuy)
        groupBuy.confirmOrder(now)

        return AdminOrderDetailResponse.from(groupBuy, countPendingRefunds(groupBuy.id), now)
    }

    @Transactional
    fun cancelOrder(orderId: Long): AdminOrderDetailResponse {
        val now = LocalDateTime.now(clock)
        val groupBuy = findOrderForUpdate(orderId)
        validatePendingOrder(groupBuy)
        groupBuy.cancelOrder(now)

        return AdminOrderDetailResponse.from(groupBuy, countPendingRefunds(groupBuy.id), now)
    }

    private fun findOrderForUpdate(orderId: Long): GroupBuy {
        val groupBuy = groupBuyRepository.findWithLockById(orderId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_NOT_FOUND) }
        validateAdminOrder(groupBuy)
        return groupBuy
    }

    private fun validateAdminOrder(groupBuy: GroupBuy) {
        if (groupBuy.status != GroupBuyStatus.ACHIEVED) {
            throw CustomException(ErrorCode.GROUPBUY_ORDER_INVALID_STATUS, "groupBuyStatus=${groupBuy.status}")
        }
    }

    private fun validatePendingOrder(groupBuy: GroupBuy) {
        if (groupBuy.orderStatus != GroupBuyOrderStatus.PENDING) {
            throw CustomException(ErrorCode.GROUPBUY_ORDER_INVALID_STATUS, "orderStatus=${groupBuy.orderStatus}")
        }
    }

    private fun countPendingRefunds(groupBuyId: Long): Long =
        participationRepository.countByGroupBuyIdAndStatusIn(
            groupBuyId = groupBuyId,
            statuses = listOf(ParticipationStatus.REFUND_PENDING)
        )

    private fun AdminOrderStatusFilter.toOrderStatuses(): List<GroupBuyOrderStatus> =
        when (this) {
            AdminOrderStatusFilter.OVERDUE_48H,
            AdminOrderStatusFilter.PENDING -> listOf(GroupBuyOrderStatus.PENDING)
            AdminOrderStatusFilter.CONFIRMED -> listOf(GroupBuyOrderStatus.CONFIRMED)
            AdminOrderStatusFilter.ALL -> GroupBuyOrderStatus.entries
        }

    private fun AdminOrderStatusFilter.overdueBefore(now: LocalDateTime): LocalDateTime? =
        when (this) {
            AdminOrderStatusFilter.OVERDUE_48H -> now.minusHours(48)
            AdminOrderStatusFilter.PENDING,
            AdminOrderStatusFilter.CONFIRMED,
            AdminOrderStatusFilter.ALL -> null
        }
}
