package com.moongchijang.domain.product.application.dto

import com.moongchijang.domain.product.domain.entity.ProductStorageType
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.StoreHolidayType
import java.time.LocalDate
import java.time.LocalTime

data class ProductDetailResponse(
    val productId: Long,
    val name: String,
    val description: String,
    val thumbnailUrl: String?,
    val imageUrls: List<String>,
    val unitPrice: Int,
    val minQuantity: Int,
    val maxQuantity: Int,
    val storageType: ProductStorageType,
    val store: ProductStoreResponse,
    val pickup: ProductPickupResponse,
    val canPurchase: Boolean,
    val unavailableReason: ProductUnavailableReason?,
)

data class ProductStoreResponse(
    val storeId: Long,
    val name: String,
    val region: RegionType,
    val district: DistrictType,
    val address: String,
    val phoneNumber: String?,
    val placeId: String?,
)

data class ProductPickupResponse(
    val preparationDays: Int,
    val availableFrom: LocalDate,
    val availableUntil: LocalDate,
    val availableDates: List<PickupDateResponse>,
    val holidays: List<StoreHolidayResponse>,
)

data class PickupDateResponse(
    val date: LocalDate,
    val timeSlots: List<PickupTimeSlotResponse>,
)

data class PickupTimeSlotResponse(
    val startTime: LocalTime,
    val endTime: LocalTime,
)

data class StoreHolidayResponse(
    val type: StoreHolidayType,
    val dayOfWeek: Int?,
    val date: LocalDate?,
    val reason: String?,
)

enum class ProductUnavailableReason {
    PRODUCT_NOT_FOR_SALE,
    STORE_NOT_FOR_SALE,
    NO_AVAILABLE_PICKUP_DATE,
}
