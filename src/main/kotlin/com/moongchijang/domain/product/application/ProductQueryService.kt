package com.moongchijang.domain.product.application

import com.moongchijang.domain.favorite.domain.repository.FavoriteRepository
import com.moongchijang.domain.product.application.dto.PickupDateResponse
import com.moongchijang.domain.product.application.dto.PickupTimeSlotResponse
import com.moongchijang.domain.product.application.dto.ProductDetailResponse
import com.moongchijang.domain.product.application.dto.ProductPickupResponse
import com.moongchijang.domain.product.application.dto.ProductStoreResponse
import com.moongchijang.domain.product.application.dto.ProductUnavailableReason
import com.moongchijang.domain.product.application.dto.StoreHolidayResponse
import com.moongchijang.domain.product.domain.entity.ProductStatus
import com.moongchijang.domain.product.domain.repository.ProductImageRepository
import com.moongchijang.domain.product.domain.repository.ProductRepository
import com.moongchijang.domain.store.domain.entity.StoreHolidayType
import com.moongchijang.domain.store.domain.entity.StoreSalesStatus
import com.moongchijang.domain.store.domain.repository.StoreHolidayRepository
import com.moongchijang.domain.store.domain.repository.StorePickupTimeSlotRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.time.kstToday
import com.moongchijang.global.util.S3ImageReferenceResolver
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class ProductQueryService(
    private val productRepository: ProductRepository,
    private val productImageRepository: ProductImageRepository,
    private val storeHolidayRepository: StoreHolidayRepository,
    private val storePickupTimeSlotRepository: StorePickupTimeSlotRepository,
    private val favoriteRepository: FavoriteRepository,
    private val s3ImageReferenceResolver: S3ImageReferenceResolver,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun getDetail(productId: Long, userId: Long?): ProductDetailResponse {
        val product = productRepository.findWithStoreById(productId)
            ?: throw CustomException(ErrorCode.PRODUCT_NOT_FOUND)
        val store = product.store
        val holidays = storeHolidayRepository.findAllByStoreIdAndActiveTrue(store.id)
        val timeSlots = storePickupTimeSlotRepository
            .findAllByStoreIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(store.id)

        val today = clock.kstToday()
        val availableFrom = today.plusDays(product.preparationDays.toLong())
        val availableUntil = today.plusDays(MAX_PICKUP_DAYS)
        val regularHolidayDays = holidays
            .filter { it.holidayType == StoreHolidayType.REGULAR }
            .mapNotNull { it.dayOfWeek }
            .toSet()
        val temporaryHolidayDates = holidays
            .filter { it.holidayType == StoreHolidayType.TEMPORARY }
            .mapNotNull { it.holidayDate }
            .toSet()
        val timeSlotsByDay = timeSlots.groupBy { it.dayOfWeek }

        val availableDates = if (availableFrom.isAfter(availableUntil)) {
            emptyList()
        } else {
            availableFrom.datesUntil(availableUntil.plusDays(1))
                .filter { date ->
                    date.dayOfWeek.value !in regularHolidayDays &&
                        date !in temporaryHolidayDates &&
                        timeSlotsByDay[date.dayOfWeek.value].orEmpty().isNotEmpty()
                }
                .map { date ->
                    PickupDateResponse(
                        date = date,
                        timeSlots = timeSlotsByDay[date.dayOfWeek.value].orEmpty().map { slot ->
                            PickupTimeSlotResponse(
                                startTime = slot.startTime,
                                endTime = slot.endTime,
                            )
                        },
                    )
                }
                .toList()
        }

        val unavailableReason = when {
            product.status != ProductStatus.ACTIVE -> ProductUnavailableReason.PRODUCT_NOT_FOR_SALE
            store.salesStatus != StoreSalesStatus.ACTIVE -> ProductUnavailableReason.STORE_NOT_FOR_SALE
            availableDates.isEmpty() -> ProductUnavailableReason.NO_AVAILABLE_PICKUP_DATE
            else -> null
        }

        return ProductDetailResponse(
            productId = product.id,
            name = product.name,
            description = product.description,
            thumbnailUrl = s3ImageReferenceResolver.resolveForRead(product.thumbnailKey),
            imageUrls = productImageRepository.findAllByProductIdOrderBySortOrderAscIdAsc(product.id)
                .mapNotNull { s3ImageReferenceResolver.resolveForRead(it.imageKey) },
            unitPrice = product.unitPrice,
            minQuantity = product.minQuantity,
            maxQuantity = product.maxQuantity,
            storageType = product.storageType,
            store = ProductStoreResponse(
                storeId = store.id,
                name = store.name,
                region = store.region,
                district = store.district,
                address = store.address,
                phoneNumber = store.phoneNumber,
                placeId = store.placeId,
            ),
            pickup = ProductPickupResponse(
                preparationDays = product.preparationDays,
                availableFrom = availableFrom,
                availableUntil = availableUntil,
                availableDates = availableDates,
                holidays = holidays.map {
                    StoreHolidayResponse(
                        type = it.holidayType,
                        dayOfWeek = it.dayOfWeek,
                        date = it.holidayDate,
                        reason = it.reason,
                    )
                },
            ),
            isWishlisted = userId?.let { favoriteRepository.existsByUserIdAndProductId(it, product.id) } ?: false,
            canPurchase = unavailableReason == null,
            unavailableReason = unavailableReason,
        )
    }

    companion object {
        private const val MAX_PICKUP_DAYS = 14L
    }
}
