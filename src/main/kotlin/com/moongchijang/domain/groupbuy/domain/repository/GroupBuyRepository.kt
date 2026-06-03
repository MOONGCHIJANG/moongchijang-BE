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
     * 상품명 FULLTEXT 인덱스(`idx_group_buys_product_name`) 매칭 공구 id 를 deadline ASC 로 반환한다.
     *
     * 인덱스 단위 매칭 여부를 호출 측에서 직접 확인할 수 있도록 매장/주소 인덱스와 분리한다.
     * store fetch 는 [findAllWithStoreByIdIn] 으로 별도 수행해 native + lazy 조합의 N+1 을 회피한다.
     *
     * @param query   FullTextQueryBuilder.toBooleanQuery 로 변환된 BOOLEAN MODE 쿼리. 빈 문자열이면 빈 결과.
     * @param status  노출 허용 상태 (보통 IN_PROGRESS)
     * @param now     마감 비교 기준 시각 (deadline > now 인 공구만 반환)
     * @param limit   반환할 최대 결과 수
     */
    @Query(
        value = """
            SELECT gb.id FROM group_buys gb
            WHERE MATCH(gb.product_name) AGAINST(:query IN BOOLEAN MODE)
              AND gb.status = :status
              AND gb.deadline > :now
            ORDER BY gb.deadline ASC
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun searchProductIdsByFullText(
        @Param("query") query: String,
        @Param("status") status: String,
        @Param("now") now: LocalDateTime,
        @Param("limit") limit: Int,
    ): List<Long>

    /**
     * 매장 FULLTEXT 인덱스(`idx_stores_name_address`) 매칭 공구 id 를 deadline ASC 로 반환한다.
     *
     * 인덱스 단위 매칭 여부를 호출 측에서 직접 확인할 수 있도록 상품명 인덱스와 분리한다.
     *
     * @param query   FullTextQueryBuilder.toBooleanQuery 로 변환된 BOOLEAN MODE 쿼리. 빈 문자열이면 빈 결과.
     * @param status  노출 허용 상태 (보통 IN_PROGRESS)
     * @param now     마감 비교 기준 시각 (deadline > now 인 공구만 반환)
     * @param limit   반환할 최대 결과 수
     */
    @Query(
        value = """
            SELECT gb.id FROM group_buys gb
            JOIN stores s ON gb.store_id = s.id
            WHERE MATCH(s.name, s.address) AGAINST(:query IN BOOLEAN MODE)
              AND gb.status = :status
              AND gb.deadline > :now
            ORDER BY gb.deadline ASC
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun searchStoreIdsByFullText(
        @Param("query") query: String,
        @Param("status") status: String,
        @Param("now") now: LocalDateTime,
        @Param("limit") limit: Int,
    ): List<Long>

    /**
     * 검색 보정 fallback 에서 사용할 현재 피드 검색 후보를 반환한다.
     *
     * FULLTEXT 검색 대상과 동일하게 진행 중이고 마감 전인 공구의 상품명, 매장명, 주소를 후보로 제한한다.
     * 후보 전체 로딩을 피하기 위해 호출 측에서 limit 을 지정한다.
     */
    @Query(
        value = """
            SELECT keyword
            FROM (
                SELECT gb.product_name AS keyword, gb.deadline AS deadline
                FROM group_buys gb
                WHERE gb.status = :status
                  AND gb.deadline > CURRENT_TIMESTAMP
                  AND gb.product_name IS NOT NULL
                  AND gb.product_name <> ''
                UNION ALL
                SELECT s.name AS keyword, gb.deadline AS deadline
                FROM group_buys gb
                JOIN stores s ON gb.store_id = s.id
                WHERE gb.status = :status
                  AND gb.deadline > CURRENT_TIMESTAMP
                  AND s.name IS NOT NULL
                  AND s.name <> ''
                UNION ALL
                SELECT s.address AS keyword, gb.deadline AS deadline
                FROM group_buys gb
                JOIN stores s ON gb.store_id = s.id
                WHERE gb.status = :status
                  AND gb.deadline > CURRENT_TIMESTAMP
                  AND s.address IS NOT NULL
                  AND s.address <> ''
            ) active_keywords
            GROUP BY keyword
            ORDER BY MIN(deadline) ASC
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findActiveSearchKeywords(
        @Param("status") status: String,
        @Param("limit") limit: Int,
    ): List<String>

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
