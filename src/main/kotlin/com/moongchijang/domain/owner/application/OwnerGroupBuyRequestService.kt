package com.moongchijang.domain.owner.application

import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyRequestCreateRequest
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyRequestCreateResponse
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequest
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestImage
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestStatus
import com.moongchijang.domain.owner.domain.repository.OwnerGroupBuyRequestImageRepository
import com.moongchijang.domain.owner.domain.repository.OwnerGroupBuyRequestRepository
import com.moongchijang.domain.store.domain.repository.StoreRepository
import com.moongchijang.domain.store.domain.repository.StoreStaffRepository
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class OwnerGroupBuyRequestService(
    private val userRepository: UserRepository,
    private val storeRepository: StoreRepository,
    private val storeStaffRepository: StoreStaffRepository,
    private val ownerGroupBuyRequestRepository: OwnerGroupBuyRequestRepository,
    private val ownerGroupBuyRequestImageRepository: OwnerGroupBuyRequestImageRepository
) {

    fun create(ownerId: Long, request: OwnerGroupBuyRequestCreateRequest): OwnerGroupBuyRequestCreateResponse {
        val owner = userRepository.findByIdAndDeletedAtIsNull(ownerId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        if (owner.role != UserRole.SELLER) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        val store = storeRepository.findById(request.storeId)
            .orElseThrow { CustomException(ErrorCode.STORE_NOT_FOUND) }

        if (!storeStaffRepository.existsByUserIdAndStoreId(ownerId, request.storeId)) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        validateRequest(request)

        val saved = ownerGroupBuyRequestRepository.save(
            OwnerGroupBuyRequest(
                owner = owner,
                store = store,
                productName = request.productName.trim(),
                productDescription = request.productDescription.trim(),
                originalPrice = request.originalPrice,
                price = request.price,
                targetQuantity = request.targetQuantity,
                maxQuantity = request.maxQuantity,
                perUserLimit = request.perUserLimit,
                thumbnailUrl = request.imageUrls.first().trim(),
                deadline = request.deadline,
                pickupDate = request.pickupDate,
                pickupTimeStart = request.pickupTimeStart,
                pickupTimeEnd = request.pickupTimeEnd,
                pickupLocation = request.pickupLocation.trim(),
                pickupContact = request.pickupContact?.trim()?.ifBlank { null },
                status = OwnerGroupBuyRequestStatus.PENDING
            )
        )

        ownerGroupBuyRequestImageRepository.saveAll(
            request.imageUrls.mapIndexed { index, imageUrl ->
                OwnerGroupBuyRequestImage(
                    request = saved,
                    imageUrl = imageUrl.trim(),
                    sortOrder = index
                )
            }
        )

        return OwnerGroupBuyRequestCreateResponse(
            requestId = saved.id,
            status = saved.status
        )
    }

    private fun validateRequest(request: OwnerGroupBuyRequestCreateRequest) {
        if (request.deadline.isBefore(LocalDateTime.now().plusDays(MIN_RECRUITING_DAYS))) {
            throw CustomException(ErrorCode.OWNER_GROUPBUY_REQUEST_INVALID_DEADLINE)
        }

        if (request.maxQuantity < request.targetQuantity) {
            throw CustomException(ErrorCode.OWNER_GROUPBUY_REQUEST_INVALID_QUANTITY)
        }

        if (request.perUserLimit != null && request.perUserLimit > request.maxQuantity) {
            throw CustomException(ErrorCode.OWNER_GROUPBUY_REQUEST_INVALID_QUANTITY)
        }

        if (!request.pickupTimeEnd.isAfter(request.pickupTimeStart)) {
            throw CustomException(ErrorCode.OWNER_GROUPBUY_REQUEST_INVALID_PICKUP_TIME)
        }

        if (request.pickupDate.isBefore(request.deadline.toLocalDate())) {
            throw CustomException(ErrorCode.OWNER_GROUPBUY_REQUEST_INVALID_PICKUP_DATE)
        }
    }

    private companion object {
        const val MIN_RECRUITING_DAYS = 7L
    }
}
