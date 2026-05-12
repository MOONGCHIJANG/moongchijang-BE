package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyEmbedding
import org.springframework.data.jpa.repository.JpaRepository

interface GroupBuyEmbeddingRepository : JpaRepository<GroupBuyEmbedding, Long> {
    fun findByGroupBuyId(groupBuyId: Long): GroupBuyEmbedding?
}
