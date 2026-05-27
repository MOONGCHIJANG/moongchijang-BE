package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatusHistory
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface GroupBuyRequestStatusHistoryRepository : JpaRepository<GroupBuyRequestStatusHistory, Long> {
    fun findByGroupBuyRequestIdOrderByChangedAtAsc(groupBuyRequestId: Long): List<GroupBuyRequestStatusHistory>
    fun findByGroupBuyRequestIdInOrderByChangedAtAsc(groupBuyRequestIds: List<Long>): List<GroupBuyRequestStatusHistory>
    fun countByStatusInAndChangedAtBetween(
        statuses: Collection<GroupBuyRequestStatus>,
        from: LocalDateTime,
        to: LocalDateTime
    ): Long
}
