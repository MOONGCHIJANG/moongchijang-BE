package com.moongchijang.domain.groupbuy.application.dto

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatusHistory
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page
import java.time.Duration
import java.time.LocalDateTime

@Schema(
    description = "어드민 입점 요청 상태 필터"
)
enum class AdminGroupBuyRequestStatusFilter {
    @Schema(description = "전체")
    ALL,

    @Schema(description = "요청 접수")
    REQUEST_RECEIVED,

    @Schema(description = "검토 중")
    IN_REVIEW,

    @Schema(description = "매장 확인 중")
    STORE_CONFIRMING,

    @Schema(description = "입점 완료")
    COMPLETED,

    @Schema(description = "입점 불가")
    UNAVAILABLE;

    fun toStatuses(): List<GroupBuyRequestStatus>? =
        when (this) {
            ALL -> null
            REQUEST_RECEIVED -> listOf(GroupBuyRequestStatus.REQUEST_RECEIVED)
            IN_REVIEW -> listOf(GroupBuyRequestStatus.IN_REVIEW)
            STORE_CONFIRMING -> listOf(GroupBuyRequestStatus.STORE_CONFIRMING)
            COMPLETED -> listOf(GroupBuyRequestStatus.COMPLETED)
            UNAVAILABLE -> listOf(GroupBuyRequestStatus.UNAVAILABLE)
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
                status = request.status,
                requesterId = requesterId(request),
                requesterName = requester?.nickname,
                originalPrice = groupBuy?.originalPrice,
                price = groupBuy?.price,
                reviewElapsedMinutes = request.createdAt?.let { Duration.between(it, now).toMinutes().coerceAtLeast(0) },
                actionable = request.status == GroupBuyRequestStatus.STORE_CONFIRMING,
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
