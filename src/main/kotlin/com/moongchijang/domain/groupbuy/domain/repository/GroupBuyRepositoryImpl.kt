package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedFilter
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.entity.QGroupBuy.groupBuy
import com.moongchijang.domain.store.domain.entity.QStore.store
import com.moongchijang.domain.favorite.domain.entity.QFavorite.favorite
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class GroupBuyRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : GroupBuyRepositoryCustom {

    override fun searchFeed(
        filter: GroupBuyFeedFilter,
        districtFilters: Set<DistrictType>,
        pageable: Pageable,
        sortMode: FeedSortMode,
    ): Page<GroupBuy> {
        val now = LocalDateTime.now()
        val where = buildWhere(filter, districtFilters, now)
        val orderSpecifiers = buildOrderSpecifiers(sortMode)

        val content = queryFactory
            .selectFrom(groupBuy)
            .join(groupBuy.store, store).fetchJoin()
            .where(where)
            .orderBy(*orderSpecifiers.toTypedArray())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(groupBuy.count())
            .from(groupBuy)
            .join(groupBuy.store, store)
            .where(where)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    private fun buildWhere(
        filter: GroupBuyFeedFilter,
        districtFilters: Set<DistrictType>,
        now: LocalDateTime
    ): BooleanBuilder {
        val builder = BooleanBuilder()

        // 마감 공구 제외하고 진행 중 공고만
        builder.and(groupBuy.status.eq(GroupBuyStatus.IN_PROGRESS))
        builder.and(groupBuy.deadline.goe(now))

        if (districtFilters.isNotEmpty()) {
            builder.and(store.district.`in`(districtFilters))
        }

        filterPredicate(filter, now)?.let { builder.and(it) }
        return builder
    }

    private fun buildOrderSpecifiers(sortMode: FeedSortMode): List<OrderSpecifier<*>> {
        val achievementRate: NumberExpression<Int> = Expressions.cases()
            .`when`(groupBuy.targetQuantity.eq(0))
            .then(0)
            .otherwise(groupBuy.currentQuantity.multiply(100).divide(groupBuy.targetQuantity))

        val favoriteCount: NumberExpression<Long> = Expressions.numberTemplate(
            Long::class.java,
            "coalesce(({0}), 0)", // 서브쿼리 결과가 null이면 0으로 대체
            JPAExpressions.select(favorite.count())
                .from(favorite)
                .where(favorite.groupBuy.eq(groupBuy))
        )

        return when (sortMode) {
            FeedSortMode.REGIONAL -> listOf(
                achievementRate.desc(),  // 달성임박
                groupBuy.deadline.asc(), // 마감임박
                groupBuy.createdAt.desc(), // 최신
                groupBuy.id.desc()
            )
            FeedSortMode.NATIONWIDE_FALLBACK -> listOf(
                favoriteCount.desc(),    // 찜(총 찜수)
                achievementRate.desc(),  // 달성임박
                groupBuy.deadline.asc(), // 마감임박
                groupBuy.id.desc()
            )
        }
    }

    private fun filterPredicate(filter: GroupBuyFeedFilter, now: LocalDateTime): BooleanExpression? {
        return when (filter) {
            GroupBuyFeedFilter.ALL -> null
            GroupBuyFeedFilter.CLOSING_SOON ->
                groupBuy.deadline.loe(now.plusDays(GroupBuyRepositoryCustom.CLOSING_SOON_DAYS))
            GroupBuyFeedFilter.ALMOST_ACHIEVED ->
                groupBuy.currentQuantity.multiply(100)
                    .goe(groupBuy.targetQuantity.multiply(GroupBuyRepositoryCustom.ALMOST_ACHIEVED_RATE))
        }
    }

    override fun searchByIntent(
        region: String?,
        product: String?,
        now: LocalDateTime,
        status: GroupBuyStatus
    ): List<GroupBuy> {
        val regionType = region?.let { runCatching { RegionType.fromLabel(it) }.getOrNull() }
        val builder = BooleanBuilder()
        builder.and(groupBuy.status.eq(status))
        builder.and(groupBuy.deadline.gt(now))
        regionType?.let { builder.and(store.region.eq(it)) }
        product?.let { builder.and(groupBuy.productName.eq(it)) }

        return queryFactory
            .selectFrom(groupBuy)
            .join(groupBuy.store, store).fetchJoin()
            .where(builder)
            .fetch()
    }

    override fun findDistinctRegions(status: GroupBuyStatus): List<String> {
        return queryFactory
            .select(store.region).distinct()
            .from(groupBuy)
            .join(groupBuy.store, store)
            .where(groupBuy.status.eq(status))
            .fetch()
            .map { it.label }
    }

    override fun findDistinctProductNames(status: GroupBuyStatus): List<String> {
        return queryFactory
            .select(groupBuy.productName).distinct()
            .from(groupBuy)
            .where(groupBuy.status.eq(status))
            .fetch()
    }
}
