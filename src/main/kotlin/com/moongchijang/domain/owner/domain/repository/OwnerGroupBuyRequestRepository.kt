package com.moongchijang.domain.owner.domain.repository

import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequest
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface OwnerGroupBuyRequestRepository : JpaRepository<OwnerGroupBuyRequest, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM OwnerGroupBuyRequest r WHERE r.owner.id = :ownerId")
    fun deleteByOwnerId(@Param("ownerId") ownerId: Long): Long

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

    @Query(
        value = """
            SELECT r FROM OwnerGroupBuyRequest r
            JOIN FETCH r.owner
            JOIN FETCH r.store
            WHERE (:status IS NULL OR r.status = :status)
              AND (
                :keyword IS NULL
                OR (:requestId IS NOT NULL AND r.id = :requestId)
                OR LOWER(r.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(r.owner.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(r.store.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY r.createdAt DESC
        """,
        countQuery = """
            SELECT COUNT(r) FROM OwnerGroupBuyRequest r
            WHERE (:status IS NULL OR r.status = :status)
              AND (
                :keyword IS NULL
                OR (:requestId IS NOT NULL AND r.id = :requestId)
                OR LOWER(r.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(r.owner.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(r.store.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
        """
    )
    fun searchAdminRequests(
        @Param("status") status: OwnerGroupBuyRequestStatus?,
        @Param("requestId") requestId: Long?,
        @Param("keyword") keyword: String?,
        pageable: Pageable
    ): Page<OwnerGroupBuyRequest>

    @Query(
        """
            SELECT r FROM OwnerGroupBuyRequest r
            JOIN FETCH r.owner
            JOIN FETCH r.store
            LEFT JOIN FETCH r.approvedGroupBuy
            WHERE r.id = :id
        """
    )
    fun findAdminDetailById(@Param("id") id: Long): Optional<OwnerGroupBuyRequest>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM OwnerGroupBuyRequest r WHERE r.id = :id")
    fun findWithLockById(@Param("id") id: Long): Optional<OwnerGroupBuyRequest>
}
