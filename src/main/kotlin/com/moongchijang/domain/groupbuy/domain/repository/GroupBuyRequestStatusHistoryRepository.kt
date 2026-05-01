package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatusHistory
import org.springframework.data.jpa.repository.JpaRepository

interface GroupBuyRequestStatusHistoryRepository : JpaRepository<GroupBuyRequestStatusHistory, Long> {
    fun findByGroupBuyRequestIdOrderByChangedAtAsc(groupBuyRequestId: Long): List<GroupBuyRequestStatusHistory>
    fun findByGroupBuyRequestIdInOrderByChangedAtAsc(groupBuyRequestIds: List<Long>): List<GroupBuyRequestStatusHistory>
}
