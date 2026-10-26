package com.moongchijang.domain.product.domain.repository

import com.moongchijang.domain.store.domain.entity.DistrictType
import java.time.LocalDate
import java.time.LocalDateTime

data class ProductFeedCriteria(
    val categoryCodes: Set<String> = emptySet(),
    val districts: Set<DistrictType> = emptySet(),
    val maxPreparationDays: Int? = null,
    val pickupDateConditions: List<ProductPickupDateCondition> = emptyList(),
    val maxMinQuantity: Int? = null,
    val createdAtFrom: LocalDateTime? = null,
    val sort: ProductFeedSort = ProductFeedSort.LATEST,
)

data class ProductPickupDateCondition(
    val date: LocalDate,
    val maxPreparationDays: Int,
)
