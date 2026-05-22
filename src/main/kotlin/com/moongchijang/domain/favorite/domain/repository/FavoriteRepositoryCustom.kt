package com.moongchijang.domain.favorite.domain.repository

import com.moongchijang.domain.favorite.application.dto.WishFilterType
import com.moongchijang.domain.favorite.application.dto.WishSortType
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

interface FavoriteRepositoryCustom {

    companion object {
        const val CLOSING_SOON_DAYS = 3L
    }

    fun findWishlistGroupBuys(
        userId: Long,
        filter: WishFilterType,
        sort: WishSortType,
        pageable: Pageable,
        now: LocalDateTime,
    ): Page<GroupBuy>
}
