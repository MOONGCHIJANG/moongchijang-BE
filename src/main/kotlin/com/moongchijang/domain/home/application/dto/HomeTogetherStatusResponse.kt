package com.moongchijang.domain.home.application.dto

import com.moongchijang.domain.together.domain.entity.TogetherRoomMemberRole
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class HomeTogetherStatusResponse(
    val hasActiveRooms: Boolean,
    val showTogetherRoomModule: Boolean,
    val togetherTabBadgeCount: Int,
    val isTodayPickupLeader: Boolean,
    val todayPickups: List<HomeTodayPickupResponse>,
    val rooms: List<HomeTogetherRoomSummaryResponse>,
)

data class HomeTodayPickupResponse(
    val roomId: Long,
    val role: TogetherRoomMemberRole,
    val productName: String,
    val quantity: Int,
    val storeName: String,
    val pickupDate: LocalDate,
    val pickupTimeStart: LocalTime,
    val pickupTimeEnd: LocalTime,
    val canOpenQr: Boolean,
)

data class HomeTogetherRoomSummaryResponse(
    val roomId: Long,
    val role: TogetherRoomMemberRole,
    val productId: Long,
    val productName: String,
    val storeName: String,
    val currentQuantity: Int,
    val minQuantity: Int,
    val maxQuantity: Int,
    val paymentDeadlineAt: LocalDateTime,
    val nextAction: HomeTogetherRoomAction,
)

enum class HomeTogetherRoomAction {
    NONE,
    PAY,
    TRANSFER,
    PICKUP,
}
