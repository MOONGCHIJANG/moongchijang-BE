package com.moongchijang.domain.favorite.domain.repository

import com.moongchijang.domain.favorite.domain.entity.Favorite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface FavoriteRepository : JpaRepository<Favorite, Long>, FavoriteRepositoryCustom {

    fun existsByUserIdAndGroupBuyId(userId: Long, groupBuyId: Long): Boolean

    fun deleteByUserIdAndGroupBuyId(userId: Long, groupBuyId: Long): Int

    fun findByUserIdAndGroupBuyId(userId: Long, groupBuyId: Long): Favorite?

    @Query(
        """
        SELECT COUNT(f)
        FROM Favorite f
        WHERE f.user.id = :userId
          AND f.groupBuy.deadline > :now
          AND f.groupBuy.deadline <= :deadlineTo
        """
    )
    fun countUrgentByUserId(
        @Param("userId") userId: Long,
        @Param("now") now: LocalDateTime,
        @Param("deadlineTo") deadlineTo: LocalDateTime,
    ): Long
}
