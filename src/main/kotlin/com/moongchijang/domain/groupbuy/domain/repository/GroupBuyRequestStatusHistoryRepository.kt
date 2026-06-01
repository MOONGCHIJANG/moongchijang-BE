package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatusHistory
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface GroupBuyRequestStatusHistoryRepository : JpaRepository<GroupBuyRequestStatusHistory, Long> {
    fun findByGroupBuyRequest_IdOrderByChangedAtAsc(groupBuyRequestId: Long): List<GroupBuyRequestStatusHistory>
    fun findByGroupBuyRequest_IdInOrderByChangedAtAsc(groupBuyRequestIds: List<Long>): List<GroupBuyRequestStatusHistory>
    @Query(
        """
        select count(history)
        from GroupBuyRequestStatusHistory history
        where history.status in :statuses
          and history.changedAt >= :from
          and history.changedAt < :to
        """
    )
    fun countByStatusInAndChangedAtFromUntil(
        @Param("statuses") statuses: Collection<GroupBuyRequestStatus>,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime
    ): Long
}
