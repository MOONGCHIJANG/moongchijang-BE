package com.moongchijang.domain.pickup.application.dto

import com.moongchijang.domain.participation.domain.entity.PickupStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class PickupAvailabilityStatus {
    LOCKED,
    AVAILABLE,
    PICKED_UP
}

data class PickupGuideResponse(
    val participationId: Long,
    val availabilityStatus: PickupAvailabilityStatus,
    val pickupStatus: PickupStatus,
    val storeName: String,
    val storeAddress: String,
    val storePhone: String?,
    val latitude: Double?,
    val longitude: Double?,
    val transitInfo: String?,
    val thumbnailUrl: String?,
    val productName: String,
    val quantity: Int,
    val pickupDate: LocalDate,
    val pickupTimeStart: LocalTime,
    val pickupTimeEnd: LocalTime,
    val pickupLocation: String,
    val remainingMinutes: Long?,
    val pickedUpAt: LocalDateTime?,
)

data class PickupQrResponse(
    val participationId: Long,
    val reservationNumber: String,
    val availabilityStatus: PickupAvailabilityStatus,
    val pickupStatus: PickupStatus,
    val userName: String?,
    val productName: String,
    val quantity: Int,
    val storeName: String,
    val storeAddress: String,
    val pickupLocation: String,
    val qrCode: String?,
    val pickupDate: LocalDate,
    val pickupTimeStart: LocalTime,
    val pickupTimeEnd: LocalTime,
    val dDay: Int,
    val pickedUpAt: LocalDateTime?,
)

enum class NearestPickupQrReason {
    NO_AVAILABLE_PICKUP,
    ONLY_FUTURE_PICKUP
}

data class NearestPickupQrResponse(
    val hasCandidate: Boolean,
    val hasMultipleToday: Boolean,
    val reason: NearestPickupQrReason?,
    val item: PickupQrResponse?,
)

data class PickupVerifyResponse(
    val participationId: Long,
    val pickupStatus: PickupStatus,
    val pickedUpAt: LocalDateTime,
    val pickupProcessedByUserId: Long?,
)
