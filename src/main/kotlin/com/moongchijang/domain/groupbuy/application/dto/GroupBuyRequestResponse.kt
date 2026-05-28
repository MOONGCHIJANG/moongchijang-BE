package com.moongchijang.domain.groupbuy.application.dto

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatusHistory
import java.time.LocalDate
import java.time.LocalDateTime

data class GroupBuyRequestIdResponse(
    val requestId: Long
)

data class GroupBuyRequestResponse(
    val requestId: Long,
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
    val status: String,
    val rejectionReason: String?,
    val openedGroupBuyId: Long?,
    val statusHistory: List<StatusHistoryItem>,
    val createdAt: LocalDateTime?
) {
    data class StatusHistoryItem(
        val status: String,
        val changedAt: LocalDateTime
    )

    companion object {
        fun from(
            request: GroupBuyRequest,
            history: List<GroupBuyRequestStatusHistory>
        ) = GroupBuyRequestResponse(
            requestId = request.id,
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
            status = request.status.name,
            rejectionReason = request.rejectionReason,
            openedGroupBuyId = request.openedGroupBuyId,
            statusHistory = history.map { StatusHistoryItem(it.status.name, it.changedAt) },
            createdAt = request.createdAt
        )
    }
}
