package com.moongchijang.domain.participation.domain.repository

import com.moongchijang.domain.participation.domain.entity.Participation
import org.springframework.data.jpa.repository.JpaRepository

interface ParticipationRepository : JpaRepository<Participation, Long> {

    fun existsByUserIdAndGroupBuyId(userId: Long, groupBuyId: Long): Boolean
}
