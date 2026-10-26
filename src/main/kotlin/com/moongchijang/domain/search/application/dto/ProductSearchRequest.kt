package com.moongchijang.domain.search.application.dto

import com.moongchijang.domain.product.domain.repository.ProductFeedSort
import com.moongchijang.domain.store.domain.entity.DistrictType
import jakarta.validation.constraints.NotBlank

data class ProductSearchRequest(
    @field:NotBlank
    val keyword: String,
    val categoryCodes: Set<String> = emptySet(),
    val districts: Set<DistrictType> = emptySet(),
    val pickupThisWeek: Boolean = false,
    val smallQuantity: Boolean = false,
    val newOnly: Boolean = false,
    val sort: ProductFeedSort = ProductFeedSort.LATEST,
    val cursor: String? = null,
    val limit: Int = 20,
)
