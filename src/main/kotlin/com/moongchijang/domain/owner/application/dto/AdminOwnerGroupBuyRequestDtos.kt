package com.moongchijang.domain.owner.application.dto

import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequest
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestImage
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.data.domain.Page
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class AdminOwnerGroupBuyRequestPageResponse(
    val content: List<AdminOwnerGroupBuyRequestListItemResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int
) {
    companion object {
        fun from(page: Page<OwnerGroupBuyRequest>, now: LocalDateTime): AdminOwnerGroupBuyRequestPageResponse =
            AdminOwnerGroupBuyRequestPageResponse(
                content = page.content.map { AdminOwnerGroupBuyRequestListItemResponse.from(it, now) },
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                number = page.number,
                size = page.size
            )
    }
}

data class AdminOwnerGroupBuyRequestListItemResponse(
    val requestId: Long,
    val requestType: String,
    val productName: String,
    val storeName: String,
    val ownerName: String?,
    val originalPrice: Int?,
    val price: Int,
    val discountRate: Int?,
    val targetQuantity: Int,
    val pickupDate: LocalDate,
    val requestedAt: LocalDateTime?,
    val reviewElapsedMinutes: Long,
    val status: OwnerGroupBuyRequestStatus,
    val actionable: Boolean,
    val approvedGroupBuyId: Long?
) {
    companion object {
        fun from(request: OwnerGroupBuyRequest, now: LocalDateTime): AdminOwnerGroupBuyRequestListItemResponse =
            AdminOwnerGroupBuyRequestListItemResponse(
                requestId = request.id,
                requestType = REQUEST_TYPE,
                productName = request.productName,
                storeName = request.store.name,
                ownerName = request.owner.nickname,
                originalPrice = request.originalPrice,
                price = request.price,
                discountRate = request.discountRate(),
                targetQuantity = request.targetQuantity,
                pickupDate = request.pickupDate,
                requestedAt = request.createdAt,
                reviewElapsedMinutes = request.createdAt?.let { Duration.between(it, now).toMinutes().coerceAtLeast(0) } ?: 0,
                status = request.status,
                actionable = request.status == OwnerGroupBuyRequestStatus.PENDING,
                approvedGroupBuyId = request.approvedGroupBuy?.id
            )
    }
}

data class AdminOwnerGroupBuyRequestDetailResponse(
    val requestId: Long,
    val requestType: String,
    val status: OwnerGroupBuyRequestStatus,
    val owner: AdminOwnerGroupBuyRequestOwnerResponse,
    val store: AdminOwnerGroupBuyRequestStoreResponse,
    val product: AdminOwnerGroupBuyRequestProductResponse,
    val recruitment: AdminOwnerGroupBuyRequestRecruitmentResponse,
    val images: List<AdminOwnerGroupBuyRequestImageResponse>,
    val imageCount: Int,
    val rejectionReason: String?,
    val approvedGroupBuyId: Long?,
    val reviewedAt: LocalDateTime?,
    val requestedAt: LocalDateTime?,
    val actionable: Boolean
) {
    companion object {
        fun from(
            request: OwnerGroupBuyRequest,
            images: List<OwnerGroupBuyRequestImage>,
            imageUrls: List<String>,
        ): AdminOwnerGroupBuyRequestDetailResponse =
            AdminOwnerGroupBuyRequestDetailResponse(
                requestId = request.id,
                requestType = REQUEST_TYPE,
                status = request.status,
                owner = AdminOwnerGroupBuyRequestOwnerResponse(
                    ownerId = request.owner.id,
                    nickname = request.owner.nickname,
                    phoneNumber = request.owner.phoneNumber,
                    email = request.owner.email
                ),
                store = AdminOwnerGroupBuyRequestStoreResponse(
                    storeId = request.store.id,
                    storeName = request.store.name,
                    address = request.store.address,
                    phoneNumber = request.store.phoneNumber
                ),
                product = AdminOwnerGroupBuyRequestProductResponse(
                    productName = request.productName,
                    productDescription = request.productDescription,
                    originalPrice = request.originalPrice,
                    price = request.price,
                    discountRate = request.discountRate(),
                    targetQuantity = request.targetQuantity,
                    maxQuantity = request.maxQuantity,
                    perUserLimit = request.perUserLimit
                ),
                recruitment = AdminOwnerGroupBuyRequestRecruitmentResponse(
                    recruitmentStartAt = request.approvedGroupBuy?.recruitmentStartAt,
                    deadline = request.deadline,
                    pickupDate = request.pickupDate,
                    pickupTimeStart = request.pickupTimeStart,
                    pickupTimeEnd = request.pickupTimeEnd,
                    pickupLocation = request.pickupLocation,
                    pickupContact = request.pickupContact
                ),
                images = images.mapIndexed { index, image ->
                    AdminOwnerGroupBuyRequestImageResponse(
                        imageUrl = imageUrls.getOrNull(index).orEmpty(),
                        sortOrder = image.sortOrder
                    )
                },
                imageCount = images.size,
                rejectionReason = request.rejectionReason,
                approvedGroupBuyId = request.approvedGroupBuy?.id,
                reviewedAt = request.reviewedAt,
                requestedAt = request.createdAt,
                actionable = request.status == OwnerGroupBuyRequestStatus.PENDING
            )
    }
}

data class AdminOwnerGroupBuyRequestOwnerResponse(
    val ownerId: Long?,
    val nickname: String?,
    val phoneNumber: String?,
    val email: String?
)

data class AdminOwnerGroupBuyRequestStoreResponse(
    val storeId: Long,
    val storeName: String,
    val address: String,
    val phoneNumber: String?
)

data class AdminOwnerGroupBuyRequestProductResponse(
    val productName: String,
    val productDescription: String,
    val originalPrice: Int?,
    val price: Int,
    val discountRate: Int?,
    val targetQuantity: Int,
    val maxQuantity: Int,
    val perUserLimit: Int?
)

data class AdminOwnerGroupBuyRequestRecruitmentResponse(
    val recruitmentStartAt: LocalDateTime?,
    val deadline: LocalDateTime,
    val pickupDate: LocalDate,
    val pickupTimeStart: LocalTime,
    val pickupTimeEnd: LocalTime,
    val pickupLocation: String,
    val pickupContact: String?
)

data class AdminOwnerGroupBuyRequestImageResponse(
    val imageUrl: String,
    val sortOrder: Int
)

data class AdminOwnerGroupBuyRequestRejectRequest(
    @field:NotBlank(message = "반려 사유는 필수입니다")
    @field:Size(max = 200, message = "반려 사유는 200자 이하이어야 합니다")
    val rejectionReason: String
)

data class AdminOwnerGroupBuyRequestActionResponse(
    val requestId: Long,
    val status: OwnerGroupBuyRequestStatus,
    val groupBuyId: Long? = null
)

private const val REQUEST_TYPE = "OWNER"

private fun OwnerGroupBuyRequest.discountRate(): Int? {
    val original = originalPrice ?: return null
    if (original <= 0 || original < price) {
        return null
    }
    return ((original - price) * 100) / original
}
