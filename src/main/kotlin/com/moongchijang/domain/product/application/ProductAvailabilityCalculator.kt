package com.moongchijang.domain.product.application

import com.moongchijang.domain.product.application.dto.PickupDateResponse
import com.moongchijang.domain.product.application.dto.PickupTimeSlotResponse
import com.moongchijang.domain.product.application.dto.ProductPickupResponse
import com.moongchijang.domain.product.application.dto.ProductUnavailableReason
import com.moongchijang.domain.product.application.dto.StoreHolidayResponse
import com.moongchijang.domain.product.domain.entity.Product
import com.moongchijang.domain.product.domain.entity.ProductStatus
import com.moongchijang.domain.store.domain.entity.StoreHoliday
import com.moongchijang.domain.store.domain.entity.StoreHolidayType
import com.moongchijang.domain.store.domain.entity.StorePickupTimeSlot
import com.moongchijang.domain.store.domain.entity.StoreSalesStatus
import java.time.LocalDate

object ProductAvailabilityCalculator {

    fun calculate(
        product: Product,
        holidays: List<StoreHoliday>,
        timeSlots: List<StorePickupTimeSlot>,
        today: LocalDate,
    ): ProductAvailabilityResult {
        val availableFrom = today.plusDays(product.preparationDays.toLong())
        val availableUntil = today.plusDays(MAX_PICKUP_DAYS)
        val regularHolidayDays = holidays
            .filter { it.holidayType == StoreHolidayType.REGULAR }
            .mapNotNull { it.dayOfWeek }
            .toSet()
        val temporaryHolidayDates = holidays
            .filter { it.holidayType == StoreHolidayType.TEMPORARY }
            .mapNotNull { it.holidayDate }
            .toSet()
        val timeSlotsByDay = timeSlots.groupBy { it.dayOfWeek }

        val availableDates = if (availableFrom.isAfter(availableUntil)) {
            emptyList()
        } else {
            availableFrom.datesUntil(availableUntil.plusDays(1))
                .filter { date ->
                    date.dayOfWeek.value !in regularHolidayDays &&
                        date !in temporaryHolidayDates &&
                        timeSlotsByDay[date.dayOfWeek.value].orEmpty().isNotEmpty()
                }
                .map { date ->
                    PickupDateResponse(
                        date = date,
                        timeSlots = timeSlotsByDay[date.dayOfWeek.value].orEmpty().map { slot ->
                            PickupTimeSlotResponse(
                                startTime = slot.startTime,
                                endTime = slot.endTime,
                            )
                        },
                    )
                }
                .toList()
        }

        val unavailableReason = when {
            product.status != ProductStatus.ACTIVE -> ProductUnavailableReason.PRODUCT_NOT_FOR_SALE
            product.store.salesStatus != StoreSalesStatus.ACTIVE -> ProductUnavailableReason.STORE_NOT_FOR_SALE
            availableDates.isEmpty() -> ProductUnavailableReason.NO_AVAILABLE_PICKUP_DATE
            else -> null
        }

        return ProductAvailabilityResult(
            pickup = ProductPickupResponse(
                preparationDays = product.preparationDays,
                availableFrom = availableFrom,
                availableUntil = availableUntil,
                availableDates = availableDates,
                holidays = holidays.map {
                    StoreHolidayResponse(
                        type = it.holidayType,
                        dayOfWeek = it.dayOfWeek,
                        date = it.holidayDate,
                        reason = it.reason,
                    )
                },
            ),
            canPurchase = unavailableReason == null,
            unavailableReason = unavailableReason,
        )
    }

    private const val MAX_PICKUP_DAYS = 14L
}

data class ProductAvailabilityResult(
    val pickup: ProductPickupResponse,
    val canPurchase: Boolean,
    val unavailableReason: ProductUnavailableReason?,
)
