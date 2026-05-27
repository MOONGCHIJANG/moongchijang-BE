package com.moongchijang.domain.admin.application

import com.moongchijang.domain.admin.application.dto.AdminDashboardUnconfirmedOrderResponse
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
class AdminDashboardOrderMonitoringService(
    private val groupBuyRepository: GroupBuyRepository,
    private val participationRepository: ParticipationRepository,
    private val clock: Clock,
) {

    fun getUnconfirmedOrders(pageable: Pageable): AdminDashboardUnconfirmedOrderResponse {
        val now = LocalDateTime.now(clock)
        val overdueBefore = now.minusHours(48)
        val page = groupBuyRepository.findAdminOrderPage(
            groupBuyStatus = GroupBuyStatus.ACHIEVED,
            orderStatuses = listOf(GroupBuyOrderStatus.PENDING),
            overdueBefore = null,
            pageable = pageable
        )
        val groupBuyIds = page.content.map { it.id }
        val pendingRefundCountsByGroupBuyId = if (groupBuyIds.isEmpty()) {
            emptyMap()
        } else {
            participationRepository.countPendingRefundsByGroupBuyIdIn(groupBuyIds)
                .associate { it.groupBuyId to it.pendingRefundCount }
        }
        val overdueCount = groupBuyRepository.countOverdueAdminOrders(
            status = GroupBuyStatus.ACHIEVED,
            orderStatus = GroupBuyOrderStatus.PENDING,
            overdueBefore = overdueBefore
        )

        return AdminDashboardUnconfirmedOrderResponse.from(
            page = page,
            pendingRefundCountsByGroupBuyId = pendingRefundCountsByGroupBuyId,
            totalUnconfirmedCount = page.totalElements,
            overdueCount = overdueCount,
            now = now,
            overdueBefore = overdueBefore
        )
    }
}
