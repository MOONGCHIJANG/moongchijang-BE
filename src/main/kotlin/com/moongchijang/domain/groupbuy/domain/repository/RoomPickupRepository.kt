package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.domain.entity.RoomPickup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface RoomPickupRepository : JpaRepository<RoomPickup, Long> {
    @Query(
        """
        SELECT pickup.groupBuy.id
        FROM RoomPickup pickup
        WHERE pickup.groupBuy.id IN :groupBuyIds
          AND pickup.usedAt IS NULL
          AND pickup.activeFrom <= :now
        """
    )
    fun findActiveGroupBuyIds(
        @Param("groupBuyIds") groupBuyIds: Collection<Long>,
        @Param("now") now: LocalDateTime,
    ): Set<Long>
}
