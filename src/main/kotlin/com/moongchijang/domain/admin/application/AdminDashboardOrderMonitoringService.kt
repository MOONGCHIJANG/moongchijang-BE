package com.moongchijang.domain.admin.application

import com.moongchijang.domain.admin.application.dto.AdminDashboardUnconfirmedOrderResponse
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyOrderStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.global.time.kstNow
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(AdminDashboardOrderMonitoringService::class.java)

    fun getUnconfirmedOrders(pageable: Pageable): AdminDashboardUnconfirmedOrderResponse {
        log.info(
            "[AdminDashboardOrderMonitoringService] 미확정 주문 조회 시작: page={}, size={}",
            pageable.pageNumber,
            pageable.pageSize,
        )
        val now = clock.kstNow()
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

        val response = AdminDashboardUnconfirmedOrderResponse.from(
            page = page,
            pendingRefundCountsByGroupBuyId = pendingRefundCountsByGroupBuyId,
            totalUnconfirmedCount = page.totalElements,
            overdueCount = overdueCount,
            now = now,
            overdueBefore = overdueBefore
        )
        log.info(
            "[AdminDashboardOrderMonitoringService] 미확정 주문 조회 완료: totalUnconfirmedCount={}, overdueCount={}",
            response.totalUnconfirmedCount,
            response.overdueCount,
        )
        return response
    }
}
