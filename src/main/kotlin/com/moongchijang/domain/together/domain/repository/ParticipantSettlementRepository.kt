package com.moongchijang.domain.together.domain.repository

import com.moongchijang.domain.together.domain.entity.ParticipantSettlement
import com.moongchijang.domain.together.domain.entity.ParticipantSettlementStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ParticipantSettlementRepository : JpaRepository<ParticipantSettlement, Long> {

    @Query(
        """
        SELECT s.room.id
        FROM ParticipantSettlement s
        WHERE s.participant.id = :userId
          AND s.status = :status
        """
    )
    fun findRoomIdsByParticipantIdAndStatus(
        @Param("userId") userId: Long,
        @Param("status") status: ParticipantSettlementStatus = ParticipantSettlementStatus.PENDING_TRANSFER,
    ): Set<Long>
}
