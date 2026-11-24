package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.application.dto.TogetherBuyRoomCreateRequest
import com.moongchijang.domain.groupbuy.application.dto.TogetherBuyRoomCreatedResponse
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.product.domain.entity.Product
import com.moongchijang.domain.product.domain.entity.ProductStatus
import com.moongchijang.domain.product.domain.repository.ProductRepository
import com.moongchijang.domain.store.application.StorePickupPolicyService
import com.moongchijang.domain.store.domain.entity.StoreSalesStatus
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.time.kstNow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@Transactional
class TogetherBuyRoomCommandService(
    private val groupBuyRepository: GroupBuyRepository,
    private val participationRepository: ParticipationRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val storePickupPolicyService: StorePickupPolicyService,
    private val clock: Clock,
) {

    fun create(userId: Long, request: TogetherBuyRoomCreateRequest): TogetherBuyRoomCreatedResponse {
        if (!request.resaleProhibitionAgreed) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }
        val organizer = userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        val product = productRepository.findWithStoreById(request.storeProductId)
            ?: throw CustomException(ErrorCode.PRODUCT_NOT_FOUND)

        validateProductAndQuantity(product, request)
        val paymentDeadline = findPaymentDeadline(product, request)
        val now = clock.kstNow()
        val room = groupBuyRepository.save(
            GroupBuy(
                store = product.store,
                product = product,
                organizer = organizer,
                thumbnailKey = product.thumbnailKey,
                productName = product.name,
                productDescription = product.description,
                price = product.unitPrice,
                targetQuantity = product.minQuantity,
                minQuantity = product.minQuantity,
                currentQuantity = request.organizerQuantity,
                maxQuantity = product.maxQuantity,
                isSoloRoom = request.isSoloRoom,
                resaleProhibitionAgreed = request.resaleProhibitionAgreed,
                status = GroupBuyStatus.IN_PROGRESS,
                recruitmentStartAt = now,
                deadline = paymentDeadline,
                paymentDeadline = paymentDeadline,
                pickupDate = request.pickupDate,
                pickupTimeStart = request.pickupSlotStart,
                pickupTimeEnd = request.pickupSlotEnd,
                pickupLocation = product.store.address,
                pickupContact = product.store.phoneNumber,
            ),
        )
        participationRepository.save(
            Participation(
                user = organizer,
                groupBuy = room,
                quantity = request.organizerQuantity,
                productAmount = product.unitPrice * request.organizerQuantity,
                feeAmount = 0,
                totalAmount = product.unitPrice * request.organizerQuantity,
            ),
        )

        return TogetherBuyRoomCreatedResponse.from(room, now)
    }

    private fun validateProductAndQuantity(product: Product, request: TogetherBuyRoomCreateRequest) {
        if (product.store.id != request.storeId ||
            product.status != ProductStatus.ACTIVE ||
            product.store.salesStatus != StoreSalesStatus.ACTIVE
        ) {
            throw CustomException(ErrorCode.GROUPBUY_ROOM_PRODUCT_NOT_AVAILABLE)
        }
        if (request.organizerQuantity !in 1..product.maxQuantity ||
            (request.isSoloRoom && request.organizerQuantity < product.minQuantity)
        ) {
            throw CustomException(ErrorCode.GROUPBUY_ROOM_INVALID_QUANTITY)
        }
    }

    private fun findPaymentDeadline(product: Product, request: TogetherBuyRoomCreateRequest): java.time.LocalDateTime {
        val date = storePickupPolicyService
            .getAvailability(product.store.id, request.pickupDate, request.pickupDate)
            .dates
            .single()
        val isSelectedSlot = date.selectable && date.slots.any {
            it.selectable &&
                it.startTime == request.pickupSlotStart &&
                it.endTime == request.pickupSlotEnd
        }
        if (!isSelectedSlot) {
            throw CustomException(ErrorCode.GROUPBUY_ROOM_PICKUP_NOT_AVAILABLE)
        }
        return requireNotNull(date.paymentDeadline)
    }
}
