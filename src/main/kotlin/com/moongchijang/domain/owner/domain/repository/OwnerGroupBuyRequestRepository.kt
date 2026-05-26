package com.moongchijang.domain.owner.domain.repository

import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequest
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OwnerGroupBuyRequestRepository : JpaRepository<OwnerGroupBuyRequest, Long> {

    @Query("SELECT r FROM OwnerGroupBuyRequest r WHERE r.owner.id = :ownerId ORDER BY r.createdAt DESC")
    fun findByOwnerIdOrderByCreatedAtDesc(@Param("ownerId") ownerId: Long): List<OwnerGroupBuyRequest>

    @Query(
        value = """
            SELECT r FROM OwnerGroupBuyRequest r
            JOIN FETCH r.store
            WHERE r.owner.id = :ownerId
            AND r.store.id IN :storeIds
            ORDER BY r.createdAt DESC
        """,
        countQuery = """
            SELECT COUNT(r) FROM OwnerGroupBuyRequest r
            WHERE r.owner.id = :ownerId
            AND r.store.id IN :storeIds
        """
    )
    fun findPageByOwnerIdAndStoreIdIn(
        @Param("ownerId") ownerId: Long,
        @Param("storeIds") storeIds: Collection<Long>,
        pageable: Pageable
    ): Page<OwnerGroupBuyRequest>

    @Query(
        """
            SELECT r FROM OwnerGroupBuyRequest r
            JOIN FETCH r.store
            WHERE r.owner.id = :ownerId
              AND r.store.id IN :storeIds
              AND r.status = :status
            ORDER BY r.createdAt DESC
        """
    )
    fun findByOwnerIdAndStoreIdInAndStatusOrderByCreatedAtDesc(
        @Param("ownerId") ownerId: Long,
        @Param("storeIds") storeIds: Collection<Long>,
        @Param("status") status: OwnerGroupBuyRequestStatus
    ): List<OwnerGroupBuyRequest>

    fun findByStatusOrderByCreatedAtAsc(status: OwnerGroupBuyRequestStatus): List<OwnerGroupBuyRequest>
}
