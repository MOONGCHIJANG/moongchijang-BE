package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.Optional

interface GroupBuyRequestRepository : JpaRepository<GroupBuyRequest, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<GroupBuyRequest>

    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<GroupBuyRequest>

    fun findByStatusOrderByCreatedAtDesc(
        status: GroupBuyRequestStatus,
        pageable: Pageable
    ): Page<GroupBuyRequest>

    @Query(
        value = """
            SELECT request
            FROM GroupBuyRequest request
            LEFT JOIN User requester ON requester.id = request.userId
            WHERE (:status IS NULL OR request.status = :status)
              AND (
                :keyword IS NULL
                OR (:requestIdKeyword IS NOT NULL AND request.id = :requestIdKeyword)
                OR LOWER(request.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(request.storeName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(requester.nickname, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(requester.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR COALESCE(requester.phoneNumber, '') LIKE CONCAT('%', :keyword, '%')
              )
            ORDER BY request.createdAt DESC
        """,
        countQuery = """
            SELECT COUNT(request)
            FROM GroupBuyRequest request
            LEFT JOIN User requester ON requester.id = request.userId
            WHERE (:status IS NULL OR request.status = :status)
              AND (
                :keyword IS NULL
                OR (:requestIdKeyword IS NOT NULL AND request.id = :requestIdKeyword)
                OR LOWER(request.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(request.storeName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(requester.nickname, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(requester.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR COALESCE(requester.phoneNumber, '') LIKE CONCAT('%', :keyword, '%')
              )
        """
    )
    fun searchAdminRequests(
        @Param("status") status: GroupBuyRequestStatus?,
        @Param("keyword") keyword: String?,
        @Param("requestIdKeyword") requestIdKeyword: Long?,
        pageable: Pageable
    ): Page<GroupBuyRequest>

    fun countByUserId(userId: Long): Long

    fun findByStatusInAndDesiredPickupDate(
        statuses: Collection<GroupBuyRequestStatus>,
        desiredPickupDate: LocalDate
    ): List<GroupBuyRequest>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockById(id: Long): Optional<GroupBuyRequest>
}
