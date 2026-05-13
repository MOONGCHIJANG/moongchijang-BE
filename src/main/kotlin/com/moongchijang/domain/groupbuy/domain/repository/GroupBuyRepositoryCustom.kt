package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedFilter
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.store.domain.entity.DistrictType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

interface GroupBuyRepositoryCustom {

    companion object {
        const val ALMOST_ACHIEVED_RATE = 80
        const val CLOSING_SOON_DAYS = 3L
    }

    fun searchFeed(
        filter: GroupBuyFeedFilter,
        districtFilters: Set<DistrictType>,
        pageable: Pageable,
        sortMode: FeedSortMode,
    ): Page<GroupBuy>

    fun searchByIntent(
        region: String?,
        product: String?,
        now: LocalDateTime,
        status: GroupBuyStatus
    ): List<GroupBuy>

    fun findDistinctRegions(status: GroupBuyStatus): List<String>

    fun findDistinctProductNames(status: GroupBuyStatus): List<String>
}
