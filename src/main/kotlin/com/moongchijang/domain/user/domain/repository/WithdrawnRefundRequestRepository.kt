package com.moongchijang.domain.user.domain.repository

import com.moongchijang.domain.user.domain.entity.WithdrawnRefundRequest
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface WithdrawnRefundRequestRepository : JpaRepository<WithdrawnRefundRequest, Long> {
    fun deleteByRetentionExpiresAtBefore(retentionExpiresAt: LocalDateTime): Long
}
