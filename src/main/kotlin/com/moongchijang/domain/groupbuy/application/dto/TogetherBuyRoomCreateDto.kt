package com.moongchijang.domain.groupbuy.application.dto

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class TogetherBuyRoomCreateRequest(
    @field:Min(1)
    val storeId: Long,

    @field:Min(1)
    val storeProductId: Long,

    @field:NotNull
    val pickupDate: LocalDate,

    @field:NotNull
    val pickupSlotStart: LocalTime,

    @field:NotNull
    val pickupSlotEnd: LocalTime,

    @field:Min(1)
    val organizerQuantity: Int,

    @field:AssertTrue(message = "재판매 금지 동의는 필수입니다.")
    val resaleProhibitionAgreed: Boolean,

    val isSoloRoom: Boolean,
)

data class TogetherBuyRoomCreatedResponse(
    val groupBuyId: Long,
    val status: TogetherBuyRoomStatus,
    val organizer: TogetherBuyOrganizerResponse,
    val store: TogetherBuyStoreResponse,
    val product: TogetherBuyProductResponse,
    val pickup: TogetherBuyPickupResponse,
    val progress: TogetherBuyProgressResponse,
    val paymentDeadline: LocalDateTime,
    val isSoloRoom: Boolean,
    val resaleProhibitionAgreed: Boolean,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(groupBuy: GroupBuy, createdAt: LocalDateTime): TogetherBuyRoomCreatedResponse {
            val product = requireNotNull(groupBuy.product)
            val organizer = requireNotNull(groupBuy.organizer)
            return TogetherBuyRoomCreatedResponse(
                groupBuyId = groupBuy.id,
                status = TogetherBuyRoomStatus.RECRUITING,
                organizer = TogetherBuyOrganizerResponse(
                    userId = requireNotNull(organizer.id),
                    nickname = organizer.nickname,
                ),
                store = TogetherBuyStoreResponse(
                    storeId = groupBuy.store.id,
                    name = groupBuy.store.name,
                    address = groupBuy.store.address,
                    phoneNumber = groupBuy.store.phoneNumber,
                ),
                product = TogetherBuyProductResponse(
                    storeProductId = product.id,
                    name = product.name,
                    unitPrice = groupBuy.price,
                    minQuantity = requireNotNull(groupBuy.minQuantity),
                    maxQuantity = groupBuy.maxQuantity,
                ),
                pickup = TogetherBuyPickupResponse(
                    date = groupBuy.pickupDate,
                    slotStart = groupBuy.pickupTimeStart,
                    slotEnd = groupBuy.pickupTimeEnd,
                    address = groupBuy.pickupLocation,
                ),
                progress = TogetherBuyProgressResponse(
                    totalQuantity = groupBuy.currentQuantity,
                    minQuantity = requireNotNull(groupBuy.minQuantity),
                    maxQuantity = groupBuy.maxQuantity,
                    remainingQuantity = groupBuy.maxQuantity - groupBuy.currentQuantity,
                ),
                paymentDeadline = requireNotNull(groupBuy.paymentDeadline),
                isSoloRoom = groupBuy.isSoloRoom,
                resaleProhibitionAgreed = groupBuy.resaleProhibitionAgreed,
                createdAt = groupBuy.createdAt ?: createdAt,
            )
        }
    }
}

enum class TogetherBuyRoomStatus {
    RECRUITING,
}

data class TogetherBuyOrganizerResponse(
    val userId: Long,
    val nickname: String?,
)

data class TogetherBuyStoreResponse(
    val storeId: Long,
    val name: String,
    val address: String,
    val phoneNumber: String?,
)

data class TogetherBuyProductResponse(
    val storeProductId: Long,
    val name: String,
    val unitPrice: Int,
    val minQuantity: Int,
    val maxQuantity: Int,
)

data class TogetherBuyPickupResponse(
    val date: LocalDate,
    val slotStart: LocalTime,
    val slotEnd: LocalTime,
    val address: String,
)

data class TogetherBuyProgressResponse(
    val totalQuantity: Int,
    val minQuantity: Int,
    val maxQuantity: Int,
    val remainingQuantity: Int,
)
