package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import org.springframework.data.jpa.repository.JpaRepository

interface GroupBuyRepository : JpaRepository<GroupBuy, Long>, GroupBuyRepositoryCustom {
}
