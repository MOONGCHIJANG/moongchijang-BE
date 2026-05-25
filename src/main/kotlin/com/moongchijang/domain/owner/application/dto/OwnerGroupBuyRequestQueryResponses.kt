package com.moongchijang.domain.owner.application.dto

import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequest
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestImage
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestStatus
import org.springframework.data.domain.Page
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class OwnerGroupBuyRequestPageResponse(
    val content: List<OwnerGroupBuyRequestListItemResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int
) {
    companion object {
        fun from(page: Page<OwnerGroupBuyRequest>): OwnerGroupBuyRequestPageResponse =
            OwnerGroupBuyRequestPageResponse(
                content = page.content.map { OwnerGroupBuyRequestListItemResponse.from(it) },
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                number = page.number,
                size = page.size
            )
    }
}

data class OwnerGroupBuyRequestListItemResponse(
    val requestId: Long,
    val productName: String,
    val storeName: String,
    val originalPrice: Int?,
    val price: Int,
    val targetQuantity: Int,
    val pickupDate: LocalDate,
    val requestedAt: LocalDateTime?,
    val status: OwnerGroupBuyRequestStatus,
    val rejectionReason: String?,
    val approvedGroupBuyId: Long?
) {
    companion object {
        fun from(request: OwnerGroupBuyRequest): OwnerGroupBuyRequestListItemResponse =
            OwnerGroupBuyRequestListItemResponse(
                requestId = request.id,
                productName = request.productName,
                storeName = request.store.name,
                originalPrice = request.originalPrice,
                price = request.price,
                targetQuantity = request.targetQuantity,
                pickupDate = request.pickupDate,
                requestedAt = request.createdAt,
                status = request.status,
                rejectionReason = request.rejectionReason,
                approvedGroupBuyId = request.approvedGroupBuy?.id
            )
    }
}

data class OwnerGroupBuyRequestDetailResponse(
    val requestId: Long,
    val storeId: Long,
    val storeName: String,
    val productName: String,
    val productDescription: String,
    val originalPrice: Int?,
    val price: Int,
    val targetQuantity: Int,
    val maxQuantity: Int,
    val perUserLimit: Int?,
    val thumbnailUrl: String,
    val imageUrls: List<String>,
    val deadline: LocalDateTime,
    val pickupDate: LocalDate,
    val pickupTimeStart: LocalTime,
    val pickupTimeEnd: LocalTime,
    val pickupLocation: String,
    val pickupContact: String?,
    val status: OwnerGroupBuyRequestStatus,
    val rejectionReason: String?,
    val approvedGroupBuyId: Long?,
    val reviewedAt: LocalDateTime?,
    val requestedAt: LocalDateTime?
) {
    companion object {
        fun from(
            request: OwnerGroupBuyRequest,
            images: List<OwnerGroupBuyRequestImage>
        ): OwnerGroupBuyRequestDetailResponse =
            OwnerGroupBuyRequestDetailResponse(
                requestId = request.id,
                storeId = request.store.id,
                storeName = request.store.name,
                productName = request.productName,
                productDescription = request.productDescription,
                originalPrice = request.originalPrice,
                price = request.price,
                targetQuantity = request.targetQuantity,
                maxQuantity = request.maxQuantity,
                perUserLimit = request.perUserLimit,
                thumbnailUrl = request.thumbnailUrl,
                imageUrls = images.map { it.imageUrl },
                deadline = request.deadline,
                pickupDate = request.pickupDate,
                pickupTimeStart = request.pickupTimeStart,
                pickupTimeEnd = request.pickupTimeEnd,
                pickupLocation = request.pickupLocation,
                pickupContact = request.pickupContact,
                status = request.status,
                rejectionReason = request.rejectionReason,
                approvedGroupBuyId = request.approvedGroupBuy?.id,
                reviewedAt = request.reviewedAt,
                requestedAt = request.createdAt
            )
    }
}
