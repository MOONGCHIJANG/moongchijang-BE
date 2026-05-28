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

    /**
     * ngram FULLTEXT 인덱스로 매칭되는 공구 id 만 반환한다.
     *
     * status/deadline 필터를 각 UNION 분기 안에 둬서 매칭 후 집계되는 임시 테이블 크기를 줄인다.
     * store fetch 는 [findAllWithStoreByIdIn] 으로 별도 수행해 native + lazy 조합의 N+1 을 회피한다.
     *
     * @param query   FullTextQueryBuilder.toBooleanQuery 로 변환된 BOOLEAN MODE 쿼리. 빈 문자열이면 빈 결과.
     * @param status  노출 허용 상태 (보통 IN_PROGRESS)
     * @param now     마감 비교 기준 시각 (deadline > now 인 공구만 반환)
     * @param limit   반환할 최대 결과 수
     */
    @Query(
        value = """
            SELECT id FROM (
                SELECT gb.id, gb.deadline FROM group_buys gb
                WHERE MATCH(gb.product_name) AGAINST(:query IN BOOLEAN MODE)
                  AND gb.status = :status
                  AND gb.deadline > :now
                UNION
                SELECT gb.id, gb.deadline FROM group_buys gb
                JOIN stores s ON gb.store_id = s.id
                WHERE MATCH(s.name, s.address) AGAINST(:query IN BOOLEAN MODE)
                  AND gb.status = :status
                  AND gb.deadline > :now
            ) AS merged
            ORDER BY merged.deadline ASC
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun searchIdsByFullText(
        @Param("query") query: String,
        @Param("status") status: String,
        @Param("now") now: LocalDateTime,
        @Param("limit") limit: Int,
    ): List<Long>

    /**
     * 주어진 id 목록의 GroupBuy 를 store 와 함께 한 번에 로드한다.
     * [searchIdsByFullText] 후속 호출 용도. 빈 id 목록은 호출자가 가드한다.
     */
    @Query("SELECT gb FROM GroupBuy gb JOIN FETCH gb.store WHERE gb.id IN :ids")
    fun findAllWithStoreByIdIn(@Param("ids") ids: Collection<Long>): List<GroupBuy>

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
