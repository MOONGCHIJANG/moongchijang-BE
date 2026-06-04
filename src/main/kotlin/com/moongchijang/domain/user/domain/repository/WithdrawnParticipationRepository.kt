package com.moongchijang.domain.user.domain.repository

import com.moongchijang.domain.user.domain.entity.WithdrawnParticipation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface WithdrawnParticipationRepository : JpaRepository<WithdrawnParticipation, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM WithdrawnParticipation p WHERE p.retentionExpiresAt < :retentionExpiresAt")
    fun deleteByRetentionExpiresAtBefore(@Param("retentionExpiresAt") retentionExpiresAt: LocalDateTime): Long
}
