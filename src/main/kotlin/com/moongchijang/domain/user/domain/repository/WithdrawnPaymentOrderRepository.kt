package com.moongchijang.domain.user.domain.repository

import com.moongchijang.domain.user.domain.entity.WithdrawnPaymentOrder
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface WithdrawnPaymentOrderRepository : JpaRepository<WithdrawnPaymentOrder, Long> {
    fun deleteByRetentionExpiresAtBefore(retentionExpiresAt: LocalDateTime): Long
}
