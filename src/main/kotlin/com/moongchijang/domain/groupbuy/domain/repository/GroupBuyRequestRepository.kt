package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import org.springframework.data.jpa.repository.JpaRepository

interface GroupBuyRequestRepository : JpaRepository<GroupBuyRequest, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<GroupBuyRequest>
}
