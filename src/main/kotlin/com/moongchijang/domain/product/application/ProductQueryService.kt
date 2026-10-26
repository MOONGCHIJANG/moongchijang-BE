package com.moongchijang.domain.product.application

import com.moongchijang.domain.favorite.domain.repository.FavoriteRepository
import com.moongchijang.domain.product.application.dto.ProductDetailResponse
import com.moongchijang.domain.product.application.dto.ProductStoreResponse
import com.moongchijang.domain.product.domain.repository.ProductImageRepository
import com.moongchijang.domain.product.domain.repository.ProductRepository
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
        val availability = ProductAvailabilityCalculator.calculate(
            product = product,
            holidays = holidays,
            timeSlots = timeSlots,
            today = clock.kstToday(),
        )

        return ProductDetailResponse(
            productId = product.id,
            categoryCode = product.categoryCode,
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
            pickup = availability.pickup,
            isWishlisted = userId?.let { favoriteRepository.existsByUserIdAndProductId(it, product.id) } ?: false,
            canPurchase = availability.canPurchase,
            unavailableReason = availability.unavailableReason,
        )
    }
}
