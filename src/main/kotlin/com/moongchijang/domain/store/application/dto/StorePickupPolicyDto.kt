package com.moongchijang.domain.store.application.dto

import com.moongchijang.domain.store.domain.entity.StoreHolidaySource
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class StorePickupAvailabilityResponse(
    val storeId: Long,
    val preparationDays: Int,
    val maxAdvanceDays: Int,
    val dates: List<StorePickupDateAvailabilityResponse>,
)
data class StorePickupDateAvailabilityResponse(
    val date: LocalDate,
    val selectable: Boolean,
    val unavailableReason: StorePickupDateUnavailableReason?,
    val paymentDeadline: LocalDateTime?,
    val sameDayPaymentWarning: Boolean,
    val slots: List<StorePickupSlotAvailabilityResponse>,
)
data class StorePickupSlotAvailabilityResponse(
    val startTime: LocalTime,
    val endTime: LocalTime,
    val selectable: Boolean,
    val unavailableReason: StorePickupSlotUnavailableReason?,
    val remainingQuantity: Int?,
)

enum class StorePickupDateUnavailableReason {
    PAST_DATE,
    PREPARATION_PERIOD,
    MAX_LEAD_DAYS,
    REGULAR_HOLIDAY,
    TEMPORARY_HOLIDAY,
    NO_ACTIVE_SLOT,
    FULL,
}

enum class StorePickupSlotUnavailableReason {
    STORE_CLOSED,
    FULL,
    INACTIVE,
}

data class StorePickupPolicyResponse(
    val storeId: Long,
    val preparationDays: Int,
    val maxAdvanceDays: Int,
    val businessHours: List<StoreBusinessHourResponse>,
    val holidays: List<StoreHolidayResponse>,
    val timeSlots: List<StorePickupTimeSlotResponse>,
)

data class StoreBusinessHourResponse(
    val dayOfWeek: Int,
    val isClosed: Boolean,
    val openTime: LocalTime?,
    val closeTime: LocalTime?,
)

data class StoreHolidayResponse(
    val date: LocalDate,
    val reason: String?,
    val source: StoreHolidaySource,
)

data class StorePickupTimeSlotResponse(
    val dayOfWeek: Int,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val active: Boolean,
    val slotCapacity: Int?,
)

data class StorePickupPolicyUpdateRequest(
    @field:Min(0)
    val preparationDays: Int,
    @field:Min(1)
    val maxAdvanceDays: Int,
    @field:Valid
    @field:Size(min = 7, max = 7)
    val businessHours: List<StoreBusinessHourRequest>,
    @field:Valid
    val holidays: List<StoreHolidayRequest>,
    @field:Valid
    val timeSlots: List<StorePickupTimeSlotRequest>,
)

data class StoreBusinessHourRequest(
    @field:Min(1)
    @field:Max(7)
    val dayOfWeek: Int,
    val isClosed: Boolean,
    val openTime: LocalTime?,
    val closeTime: LocalTime?,
)

data class StoreHolidayRequest(
    val date: LocalDate,
    @field:Size(max = 100)
    val reason: String? = null,
    val source: StoreHolidaySource,
)

data class StorePickupTimeSlotRequest(
    @field:Min(1)
    @field:Max(7)
    val dayOfWeek: Int,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val active: Boolean,
    @field:Min(1)
    val slotCapacity: Int? = null,
)
