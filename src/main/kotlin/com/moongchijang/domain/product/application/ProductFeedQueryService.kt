package com.moongchijang.domain.product.application

import com.moongchijang.domain.favorite.domain.repository.FavoriteRepository
import com.moongchijang.domain.product.application.dto.ProductFeedItemResponse
import com.moongchijang.domain.product.application.dto.ProductFeedRequest
import com.moongchijang.domain.product.application.dto.ProductFeedResponse
import com.moongchijang.domain.product.application.dto.ProductFeedStoreResponse
import com.moongchijang.domain.product.domain.entity.Product
import com.moongchijang.domain.product.domain.repository.ProductFeedCriteria
import com.moongchijang.domain.product.domain.repository.ProductFeedCursor
import com.moongchijang.domain.product.domain.repository.ProductFeedSort
import com.moongchijang.domain.product.domain.repository.ProductPickupDateCondition
import com.moongchijang.domain.product.domain.repository.ProductRepository
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.repository.StoreHolidayRepository
import com.moongchijang.domain.store.domain.repository.StorePickupTimeSlotRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.time.kstNow
import com.moongchijang.global.time.kstToday
import com.moongchijang.global.util.S3ImageReferenceResolver
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.DayOfWeek
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

@Service
class ProductFeedQueryService(
    private val productRepository: ProductRepository,
    private val storeHolidayRepository: StoreHolidayRepository,
    private val storePickupTimeSlotRepository: StorePickupTimeSlotRepository,
    private val favoriteRepository: FavoriteRepository,
    private val s3ImageReferenceResolver: S3ImageReferenceResolver,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun getFeed(
        request: ProductFeedRequest,
        userId: Long?,
        candidateProductIds: Set<Long>? = null,
    ): ProductFeedResponse {
        validate(request)

        if (candidateProductIds?.isEmpty() == true) {
            return ProductFeedResponse(items = emptyList(), nextCursor = null, hasNext = false)
        }

        val today = clock.kstToday()
        val now = clock.kstNow()
        val safeLimit = request.limit.coerceIn(MIN_LIMIT, MAX_LIMIT)
        val criteria = ProductFeedCriteria(
            productIds = candidateProductIds,
            categoryCodes = request.categoryCodes.map { it.trim().uppercase() }.toSet(),
            districts = expandAllDistricts(request.districts),
            maxPreparationDays = if (request.pickupThisWeek) {
                ChronoUnit.DAYS.between(
                    today,
                    today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)),
                ).toInt()
            } else {
                null
            },
            pickupDateConditions = if (request.pickupThisWeek) {
                today.datesUntil(today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).plusDays(1))
                    .map { date ->
                        ProductPickupDateCondition(
                            date = date,
                            maxPreparationDays = ChronoUnit.DAYS.between(today, date).toInt(),
                        )
                    }
                    .toList()
            } else {
                emptyList()
            },
            maxMinQuantity = SMALL_QUANTITY_MAX.takeIf { request.smallQuantity },
            createdAtFrom = now.minusDays(NEW_PRODUCT_DAYS).takeIf { request.newOnly },
            sort = request.sort,
        )
        val cursor = request.cursor?.let { ProductFeedCursor.decode(it, request.sort) }
        val fetched = productRepository.findFeed(
            criteria = criteria,
            cursor = cursor,
            limit = safeLimit + 1,
        )
        val hasNext = fetched.size > safeLimit
        val products = if (hasNext) fetched.take(safeLimit) else fetched

        if (products.isEmpty()) {
            return ProductFeedResponse(items = emptyList(), nextCursor = null, hasNext = false)
        }

        val productIds = products.map { it.id }
        val storeIds = products.map { it.store.id }.toSet()
        val wishlistedProductIds = userId?.let {
            favoriteRepository.findProductIdsByUserIdAndProductIdIn(it, productIds)
        }.orEmpty()
        val holidaysByStoreId = storeHolidayRepository.findAllByStoreIdInAndActiveTrue(storeIds)
            .groupBy { it.store.id }
        val timeSlotsByStoreId = storePickupTimeSlotRepository
            .findAllByStoreIdInAndActiveTrueOrderByStoreIdAscDayOfWeekAscStartTimeAsc(storeIds)
            .groupBy { it.store.id }
        val newProductThreshold = now.minusDays(NEW_PRODUCT_DAYS)

        val items = products.map { product ->
            val availability = ProductAvailabilityCalculator.calculate(
                product = product,
                holidays = holidaysByStoreId[product.store.id].orEmpty(),
                timeSlots = timeSlotsByStoreId[product.store.id].orEmpty(),
                today = today,
            )
            ProductFeedItemResponse(
                productId = product.id,
                categoryCode = product.categoryCode,
                name = product.name,
                thumbnailUrl = s3ImageReferenceResolver.resolveForRead(product.thumbnailKey),
                unitPrice = product.unitPrice,
                minQuantity = product.minQuantity,
                maxQuantity = product.maxQuantity,
                storageType = product.storageType,
                store = ProductFeedStoreResponse(
                    storeId = product.store.id,
                    name = product.store.name,
                    region = product.store.region,
                    district = product.store.district,
                ),
                earliestPickupDate = availability.pickup.availableDates.firstOrNull()?.date,
                isNew = product.createdAt?.let { !it.isBefore(newProductThreshold) } ?: false,
                isWishlisted = product.id in wishlistedProductIds,
                canPurchase = availability.canPurchase,
                unavailableReason = availability.unavailableReason,
            )
        }

        return ProductFeedResponse(
            items = items,
            nextCursor = if (hasNext) createCursor(products.last(), request.sort).encode() else null,
            hasNext = hasNext,
        )
    }

    private fun validate(request: ProductFeedRequest) {
        if (request.categoryCodes.size > MAX_CATEGORY_COUNT ||
            request.categoryCodes.any { it.isBlank() || it.trim().length > MAX_CATEGORY_CODE_LENGTH }
        ) {
            throw CustomException(ErrorCode.INVALID_INPUT, "categoryCodes are invalid")
        }
        if (request.districts.size > MAX_DISTRICT_COUNT) {
            throw CustomException(ErrorCode.INVALID_INPUT, "districts can contain up to $MAX_DISTRICT_COUNT values")
        }
        request.districts.groupBy { it.region }.forEach { (_, districts) ->
            if (districts.any { it.name.endsWith("_ALL") } && districts.any { !it.name.endsWith("_ALL") }) {
                throw CustomException(ErrorCode.INVALID_INPUT, "region-wide and child districts cannot be selected together")
            }
        }
    }

    private fun expandAllDistricts(districts: Set<DistrictType>): Set<DistrictType> = districts
        .flatMap { district ->
            if (district.name.endsWith("_ALL")) {
                DistrictType.findLeafByRegion(district.region)
            } else {
                listOf(district)
            }
        }
        .toSet()

    private fun createCursor(product: Product, sort: ProductFeedSort): ProductFeedCursor = when (sort) {
        ProductFeedSort.LATEST -> ProductFeedCursor.Latest(
            createdAt = checkNotNull(product.createdAt) { "Persisted product must have createdAt" },
            id = product.id,
        )
        ProductFeedSort.PRICE_LOW -> ProductFeedCursor.PriceLow(
            unitPrice = product.unitPrice,
            id = product.id,
        )
        ProductFeedSort.MIN_QUANTITY -> ProductFeedCursor.MinQuantity(
            minQuantity = product.minQuantity,
            id = product.id,
        )
    }

    companion object {
        private const val MIN_LIMIT = 1
        private const val MAX_LIMIT = 100
        private const val MAX_CATEGORY_COUNT = 20
        private const val MAX_CATEGORY_CODE_LENGTH = 50
        private const val MAX_DISTRICT_COUNT = 10
        private const val SMALL_QUANTITY_MAX = 10
        private const val NEW_PRODUCT_DAYS = 7L
    }
}
