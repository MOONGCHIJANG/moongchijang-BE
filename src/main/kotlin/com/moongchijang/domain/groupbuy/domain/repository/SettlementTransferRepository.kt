package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.domain.entity.SettlementTransfer
import com.moongchijang.domain.groupbuy.domain.entity.SettlementTransferStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SettlementTransferRepository : JpaRepository<SettlementTransfer, Long> {
    @Query(
        """
        SELECT transfer.roomSettlement.groupBuy.id
        FROM SettlementTransfer transfer
        WHERE transfer.participation.user.id = :userId
          AND transfer.status = :status
        """
    )
    fun findGroupBuyIdsByParticipantIdAndStatus(
        @Param("userId") userId: Long,
        @Param("status") status: SettlementTransferStatus = SettlementTransferStatus.PENDING,
    ): Set<Long>
}
