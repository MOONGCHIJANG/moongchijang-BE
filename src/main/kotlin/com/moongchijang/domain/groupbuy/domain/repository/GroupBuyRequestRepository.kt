package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface GroupBuyRequestRepository : JpaRepository<GroupBuyRequest, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<GroupBuyRequest>

    fun findByStatusInAndDesiredPickupDate(
        statuses: Collection<GroupBuyRequestStatus>,
        desiredPickupDate: LocalDate
    ): List<GroupBuyRequest>
}
