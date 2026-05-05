package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedFilter
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.store.domain.entity.DistrictType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface GroupBuyRepositoryCustom {

    companion object {
        const val ALMOST_ACHIEVED_RATE = 80
        const val CLOSING_SOON_DAYS = 3L
    }

    fun searchFeed(
        filter: GroupBuyFeedFilter,
        districtFilters: Set<DistrictType>,
        keyword: String?,
        pageable: Pageable
    ): Page<GroupBuy>
}
