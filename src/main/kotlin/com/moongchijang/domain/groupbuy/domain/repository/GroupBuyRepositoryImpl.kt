package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedFilter
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.entity.QGroupBuy.groupBuy
import com.moongchijang.domain.store.domain.entity.QStore.store
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.dsl.BooleanExpression
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
        keyword: String?,
        pageable: Pageable,
    ): Page<GroupBuy> {
        val now = LocalDateTime.now()
        val where = buildWhere(filter, districtFilters, keyword, now)

        val content = queryFactory
            .selectFrom(groupBuy)
            .join(groupBuy.store, store).fetchJoin()
            .where(where)
            .orderBy(groupBuy.createdAt.desc()) // MVP: 최신순 고정, 추후 sortType 기반 동적 정렬로 확장 예정
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
        keyword: String?,
        now: LocalDateTime
    ): BooleanBuilder {
        val builder = BooleanBuilder()

        // 마감 공구 제외하고 진행 중 공고만
        builder.and(groupBuy.status.eq(GroupBuyStatus.IN_PROGRESS))
        builder.and(groupBuy.deadline.goe(now))

        keyword?.trim()?.takeIf { it.isNotEmpty() }?.let {
            builder.and(
                groupBuy.productName.containsIgnoreCase(it)
                        .or(store.name.containsIgnoreCase(it))
            )
        }

        if (districtFilters.isNotEmpty()) {
            builder.and(store.district.`in`(districtFilters))
        }

        filterPredicate(filter, now)?.let { builder.and(it) }
        return builder
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
}
