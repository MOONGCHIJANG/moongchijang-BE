package com.moongchijang.domain.groupbuy.application.dto

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatusHistory
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import org.springframework.data.domain.Page
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

enum class AdminGroupBuyRequestStatusFilter {
    ALL,
    IN_REVIEW,
    IN_CONTACT,
    OPENED,
    REJECTED;

    fun toStatus(): GroupBuyRequestStatus? =
        when (this) {
            ALL -> null
            IN_REVIEW -> GroupBuyRequestStatus.IN_REVIEW
            IN_CONTACT -> GroupBuyRequestStatus.IN_CONTACT
            OPENED -> GroupBuyRequestStatus.OPENED
            REJECTED -> GroupBuyRequestStatus.REJECTED
        }
}

data class AdminGroupBuyRequestPageResponse(
    val content: List<AdminGroupBuyRequestListItemResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int
) {
    companion object {
        fun from(
            page: Page<GroupBuyRequest>,
            groupBuysById: Map<Long, GroupBuy>,
            now: LocalDateTime
        ): AdminGroupBuyRequestPageResponse =
            AdminGroupBuyRequestPageResponse(
                content = page.content.map {
                    AdminGroupBuyRequestListItemResponse.from(
                        request = it,
                        requester = it.user,
                        groupBuy = it.openedGroupBuyId?.let(groupBuysById::get),
                        now = now
                    )
                },
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                number = page.number,
                size = page.size
            )
    }
}

data class AdminGroupBuyRequestListItemResponse(
    val requestId: Long,
    val storeName: String,
    val productName: String,
    val desiredQuantity: Int,
    val desiredPickupDate: LocalDate,
    val status: GroupBuyRequestStatus,
    val requesterId: Long,
    val requesterName: String?,
    val originalPrice: Int?,
    val price: Int?,
    val reviewElapsedMinutes: Long?,
    val actionable: Boolean,
    val createdAt: LocalDateTime?
) {
    companion object {
        fun from(
            request: GroupBuyRequest,
            requester: User?,
            groupBuy: GroupBuy?,
            now: LocalDateTime
        ): AdminGroupBuyRequestListItemResponse =
            AdminGroupBuyRequestListItemResponse(
                requestId = request.id,
                storeName = request.storeName,
                productName = request.productName,
                desiredQuantity = request.desiredQuantity,
                desiredPickupDate = request.desiredPickupDate,
                status = request.status,
                requesterId = requesterId(request),
                requesterName = requester?.nickname,
                originalPrice = groupBuy?.originalPrice,
                price = groupBuy?.price,
                reviewElapsedMinutes = request.createdAt?.let { Duration.between(it, now).toMinutes().coerceAtLeast(0) },
                actionable = request.status == GroupBuyRequestStatus.IN_REVIEW ||
                    request.status == GroupBuyRequestStatus.IN_CONTACT,
                createdAt = request.createdAt
            )
    }
}

data class AdminGroupBuyRequestDetailResponse(
    val requestId: Long,
    val requester: AdminGroupBuyRequestRequesterResponse,
    val storeName: String,
    val storeAddress: String?,
    val placeId: String?,
    val roadAddress: String?,
    val lotAddress: String?,
    val latitude: Double?,
    val longitude: Double?,
    val productName: String,
    val desiredQuantity: Int,
    val desiredPickupDate: LocalDate,
    val additionalNote: String?,
    val status: GroupBuyRequestStatus,
    val rejectionReason: String?,
    val openedGroupBuyId: Long?,
    val statusHistory: List<AdminGroupBuyRequestStatusHistoryResponse>,
    val createdAt: LocalDateTime?
) {
    companion object {
        fun from(
            request: GroupBuyRequest,
            requester: User?,
            history: List<GroupBuyRequestStatusHistory>
        ): AdminGroupBuyRequestDetailResponse =
            AdminGroupBuyRequestDetailResponse(
                requestId = request.id,
                requester = AdminGroupBuyRequestRequesterResponse.from(requesterId(request), requester),
                storeName = request.storeName,
                storeAddress = request.storeAddress,
                placeId = request.placeId,
                roadAddress = request.roadAddress,
                lotAddress = request.lotAddress,
                latitude = request.latitude,
                longitude = request.longitude,
                productName = request.productName,
                desiredQuantity = request.desiredQuantity,
                desiredPickupDate = request.desiredPickupDate,
                additionalNote = request.additionalNote,
                status = request.status,
                rejectionReason = request.rejectionReason,
                openedGroupBuyId = request.openedGroupBuyId,
                statusHistory = history.map { AdminGroupBuyRequestStatusHistoryResponse.from(it) },
                createdAt = request.createdAt
            )
    }
}

data class AdminGroupBuyRequestRequesterResponse(
    val userId: Long,
    val nickname: String?,
    val phoneNumber: String?,
    val email: String?,
    val provider: AuthProvider?
) {
    companion object {
        fun from(
            userId: Long,
            user: User?
        ): AdminGroupBuyRequestRequesterResponse =
            AdminGroupBuyRequestRequesterResponse(
                userId = userId,
                nickname = user?.nickname,
                phoneNumber = user?.phoneNumber,
                email = user?.email,
                provider = user?.provider
            )
    }
}

data class AdminGroupBuyRequestStatusHistoryResponse(
    val status: GroupBuyRequestStatus,
    val changedAt: LocalDateTime
) {
    companion object {
        fun from(history: GroupBuyRequestStatusHistory): AdminGroupBuyRequestStatusHistoryResponse =
            AdminGroupBuyRequestStatusHistoryResponse(
                status = history.status,
                changedAt = history.changedAt
            )
    }
}

private fun requesterId(request: GroupBuyRequest): Long =
    requireNotNull(request.user.id) { "GroupBuyRequest.user.id must not be null" }
