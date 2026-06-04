package com.moongchijang.domain.admin.application

import com.moongchijang.domain.admin.application.dto.AdminDashboardUrgentRefundItemResponse
import com.moongchijang.domain.admin.application.dto.AdminDashboardUrgentRefundResponse
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
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
class AdminDashboardUrgentRefundService(
    private val participationRepository: ParticipationRepository,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getUrgentRefunds(pageable: Pageable): AdminDashboardUrgentRefundResponse {
        val now = clock.kstNow()
        val requestedBefore = now.minusHours(URGENT_REFUND_THRESHOLD_HOURS)
        log.info(
            "[AdminDashboardUrgentRefundService] 긴급 환불 요청 조회 시작: requestedBefore={}, page={}, size={}",
            requestedBefore,
            pageable.pageNumber,
            pageable.pageSize,
        )

        val page = participationRepository.findDashboardUrgentRefundRequests(
            status = ParticipationStatus.REFUND_PENDING,
            requestedBefore = requestedBefore,
            pageable = pageable,
        )
        val content = page.content.map { AdminDashboardUrgentRefundItemResponse.from(it, now) }

        val response = AdminDashboardUrgentRefundResponse(
            totalUrgentCount = page.totalElements,
            hasUrgentRefunds = page.totalElements > 0,
            content = content,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            number = page.number,
            size = page.size,
        )
        log.info(
            "[AdminDashboardUrgentRefundService] 긴급 환불 요청 조회 완료: totalElements={}",
            response.totalElements,
        )
        return response
    }

    private companion object {
        const val URGENT_REFUND_THRESHOLD_HOURS = 1L
    }
}
