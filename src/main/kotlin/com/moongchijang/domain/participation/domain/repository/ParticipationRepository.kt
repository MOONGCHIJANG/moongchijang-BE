package com.moongchijang.domain.participation.domain.repository

import com.moongchijang.domain.participation.domain.entity.Participation
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface ParticipationRepository : JpaRepository<Participation, Long> {

    fun existsByUserIdAndGroupBuyId(userId: Long, groupBuyId: Long): Boolean

    fun findByUserIdAndGroupBuyId(userId: Long, groupBuyId: Long): Participation?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Participation p join fetch p.groupBuy where p.id = :id")
    fun findByIdForUpdate(@Param("id") id: Long): Optional<Participation>
}
