package com.moongchijang.domain.participation.domain.repository

import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationCancelReason
import com.moongchijang.domain.participation.domain.entity.OwnerRefundReviewStatus
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
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

interface ParticipationRepository : JpaRepository<Participation, Long> {
    fun findAllByUserId(userId: Long): List<Participation>

    @Query(
        """
        select case when count(p) > 0 then true else false end
        from Participation p
        join p.groupBuy gb
        where p.user.id = :userId
          and p.status = :participationStatus
          and p.pickupStatus in :pickupStatuses
          and gb.status in :groupBuyStatuses
        """
    )
    fun existsPendingPickupForWithdrawal(
        @Param("userId") userId: Long,
        @Param("participationStatus") participationStatus: ParticipationStatus,
        @Param("pickupStatuses") pickupStatuses: Collection<PickupStatus>,
        @Param("groupBuyStatuses") groupBuyStatuses: Collection<GroupBuyStatus>,
    ): Boolean

    fun existsByUserIdAndGroupBuyId(userId: Long, groupBuyId: Long): Boolean
    fun existsByUserIdAndStatus(userId: Long, status: ParticipationStatus): Boolean

    @Query(
        """
        select case when count(p) > 0 then true else false end
        from Participation p
        join p.groupBuy gb
        where gb.store.id in :storeIds
          and gb.status in :groupBuyStatuses
          and p.status in :participationStatuses
          and p.pickupStatus <> com.moongchijang.domain.participation.domain.entity.PickupStatus.PICKED_UP
        """
    )
    fun existsUnpickedParticipationByStoreIdsAndGroupBuyStatuses(
        @Param("storeIds") storeIds: Collection<Long>,
        @Param("groupBuyStatuses") groupBuyStatuses: Collection<GroupBuyStatus>,
        @Param("participationStatuses") participationStatuses: Collection<ParticipationStatus>,
    ): Boolean

    fun findByUserIdAndGroupBuyId(userId: Long, groupBuyId: Long): Participation?

    @Query(
        """
        select p
        from Participation p
        join fetch p.user
        join fetch p.groupBuy gb
        join fetch gb.store
        where p.id = :participationId
        """
    )
    fun findPickupDetailById(@Param("participationId") participationId: Long): Participation?

    @Query(
        """
        SELECT DISTINCT p.user.id
        FROM Participation p
        WHERE p.groupBuy.id = :groupBuyId
        """
    )
    fun findDistinctUserIdsByGroupBuyId(@Param("groupBuyId") groupBuyId: Long): List<Long>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update Participation p
        set p.status = :newStatus,
            p.cancelledAt = :cancelledAt
        where p.groupBuy.id = :groupBuyId
          and p.status = :oldStatus
        """
    )
    fun updateStatusByGroupBuyIdAndStatus(
        @Param("groupBuyId") groupBuyId: Long,
        @Param("oldStatus") oldStatus: ParticipationStatus,
        @Param("newStatus") newStatus: ParticipationStatus,
        @Param("cancelledAt") cancelledAt: LocalDateTime
    ): Int

    @Query(
        """
        SELECT p
        FROM Participation p
        JOIN FETCH p.user u
        JOIN FETCH p.groupBuy gb
        JOIN FETCH gb.store s
        WHERE gb.pickupDate = :pickupDate
          AND p.status IN :participationStatuses
          AND p.pickupStatus IN :pickupStatuses
        """
    )
    fun findForPickupReminderByPickupDate(
        @Param("pickupDate") pickupDate: LocalDate,
        @Param("participationStatuses") participationStatuses: Collection<ParticipationStatus>,
        @Param("pickupStatuses") pickupStatuses: Collection<PickupStatus>,
    ): List<Participation>

    @Query(
        """
        SELECT p
        FROM Participation p
        JOIN FETCH p.groupBuy gb
        WHERE function('timestamp', gb.pickupDate, gb.pickupTimeEnd) <= :pickupCutoffBaseAt
          AND p.status IN :participationStatuses
          AND p.pickupStatus IN :pickupStatuses
        """
    )
    fun findForPickupCutoffCheck(
        @Param("pickupCutoffBaseAt") pickupCutoffBaseAt: LocalDateTime,
        @Param("participationStatuses") participationStatuses: Collection<ParticipationStatus>,
        @Param("pickupStatuses") pickupStatuses: Collection<PickupStatus>,
    ): List<Participation>

    @Query(
        value = """
            select p
            from Participation p
            join fetch p.groupBuy gb
            join fetch gb.store
            where p.user.id = :userId
              and p.status in :statuses
            order by p.createdAt desc
        """,
        countQuery = """
            select count(p)
            from Participation p
            where p.user.id = :userId
              and p.status in :statuses
        """
    )
    fun findInProgressByUserId(
        @Param("userId") userId: Long,
        @Param("statuses") statuses: Collection<ParticipationStatus>,
        pageable: Pageable
    ): Page<Participation>

    @Query(
        value = """
            select p
            from Participation p
            join fetch p.groupBuy gb
            join fetch gb.store
            where p.user.id = :userId
              and p.status in :participationStatuses
              and p.pickupStatus in :pickupStatuses
            order by p.createdAt desc
        """,
        countQuery = """
            select count(p)
            from Participation p
            where p.user.id = :userId
              and p.status in :participationStatuses
              and p.pickupStatus in :pickupStatuses
        """
    )
    fun findPickupWaitingByUserId(
        @Param("userId") userId: Long,
        @Param("participationStatuses") participationStatuses: Collection<ParticipationStatus>,
        @Param("pickupStatuses") pickupStatuses: Collection<PickupStatus>,
        pageable: Pageable
    ): Page<Participation>

    @Query(
        """
        select p
        from Participation p
        join fetch p.user
        join fetch p.groupBuy gb
        join fetch gb.store
        where p.user.id = :userId
          and p.status = :status
          and p.pickupStatus in :pickupStatuses
          and gb.pickupDate >= :fromDate
        order by gb.pickupDate asc, gb.pickupTimeStart asc, p.id asc
        """
    )
    fun findNearestPickupQrCandidates(
        @Param("userId") userId: Long,
        @Param("status") status: ParticipationStatus,
        @Param("pickupStatuses") pickupStatuses: Collection<PickupStatus>,
        @Param("fromDate") fromDate: LocalDate,
    ): List<Participation>

    @EntityGraph(attributePaths = ["groupBuy", "groupBuy.store"])
    fun findByUserIdAndStatusOrderByRefundedAtDescCreatedAtDesc(
        userId: Long,
        status: ParticipationStatus
    ): List<Participation>

    @EntityGraph(attributePaths = ["groupBuy", "groupBuy.store"])
    fun findByUserIdAndStatusOrderByCreatedAtDesc(
        userId: Long,
        status: ParticipationStatus
    ): List<Participation>

    @Query(
        """
        select p
        from Participation p
        join fetch p.groupBuy gb
        join fetch gb.store
        where p.user.id = :userId
          and p.status in :statuses
        order by
          case when p.status = :refundPendingStatus then 0 else 1 end,
          p.refundedAt desc,
          p.createdAt desc
        """
    )
    fun findByUserIdAndStatusInOrderByRefundedAtDescCreatedAtDesc(
        @Param("userId") userId: Long,
        @Param("statuses") statuses: Collection<ParticipationStatus>,
        @Param("refundPendingStatus") refundPendingStatus: ParticipationStatus = ParticipationStatus.REFUND_PENDING
    ): List<Participation>

    @EntityGraph(attributePaths = ["groupBuy", "groupBuy.store"])
    fun findByUserIdAndStatusInOrderByCreatedAtDesc(
        userId: Long,
        statuses: Collection<ParticipationStatus>
    ): List<Participation>

    @EntityGraph(attributePaths = ["user", "groupBuy", "groupBuy.store"])
    fun findByStatusOrderByCancelledAtAscCreatedAtAsc(
        status: ParticipationStatus,
        pageable: Pageable
    ): List<Participation>

    @EntityGraph(attributePaths = ["groupBuy", "groupBuy.store"])
    fun findByUserIdAndStatusInAndPickupStatusInOrderByCreatedAtDesc(
        userId: Long,
        statuses: Collection<ParticipationStatus>,
        pickupStatuses: Collection<PickupStatus>
    ): List<Participation>

    @EntityGraph(attributePaths = ["groupBuy", "groupBuy.store"])
    fun findByUserIdAndStatusInAndPickupStatusOrderByPickedUpAtDescCreatedAtDesc(
        userId: Long,
        statuses: Collection<ParticipationStatus>,
        pickupStatus: PickupStatus
    ): List<Participation>

    fun countByUserIdAndStatusInAndPickupStatusIn(
        userId: Long,
        statuses: Collection<ParticipationStatus>,
        pickupStatuses: Collection<PickupStatus>
    ): Long

    fun countByUserIdAndStatusInAndPickupStatus(
        userId: Long,
        statuses: Collection<ParticipationStatus>,
        pickupStatus: PickupStatus
    ): Long

    fun countByUserIdAndStatus(userId: Long, status: ParticipationStatus): Long

    @Query(
        """
        select count(p)
        from Participation p
        where p.status = :status
          and p.refundedAt >= :from
          and p.refundedAt < :to
        """
    )
    fun countByStatusAndRefundedAtFromUntil(
        @Param("status") status: ParticipationStatus,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime
    ): Long

    @Query(
        """
        select coalesce(sum(po.totalAmount), 0)
        from Participation p
        join PaymentOrder po on po.user = p.user and po.groupBuy = p.groupBuy
        where p.status = :status
        """
    )
    fun sumPaymentOrderAmountByStatus(@Param("status") status: ParticipationStatus): Long

    @Query(
        """
        select coalesce(sum(po.totalAmount), 0)
        from Participation p
        join PaymentOrder po on po.user = p.user and po.groupBuy = p.groupBuy
        where p.status = :status
          and p.cancelledAt >= :from
          and p.cancelledAt < :to
        """
    )
    fun sumPaymentOrderAmountByStatusAndCancelledAtBetween(
        @Param("status") status: ParticipationStatus,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime
    ): Long

    fun countByUserIdAndStatusIn(userId: Long, statuses: Collection<ParticipationStatus>): Long

    fun countByGroupBuyIdAndStatusIn(groupBuyId: Long, statuses: Collection<ParticipationStatus>): Long

    fun countByGroupBuyIdAndStatusInAndPickupStatus(groupBuyId: Long, statuses: Collection<ParticipationStatus>, pickupStatus: PickupStatus): Long

    @Query(
        """
        select p.groupBuy.id as groupBuyId,
               count(p) as pendingRefundCount
        from Participation p
        where p.groupBuy.id in :groupBuyIds
          and p.status = :status
        group by p.groupBuy.id
        """
    )
    fun countPendingRefundsByGroupBuyIdIn(
        @Param("groupBuyIds") groupBuyIds: Collection<Long>,
        @Param("status") status: ParticipationStatus = ParticipationStatus.REFUND_PENDING
    ): List<GroupBuyPendingRefundCount>

    @Query(
        """
        select coalesce(sum(p.totalAmount), 0)
        from Participation p
        join p.groupBuy gb
        where gb.store.id in :storeIds
          and gb.status in :groupBuyStatuses
          and p.status in :participationStatuses
          and year(gb.pickupDate) = :year
          and month(gb.pickupDate) = :month
        """
    )
    fun sumTotalAmountByStoreIdsAndStatusesAndYearMonth(
        @Param("storeIds") storeIds: Collection<Long>,
        @Param("groupBuyStatuses") groupBuyStatuses: Collection<GroupBuyStatus>,
        @Param("participationStatuses") participationStatuses: Collection<ParticipationStatus>,
        @Param("year") year: Int,
        @Param("month") month: Int,
    ): Long

    @Query(
        """
        select coalesce(sum(p.feeAmount), 0)
        from Participation p
        join p.groupBuy gb
        where gb.store.id in :storeIds
          and p.status in :refundStatuses
          and year(gb.pickupDate) = :year
          and month(gb.pickupDate) = :month
        """
    )
    fun sumRefundFeeAmountByStoreIdsAndYearMonth(
        @Param("storeIds") storeIds: Collection<Long>,
        @Param("refundStatuses") refundStatuses: Collection<ParticipationStatus>,
        @Param("year") year: Int,
        @Param("month") month: Int,
    ): Long

    @Query(
        """
        select p
        from Participation p
        join fetch p.user
        join fetch p.groupBuy gb
        join fetch gb.store
        where gb.store.id in :storeIds
          and p.status in :statuses
          and coalesce(p.cancelledAt, p.createdAt) >= :fromDateTime
        order by p.cancelledAt desc, p.createdAt desc
        """
    )
    fun findRefundRequestsByStoreIdsAndStatuses(
        @Param("storeIds") storeIds: Collection<Long>,
        @Param("statuses") statuses: Collection<ParticipationStatus>,
        @Param("fromDateTime") fromDateTime: LocalDateTime,
    ): List<Participation>

    @Query(
        value = """
        select gb.id as groupBuyId,
               gb.store.name as storeName,
               gb.productName as productName,
               gb.pickupDate as pickupCompletedDate,
               coalesce(sum(case when p.status in :revenueStatuses then 1 else 0 end), 0) as participantCount,
               coalesce(sum(case when p.status in :transactionStatuses then p.totalAmount else 0 end), 0) as totalPaymentAmount,
               coalesce(sum(case when p.status in :refundStatuses then p.totalAmount else 0 end), 0) as refundDeductionAmount,
               0 as platformFeeAmount
        from GroupBuy gb
        left join Participation p on p.groupBuy = gb
        where gb.status in :groupBuyStatuses
          and gb.pickupDate >= :pickupDateFrom
          and gb.pickupDate < :pickupDateTo
        group by gb.id, gb.store.name, gb.productName, gb.pickupDate
        order by gb.pickupDate desc, gb.id desc
        """,
        countQuery = """
        select count(gb)
        from GroupBuy gb
        where gb.status in :groupBuyStatuses
          and gb.pickupDate >= :pickupDateFrom
          and gb.pickupDate < :pickupDateTo
        """
    )
    fun findAdminSettlementPage(
        @Param("groupBuyStatuses") groupBuyStatuses: Collection<GroupBuyStatus>,
        @Param("transactionStatuses") transactionStatuses: Collection<ParticipationStatus>,
        @Param("revenueStatuses") revenueStatuses: Collection<ParticipationStatus>,
        @Param("refundStatuses") refundStatuses: Collection<ParticipationStatus>,
        @Param("pickupDateFrom") pickupDateFrom: LocalDate,
        @Param("pickupDateTo") pickupDateTo: LocalDate,
        pageable: Pageable,
    ): Page<AdminSettlementAggregation>

    @Query(
        """
        select gb.id as groupBuyId,
               gb.store.name as storeName,
               gb.productName as productName,
               gb.pickupDate as pickupCompletedDate,
               coalesce(sum(case when p.status in :revenueStatuses then 1 else 0 end), 0) as participantCount,
               coalesce(sum(case when p.status in :transactionStatuses then p.totalAmount else 0 end), 0) as totalPaymentAmount,
               coalesce(sum(case when p.status in :refundStatuses then p.totalAmount else 0 end), 0) as refundDeductionAmount,
               0 as platformFeeAmount
        from GroupBuy gb
        left join Participation p on p.groupBuy = gb
        where gb.status in :groupBuyStatuses
          and gb.pickupDate >= :pickupDateFrom
          and gb.pickupDate < :pickupDateTo
        group by gb.id, gb.store.name, gb.productName, gb.pickupDate
        """
    )
    fun findAdminSettlementAggregations(
        @Param("groupBuyStatuses") groupBuyStatuses: Collection<GroupBuyStatus>,
        @Param("transactionStatuses") transactionStatuses: Collection<ParticipationStatus>,
        @Param("revenueStatuses") revenueStatuses: Collection<ParticipationStatus>,
        @Param("refundStatuses") refundStatuses: Collection<ParticipationStatus>,
        @Param("pickupDateFrom") pickupDateFrom: LocalDate,
        @Param("pickupDateTo") pickupDateTo: LocalDate,
    ): List<AdminSettlementAggregation>

    @Query(
        """
        select gb.id as groupBuyId,
               gb.store.name as storeName,
               gb.productName as productName,
               gb.pickupDate as pickupCompletedDate,
               coalesce(sum(case when p.status in :revenueStatuses then 1 else 0 end), 0) as participantCount,
               coalesce(sum(case when p.status in :transactionStatuses then p.totalAmount else 0 end), 0) as totalPaymentAmount,
               coalesce(sum(case when p.status in :refundStatuses then p.totalAmount else 0 end), 0) as refundDeductionAmount,
               0 as platformFeeAmount
        from GroupBuy gb
        left join Participation p on p.groupBuy = gb
        where gb.store.id in :storeIds
          and gb.status in :groupBuyStatuses
          and gb.pickupDate >= :pickupDateFrom
          and gb.pickupDate < :pickupDateTo
        group by gb.id, gb.store.name, gb.productName, gb.pickupDate
        order by gb.pickupDate desc, gb.id desc
        """
    )
    fun findOwnerSettlementAggregations(
        @Param("storeIds") storeIds: Collection<Long>,
        @Param("groupBuyStatuses") groupBuyStatuses: Collection<GroupBuyStatus>,
        @Param("transactionStatuses") transactionStatuses: Collection<ParticipationStatus>,
        @Param("revenueStatuses") revenueStatuses: Collection<ParticipationStatus>,
        @Param("refundStatuses") refundStatuses: Collection<ParticipationStatus>,
        @Param("pickupDateFrom") pickupDateFrom: LocalDate,
        @Param("pickupDateTo") pickupDateTo: LocalDate,
    ): List<AdminSettlementAggregation>

    @Query(
        """
        select gb.id as groupBuyId,
               gb.store.name as storeName,
               gb.productName as productName,
               gb.pickupDate as pickupCompletedDate,
               coalesce(sum(case when p.status in :revenueStatuses then 1 else 0 end), 0) as participantCount,
               coalesce(sum(case when p.status in :transactionStatuses then p.totalAmount else 0 end), 0) as totalPaymentAmount,
               coalesce(sum(case when p.status in :refundStatuses then p.totalAmount else 0 end), 0) as refundDeductionAmount,
               0 as platformFeeAmount
        from GroupBuy gb
        left join Participation p on p.groupBuy = gb
        where gb.id = :groupBuyId
          and gb.status in :groupBuyStatuses
        group by gb.id, gb.store.name, gb.productName, gb.pickupDate
        """
    )
    fun findAdminSettlementDetail(
        @Param("groupBuyId") groupBuyId: Long,
        @Param("groupBuyStatuses") groupBuyStatuses: Collection<GroupBuyStatus>,
        @Param("transactionStatuses") transactionStatuses: Collection<ParticipationStatus>,
        @Param("revenueStatuses") revenueStatuses: Collection<ParticipationStatus>,
        @Param("refundStatuses") refundStatuses: Collection<ParticipationStatus>,
    ): AdminSettlementAggregation?

    @Query(
        """
        select p
        from Participation p
        join fetch p.user
        join fetch p.groupBuy gb
        where gb.id = :groupBuyId
          and p.status in :statuses
        order by p.createdAt asc
        """
    )
    fun findByGroupBuyIdAndStatusInOrderByCreatedAtAsc(
        @Param("groupBuyId") groupBuyId: Long,
        @Param("statuses") statuses: Collection<ParticipationStatus>
    ): List<Participation>

    fun existsByPickupToken(pickupToken: String): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select p
        from Participation p
        join fetch p.user
        join fetch p.groupBuy gb
        join fetch gb.store
        where p.pickupToken = :pickupToken
        """
    )
    fun findByPickupTokenForUpdate(@Param("pickupToken") pickupToken: String): Participation?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Participation p join fetch p.groupBuy where p.id = :id")
    fun findByIdForUpdate(@Param("id") id: Long): Optional<Participation>

    @Query(
        value = """
        select p
        from Participation p
        join fetch p.user u
        join fetch p.groupBuy gb
        join fetch gb.store
        where p.status in :statuses
          and (
            :useReviewStatusFilter = false
            or p.ownerRefundReviewStatus in :reviewStatuses
            or (:includeNullReviewStatus = true and p.ownerRefundReviewStatus is null)
          )
          and (
            :caseFilter = 'ALL'
            or (
              :caseFilter = 'TARGET_NOT_MET'
              and p.cancelReason is null
              and gb.status = com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus.FAILED
            )
            or (
              :caseFilter = 'OWNER_FAULT_CANCEL'
              and p.cancelReason is null
              and gb.status <> com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus.FAILED
            )
            or (
              :caseFilter not in ('ALL', 'TARGET_NOT_MET', 'OWNER_FAULT_CANCEL')
              and p.cancelReason in :cancelReasons
            )
          )
          and (
            :keyword is null
            or str(p.id) = :keyword
            or lower(u.nickname) like lower(concat('%', :keyword, '%'))
            or lower(gb.productName) like lower(concat('%', :keyword, '%'))
          )
        order by p.cancelledAt desc, p.createdAt desc
        """,
        countQuery = """
        select count(p)
        from Participation p
        join p.user u
        join p.groupBuy gb
        where p.status in :statuses
          and (
            :useReviewStatusFilter = false
            or p.ownerRefundReviewStatus in :reviewStatuses
            or (:includeNullReviewStatus = true and p.ownerRefundReviewStatus is null)
          )
          and (
            :caseFilter = 'ALL'
            or (
              :caseFilter = 'TARGET_NOT_MET'
              and p.cancelReason is null
              and gb.status = com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus.FAILED
            )
            or (
              :caseFilter = 'OWNER_FAULT_CANCEL'
              and p.cancelReason is null
              and gb.status <> com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus.FAILED
            )
            or (
              :caseFilter not in ('ALL', 'TARGET_NOT_MET', 'OWNER_FAULT_CANCEL')
              and p.cancelReason in :cancelReasons
            )
          )
          and (
            :keyword is null
            or str(p.id) = :keyword
            or lower(u.nickname) like lower(concat('%', :keyword, '%'))
            or lower(gb.productName) like lower(concat('%', :keyword, '%'))
          )
        """
    )
    fun findAdminRefundRequests(
        @Param("statuses") statuses: Collection<ParticipationStatus>,
        @Param("useReviewStatusFilter") useReviewStatusFilter: Boolean,
        @Param("reviewStatuses") reviewStatuses: Collection<OwnerRefundReviewStatus>,
        @Param("includeNullReviewStatus") includeNullReviewStatus: Boolean,
        @Param("caseFilter") caseFilter: String,
        @Param("cancelReasons") cancelReasons: Collection<ParticipationCancelReason>,
        @Param("keyword") keyword: String?,
        pageable: Pageable,
    ): Page<Participation>

    @Query(
        value = """
        select p
        from Participation p
        join fetch p.user
        join fetch p.groupBuy gb
        where p.status = :status
          and (
            (p.cancelledAt is not null and p.cancelledAt <= :requestedBefore)
            or (p.cancelledAt is null and p.createdAt <= :requestedBefore)
          )
        order by coalesce(p.cancelledAt, p.createdAt) asc, p.id asc
        """,
        countQuery = """
        select count(p)
        from Participation p
        where p.status = :status
          and (
            (p.cancelledAt is not null and p.cancelledAt <= :requestedBefore)
            or (p.cancelledAt is null and p.createdAt <= :requestedBefore)
          )
        """
    )
    fun findDashboardUrgentRefundRequests(
        @Param("status") status: ParticipationStatus,
        @Param("requestedBefore") requestedBefore: LocalDateTime,
        pageable: Pageable,
    ): Page<Participation>

    @Query(
        """
        select p
        from Participation p
        join fetch p.user
        join fetch p.groupBuy gb
        join fetch gb.store
        where p.id = :id
        """
    )
    fun findForPickupReminderById(@Param("id") id: Long): Participation?
}

interface GroupBuyPendingRefundCount {
    val groupBuyId: Long
    val pendingRefundCount: Long
}

interface AdminSettlementAggregation {
    val groupBuyId: Long
    val storeName: String
    val productName: String
    val pickupCompletedDate: LocalDate
    val participantCount: Long
    val totalPaymentAmount: Long
    val refundDeductionAmount: Long
    val platformFeeAmount: Long
}
