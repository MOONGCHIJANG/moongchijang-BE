package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import java.time.LocalDate
import java.util.Optional

interface GroupBuyRequestRepository : JpaRepository<GroupBuyRequest, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<GroupBuyRequest>

    fun countByUserId(userId: Long): Long

    fun findByStatusInAndDesiredPickupDate(
        statuses: Collection<GroupBuyRequestStatus>,
        desiredPickupDate: LocalDate
    ): List<GroupBuyRequest>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockById(id: Long): Optional<GroupBuyRequest>
}
