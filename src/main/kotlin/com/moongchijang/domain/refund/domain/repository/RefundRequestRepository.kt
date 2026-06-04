package com.moongchijang.domain.refund.domain.repository

import com.moongchijang.domain.refund.domain.entity.RefundRequest
import org.springframework.data.jpa.repository.JpaRepository

interface RefundRequestRepository : JpaRepository<RefundRequest, Long> {
    fun findAllByParticipationUserId(userId: Long): List<RefundRequest>

    fun findByParticipationId(participationId: Long): RefundRequest?
}
