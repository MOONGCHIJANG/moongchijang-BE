package com.moongchijang.domain.user.domain.repository

import com.moongchijang.domain.user.domain.entity.WithdrawnParticipation
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface WithdrawnParticipationRepository : JpaRepository<WithdrawnParticipation, Long> {
    fun deleteByRetentionExpiresAtBefore(retentionExpiresAt: LocalDateTime): Long
}
