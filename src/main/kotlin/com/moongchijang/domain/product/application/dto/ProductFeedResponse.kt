package com.moongchijang.domain.product.application.dto

import com.moongchijang.domain.product.domain.entity.ProductStorageType
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import java.time.LocalDate

data class ProductFeedResponse(
    val items: List<ProductFeedItemResponse>,
    val nextCursor: String?,
    val hasNext: Boolean,
)

data class ProductFeedItemResponse(
    val productId: Long,
    val categoryCode: String,
    val name: String,
    val thumbnailUrl: String?,
    val unitPrice: Int,
    val minQuantity: Int,
    val maxQuantity: Int,
    val storageType: ProductStorageType,
    val store: ProductFeedStoreResponse,
    val earliestPickupDate: LocalDate?,
    val isNew: Boolean,
    val isWishlisted: Boolean,
    val canPurchase: Boolean,
    val unavailableReason: ProductUnavailableReason?,
    val activeTogetherRoomCount: Long = 0,
)

data class ProductFeedStoreResponse(
    val storeId: Long,
    val name: String,
    val region: RegionType,
    val district: DistrictType,
)
