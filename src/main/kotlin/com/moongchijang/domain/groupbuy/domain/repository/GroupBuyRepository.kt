package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface GroupBuyRepository : JpaRepository<GroupBuy, Long>, GroupBuyRepositoryCustom {

    @EntityGraph(attributePaths = ["store"])
    fun findWithStoreById(id: Long): Optional<GroupBuy>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE GroupBuy gb
        SET gb.currentQuantity = gb.currentQuantity + :quantity
        WHERE gb.id = :groupBuyId
        AND gb.status = :status
        AND gb.deadline > CURRENT_TIMESTAMP 
        AND (gb.maxQuantity - gb.currentQuantity) >= :quantity
        """
    )
    fun increaseCurrentQuantityIfAvailable(
        @Param("groupBuyId") groupBuyId: Long,
        @Param("quantity") quantity: Int,
        @Param("status") status: GroupBuyStatus = GroupBuyStatus.IN_PROGRESS
    ): Int

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockById(id: Long): Optional<GroupBuy>
}
