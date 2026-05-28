package com.moongchijang.domain.favorite.domain.repository

import com.moongchijang.domain.favorite.domain.entity.Favorite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

interface FavoriteRepository : JpaRepository<Favorite, Long>, FavoriteRepositoryCustom {

    fun existsByUserIdAndGroupBuyId(userId: Long, groupBuyId: Long): Boolean

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Favorite f where f.user.id = :userId")
    fun deleteByUserId(@Param("userId") userId: Long): Int

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

    @Query(
        """
        SELECT DISTINCT f.user.id
        FROM Favorite f
        WHERE f.groupBuy.id = :groupBuyId
          AND NOT EXISTS (
              SELECT p.id
              FROM Participation p
              WHERE p.user.id = f.user.id
                AND p.groupBuy.id = :groupBuyId
          )
        """
    )
    fun findUserIdsByGroupBuyIdExcludingParticipants(
        @Param("groupBuyId") groupBuyId: Long
    ): List<Long>

    @Query(
        """
        SELECT f.groupBuy.id AS groupBuyId, f.user.id AS userId
        FROM Favorite f
        WHERE f.groupBuy.id IN :groupBuyIds
          AND NOT EXISTS (
              SELECT p.id
              FROM Participation p
              WHERE p.user.id = f.user.id
                AND p.groupBuy.id = f.groupBuy.id
          )
        """
    )
    fun findNotificationTargetsByGroupBuyIdsExcludingParticipants(
        @Param("groupBuyIds") groupBuyIds: Collection<Long>
    ): List<FavoriteNotificationTargetProjection>
}
