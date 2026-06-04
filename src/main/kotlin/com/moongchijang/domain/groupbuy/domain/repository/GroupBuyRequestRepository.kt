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
import java.time.LocalDateTime
import java.util.Optional

interface GroupBuyRequestRepository : JpaRepository<GroupBuyRequest, Long> {
    fun findByUser_IdOrderByCreatedAtDesc(userId: Long): List<GroupBuyRequest>
    fun deleteByUser_Id(userId: Long): Long

    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<GroupBuyRequest>

    fun findByStatusOrderByCreatedAtDesc(
        status: GroupBuyRequestStatus,
        pageable: Pageable
    ): Page<GroupBuyRequest>

    @Query(
        value = """
            SELECT request
            FROM GroupBuyRequest request
            LEFT JOIN FETCH request.user requester
            WHERE (:status IS NULL OR request.status = :status)
              AND (
                :keyword IS NULL
                OR (:requestIdKeyword IS NOT NULL AND request.id = :requestIdKeyword)
                OR LOWER(request.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(request.storeName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(requester.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(requester.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR requester.phoneNumber LIKE CONCAT('%', :keyword, '%')
              )
            ORDER BY request.createdAt DESC
        """,
        countQuery = """
            SELECT COUNT(request)
            FROM GroupBuyRequest request
            LEFT JOIN request.user requester
            WHERE (:status IS NULL OR request.status = :status)
              AND (
                :keyword IS NULL
                OR (:requestIdKeyword IS NOT NULL AND request.id = :requestIdKeyword)
                OR LOWER(request.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(request.storeName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(requester.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(requester.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR requester.phoneNumber LIKE CONCAT('%', :keyword, '%')
              )
        """
    )
    fun searchAdminRequests(
        @Param("status") status: GroupBuyRequestStatus?,
        @Param("keyword") keyword: String?,
        @Param("requestIdKeyword") requestIdKeyword: Long?,
        pageable: Pageable
    ): Page<GroupBuyRequest>

    fun countByUser_Id(userId: Long): Long
    fun countByStatusIn(statuses: Collection<GroupBuyRequestStatus>): Long

    @Query(
        """
        select count(request)
        from GroupBuyRequest request
        where request.status in :statuses
          and request.createdAt >= :from
          and request.createdAt < :to
        """
    )
    fun countByStatusInAndCreatedAtBetween(
        @Param("statuses") statuses: Collection<GroupBuyRequestStatus>,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime
    ): Long

    @Query(
        """
        select request.createdAt
        from GroupBuyRequest request
        where request.status in :statuses
          and request.createdAt is not null
        """
    )
    fun findCreatedAtByStatusIn(@Param("statuses") statuses: Collection<GroupBuyRequestStatus>): List<LocalDateTime>

    fun findByStatusInAndDesiredPickupDate(
        statuses: Collection<GroupBuyRequestStatus>,
        desiredPickupDate: LocalDate
    ): List<GroupBuyRequest>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockById(id: Long): Optional<GroupBuyRequest>
}
