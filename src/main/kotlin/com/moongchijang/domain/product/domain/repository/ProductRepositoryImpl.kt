package com.moongchijang.domain.product.domain.repository

import com.moongchijang.domain.product.domain.entity.Product
import com.moongchijang.domain.product.domain.entity.ProductStatus
import com.moongchijang.domain.product.domain.entity.QProduct.product
import com.moongchijang.domain.store.domain.entity.QStoreHoliday
import com.moongchijang.domain.store.domain.entity.QStorePickupTimeSlot
import com.moongchijang.domain.store.domain.entity.QStore.store
import com.moongchijang.domain.store.domain.entity.StoreHolidayType
import com.moongchijang.domain.store.domain.entity.StoreSalesStatus
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class ProductRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : ProductRepositoryCustom {

    override fun findFeed(
        criteria: ProductFeedCriteria,
        cursor: ProductFeedCursor?,
        limit: Int,
    ): List<Product> {
        val where = BooleanBuilder()
            .and(product.status.eq(ProductStatus.ACTIVE))
            .and(store.salesStatus.eq(StoreSalesStatus.ACTIVE))

        criteria.categoryCodes
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() }
            .toSet()
            .takeIf { it.isNotEmpty() }
            ?.let { where.and(product.categoryCode.`in`(it)) }
        criteria.districts.takeIf { it.isNotEmpty() }
            ?.let { where.and(store.district.`in`(it)) }
        criteria.maxPreparationDays?.let { where.and(product.preparationDays.loe(it)) }
        pickupDatePredicate(criteria.pickupDateConditions)?.let { where.and(it) }
        criteria.maxMinQuantity?.let { where.and(product.minQuantity.loe(it)) }
        criteria.createdAtFrom?.let { where.and(product.createdAt.goe(it)) }
        cursorPredicate(cursor)?.let { where.and(it) }

        return queryFactory
            .selectFrom(product)
            .join(product.store, store).fetchJoin()
            .where(where)
            .orderBy(*orderSpecifiers(criteria.sort).toTypedArray())
            .limit(limit.toLong())
            .fetch()
    }

    private fun pickupDatePredicate(
        conditions: List<ProductPickupDateCondition>,
    ): Predicate? {
        if (conditions.isEmpty()) {
            return null
        }

        val pickupSlot = QStorePickupTimeSlot("productFeedPickupSlot")
        val holiday = QStoreHoliday("productFeedHoliday")
        val availableDate = BooleanBuilder()

        conditions.distinctBy { it.date }.forEach { condition ->
            val hasPickupSlot = JPAExpressions.selectOne()
                .from(pickupSlot)
                .where(
                    pickupSlot.store.eq(product.store),
                    pickupSlot.active.isTrue,
                    pickupSlot.dayOfWeek.eq(condition.date.dayOfWeek.value),
                )
                .exists()
            val hasHoliday = JPAExpressions.selectOne()
                .from(holiday)
                .where(
                    holiday.store.eq(product.store),
                    holiday.active.isTrue,
                    holiday.holidayType.eq(StoreHolidayType.REGULAR)
                        .and(holiday.dayOfWeek.eq(condition.date.dayOfWeek.value))
                        .or(
                            holiday.holidayType.eq(StoreHolidayType.TEMPORARY)
                                .and(holiday.holidayDate.eq(condition.date))
                        ),
                )
                .exists()

            availableDate.or(
                product.preparationDays.loe(condition.maxPreparationDays)
                    .and(hasPickupSlot)
                    .and(hasHoliday.not())
            )
        }

        return availableDate.value
    }

    private fun cursorPredicate(cursor: ProductFeedCursor?): BooleanExpression? = when (cursor) {
        null -> null
        is ProductFeedCursor.Latest ->
            product.createdAt.lt(cursor.createdAt)
                .or(product.createdAt.eq(cursor.createdAt).and(product.id.lt(cursor.id)))
        is ProductFeedCursor.PriceLow ->
            product.unitPrice.gt(cursor.unitPrice)
                .or(product.unitPrice.eq(cursor.unitPrice).and(product.id.gt(cursor.id)))
        is ProductFeedCursor.MinQuantity ->
            product.minQuantity.gt(cursor.minQuantity)
                .or(product.minQuantity.eq(cursor.minQuantity).and(product.id.gt(cursor.id)))
    }

    private fun orderSpecifiers(sort: ProductFeedSort): List<OrderSpecifier<*>> = when (sort) {
        ProductFeedSort.LATEST -> listOf(product.createdAt.desc(), product.id.desc())
        ProductFeedSort.PRICE_LOW -> listOf(product.unitPrice.asc(), product.id.asc())
        ProductFeedSort.MIN_QUANTITY -> listOf(product.minQuantity.asc(), product.id.asc())
    }
}
