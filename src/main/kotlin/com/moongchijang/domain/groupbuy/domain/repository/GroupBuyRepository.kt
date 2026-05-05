package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface GroupBuyRepository : JpaRepository<GroupBuy, Long>, GroupBuyRepositoryCustom {

    @EntityGraph(attributePaths = ["store"])
    fun findWithStoreById(id: Long): Optional<GroupBuy>
}
