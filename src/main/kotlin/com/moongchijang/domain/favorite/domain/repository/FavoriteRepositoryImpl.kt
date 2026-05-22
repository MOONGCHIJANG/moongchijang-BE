package com.moongchijang.domain.favorite.domain.repository

import com.moongchijang.domain.favorite.application.dto.WishFilterType
import com.moongchijang.domain.favorite.application.dto.WishSortType
import com.moongchijang.domain.favorite.domain.entity.QFavorite.favorite
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.entity.QGroupBuy.groupBuy
import com.moongchijang.domain.store.domain.entity.QStore.store
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class FavoriteRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : FavoriteRepositoryCustom {

    override fun findWishlistGroupBuys(
        userId: Long,
        filter: WishFilterType,
        sort: WishSortType,
        pageable: Pageable,
    ): Page<GroupBuy> {
        val now = LocalDateTime.now()
        val where = buildWhere(userId, filter, now)
        val orderSpecifiers = buildOrderSpecifiers(sort)

        val content = queryFactory
            .select(groupBuy)
            .from(favorite)
            .join(favorite.groupBuy, groupBuy).fetchJoin()
            .join(groupBuy.store, store).fetchJoin()
            .where(where)
            .orderBy(*orderSpecifiers.toTypedArray())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(favorite.count())
            .from(favorite)
            .join(favorite.groupBuy, groupBuy)
            .where(where)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    private fun buildWhere(
        userId: Long,
        filter: WishFilterType,
        now: LocalDateTime,
    ): BooleanBuilder {
        val builder = BooleanBuilder()
            .and(favorite.user.id.eq(userId))

        filterPredicate(filter, now)?.let { builder.and(it) }
        return builder
    }

    private fun buildOrderSpecifiers(sort: WishSortType): List<OrderSpecifier<*>> {
        return when (sort) {
            WishSortType.LATEST -> listOf(
                favorite.createdAt.desc(),
                favorite.id.desc(),
            )
            WishSortType.DEADLINE -> listOf(
                groupBuy.deadline.asc(),
                favorite.id.desc(),
            )
        }
    }

    private fun filterPredicate(filter: WishFilterType, now: LocalDateTime): BooleanExpression? {
        return when (filter) {
            WishFilterType.ALL -> null
            WishFilterType.CLOSING_SOON -> groupBuy.deadline.loe(now.plusDays(FavoriteRepositoryCustom.CLOSING_SOON_DAYS))
            WishFilterType.ACHIEVEMENT_SOON -> groupBuy.currentQuantity.multiply(100)
                .goe(groupBuy.targetQuantity.multiply(FavoriteRepositoryCustom.ALMOST_ACHIEVED_RATE))
            WishFilterType.OPEN -> groupBuy.status.eq(GroupBuyStatus.IN_PROGRESS).and(groupBuy.deadline.gt(now))
            WishFilterType.CLOSED -> groupBuy.status.eq(GroupBuyStatus.CLOSED).or(groupBuy.deadline.loe(now))
        }
    }
}
