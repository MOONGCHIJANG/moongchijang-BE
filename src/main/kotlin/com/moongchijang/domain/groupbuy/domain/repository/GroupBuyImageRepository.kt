package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyImage
import org.springframework.data.jpa.repository.JpaRepository

interface GroupBuyImageRepository : JpaRepository<GroupBuyImage, Long> {

    fun findAllByGroupBuyId(groupBuyId: Long): List<GroupBuyImage>
}
