package com.moongchijang.domain.admin.application

import com.moongchijang.domain.admin.application.dto.AdminOrderPageResponse
import com.moongchijang.domain.admin.application.dto.AdminOrderStatusFilter
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyOrderStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
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
