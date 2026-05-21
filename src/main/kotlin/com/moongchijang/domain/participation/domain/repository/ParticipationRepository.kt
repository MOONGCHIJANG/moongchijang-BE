package com.moongchijang.domain.participation.domain.repository

import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface ParticipationRepository : JpaRepository<Participation, Long> {

    fun existsByUserIdAndGroupBuyId(userId: Long, groupBuyId: Long): Boolean

    fun findByUserIdAndGroupBuyId(userId: Long, groupBuyId: Long): Participation?

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Participation p join fetch p.groupBuy where p.id = :id")
    fun findByIdForUpdate(@Param("id") id: Long): Optional<Participation>
}
