package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyOrderStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import org.springframework.data.domain.Page
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

interface GroupBuyRepository : JpaRepository<GroupBuy, Long>, GroupBuyRepositoryCustom {

    fun findByStatusInAndDeadlineLessThanEqual(
        statuses: Collection<GroupBuyStatus>,
        deadline: LocalDateTime,
        pageable: Pageable
    ): List<GroupBuy>

    fun findByStatusInAndDeadlineBetween(
        statuses: Collection<GroupBuyStatus>,
        deadlineFrom: LocalDateTime,
        deadlineTo: LocalDateTime
    ): List<GroupBuy>

    @Query(
        """
        select gb
        from GroupBuy gb
        join fetch gb.store
        where gb.status = :status
          and gb.deadline between :deadlineFrom and :deadlineTo
          and gb.currentQuantity < gb.targetQuantity
        """
    )
    fun findDeadlineRiskTargets(
        @Param("status") status: GroupBuyStatus,
        @Param("deadlineFrom") deadlineFrom: LocalDateTime,
        @Param("deadlineTo") deadlineTo: LocalDateTime
    ): List<GroupBuy>

    @Query(
        value = """
            select gb
            from GroupBuy gb
            join fetch gb.store
            where gb.status = :groupBuyStatus
              and gb.orderStatus in :orderStatuses
              and (:overdueBefore is null or gb.achievedAt < :overdueBefore)
            order by gb.achievedAt asc, gb.id asc
        """,
        countQuery = """
            select count(gb)
            from GroupBuy gb
            where gb.status = :groupBuyStatus
              and gb.orderStatus in :orderStatuses
              and (:overdueBefore is null or gb.achievedAt < :overdueBefore)
        """
    )
    fun findAdminOrderPage(
        @Param("groupBuyStatus") groupBuyStatus: GroupBuyStatus,
        @Param("orderStatuses") orderStatuses: Collection<GroupBuyOrderStatus>,
        @Param("overdueBefore") overdueBefore: LocalDateTime?,
        pageable: Pageable
    ): Page<GroupBuy>

    fun countByStatusAndOrderStatus(
        status: GroupBuyStatus,
        orderStatus: GroupBuyOrderStatus
    ): Long

    @Query(
        """
        select count(gb)
        from GroupBuy gb
        where gb.status = :status
          and gb.orderStatus = :orderStatus
          and gb.achievedAt < :overdueBefore
        """
    )
    fun countOverdueAdminOrders(
        @Param("status") status: GroupBuyStatus,
        @Param("orderStatus") orderStatus: GroupBuyOrderStatus,
        @Param("overdueBefore") overdueBefore: LocalDateTime
    ): Long

    @EntityGraph(attributePaths = ["store"])
    fun findWithStoreById(id: Long): Optional<GroupBuy>

    @Query("select gb from GroupBuy gb join fetch gb.store where gb.id = :id")
    fun findAdminOrderDetailById(@Param("id") id: Long): Optional<GroupBuy>

    fun findByStoreIdInAndStatusInOrderByDeadlineAsc(
        storeIds: Collection<Long>,
        statuses: Collection<GroupBuyStatus>
    ): List<GroupBuy>

    @EntityGraph(attributePaths = ["store"])
    fun findByStatusInAndPickupDate(
        statuses: Collection<GroupBuyStatus>,
        pickupDate: LocalDate
    ): List<GroupBuy>

    fun existsByStoreIdInAndStatusIn(
        storeIds: Collection<Long>,
        statuses: Collection<GroupBuyStatus>
    ): Boolean

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE GroupBuy gb
        SET gb.currentQuantity = gb.currentQuantity + :quantity
        WHERE gb.id = :groupBuyId
        AND gb.status IN :statuses
        AND gb.deadline > CURRENT_TIMESTAMP
        AND (gb.maxQuantity - gb.currentQuantity) >= :quantity
        """
    )
    fun increaseCurrentQuantityIfAvailable(
        @Param("groupBuyId") groupBuyId: Long,
        @Param("quantity") quantity: Int,
        @Param("statuses") statuses: Collection<GroupBuyStatus> = listOf(
            GroupBuyStatus.IN_PROGRESS,
            GroupBuyStatus.ACHIEVED
        )
    ): Int

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockById(id: Long): Optional<GroupBuy>

    @Query("SELECT DISTINCT gb.store.id FROM GroupBuy gb WHERE gb.store.id IN :storeIds")
    fun findStoreIdsWithGroupBuyHistory(@Param("storeIds") storeIds: Collection<Long>): List<Long>

    @Query(
        """
        select distinct gb.pickupDate
        from GroupBuy gb
        where gb.store.id in :storeIds
          and gb.status in :statuses
        order by gb.pickupDate desc
        """
    )
    fun findDistinctPickupDatesByStoreIdsAndStatuses(
        @Param("storeIds") storeIds: Collection<Long>,
        @Param("statuses") statuses: Collection<GroupBuyStatus>,
    ): List<LocalDate>
}
