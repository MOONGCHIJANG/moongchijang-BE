package com.moongchijang.domain.favorite.domain.repository

import com.moongchijang.domain.favorite.domain.entity.Favorite
import org.springframework.data.jpa.repository.JpaRepository

interface FavoriteRepository : JpaRepository<Favorite, Long> {

    fun existsByUserIdAndGroupBuyId(userId: Long, groupBuyId: Long): Boolean

    fun deleteByUserIdAndGroupBuyId(userId: Long, groupBuyId: Long): Int

    fun findByUserIdAndGroupBuyId(userId: Long, groupBuyId: Long): Favorite?
}
