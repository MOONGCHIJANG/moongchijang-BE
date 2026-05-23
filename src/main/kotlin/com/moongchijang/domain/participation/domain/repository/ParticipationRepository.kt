package com.moongchijang.domain.participation.domain.repository

import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
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

    fun existsByUserIdAndGroupBuyId(userId: Long, groupBuyId: Long): Boolean

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
        JOIN FETCH p.groupBuy gb
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

    fun countByUserIdAndStatusIn(userId: Long, statuses: Collection<ParticipationStatus>): Long

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
}
