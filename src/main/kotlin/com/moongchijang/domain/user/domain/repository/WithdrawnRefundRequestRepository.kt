package com.moongchijang.domain.user.domain.repository

import com.moongchijang.domain.user.domain.entity.WithdrawnRefundRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface WithdrawnRefundRequestRepository : JpaRepository<WithdrawnRefundRequest, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM WithdrawnRefundRequest r WHERE r.retentionExpiresAt < :retentionExpiresAt")
    fun deleteByRetentionExpiresAtBefore(@Param("retentionExpiresAt") retentionExpiresAt: LocalDateTime): Long
}
