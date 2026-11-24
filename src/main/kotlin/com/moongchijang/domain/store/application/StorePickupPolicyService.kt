package com.moongchijang.domain.store.application

import com.moongchijang.domain.store.application.dto.StoreBusinessHourRequest
import com.moongchijang.domain.store.application.dto.StoreBusinessHourResponse
import com.moongchijang.domain.store.application.dto.StoreHolidayResponse
import com.moongchijang.domain.store.application.dto.StoreHolidayRequest
import com.moongchijang.domain.store.application.dto.StorePickupAvailabilityResponse
import com.moongchijang.domain.store.application.dto.StorePickupDateAvailabilityResponse
import com.moongchijang.domain.store.application.dto.StorePickupDateUnavailableReason
import com.moongchijang.domain.store.application.dto.StorePickupPolicyResponse
import com.moongchijang.domain.store.application.dto.StorePickupPolicyUpdateRequest
import com.moongchijang.domain.store.application.dto.StorePickupSlotAvailabilityResponse
import com.moongchijang.domain.store.application.dto.StorePickupSlotUnavailableReason
import com.moongchijang.domain.store.application.dto.StorePickupTimeSlotRequest
import com.moongchijang.domain.store.application.dto.StorePickupTimeSlotResponse
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.domain.store.domain.entity.StoreBusinessHour
import com.moongchijang.domain.store.domain.entity.StoreHoliday
import com.moongchijang.domain.store.domain.entity.StoreHolidaySource
import com.moongchijang.domain.store.domain.entity.StoreHolidayType
import com.moongchijang.domain.store.domain.entity.StorePickupTimeSlot
import com.moongchijang.domain.store.domain.repository.StoreBusinessHourRepository
import com.moongchijang.domain.store.domain.repository.StoreHolidayRepository
import com.moongchijang.domain.store.domain.repository.StorePickupTimeSlotRepository
import com.moongchijang.domain.store.domain.repository.StoreRepository
import com.moongchijang.domain.store.domain.repository.StoreStaffRepository
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.time.kstToday
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class StorePickupPolicyService(
    private val storeRepository: StoreRepository,
    private val storeBusinessHourRepository: StoreBusinessHourRepository,
    private val storeHolidayRepository: StoreHolidayRepository,
    private val storePickupTimeSlotRepository: StorePickupTimeSlotRepository,
    private val storeStaffRepository: StoreStaffRepository,
    private val clock: Clock,
) {

    fun getAvailability(
        storeId: Long,
        from: LocalDate?,
        to: LocalDate?,
    ): StorePickupAvailabilityResponse {
        val store = findStore(storeId)
        val today = clock.kstToday()
        val startDate = from ?: today
        val endDate = to ?: today.plusDays(store.maxLeadDays.toLong())
        validateDateRange(startDate, endDate)

        val businessHours = storeBusinessHourRepository.findAllByStoreIdOrderByDayOfWeekAsc(storeId)
        val holidays = storeHolidayRepository.findAllByStoreIdAndActiveTrue(storeId)
        val timeSlots = storePickupTimeSlotRepository.findAllByStoreIdOrderByDayOfWeekAscStartTimeAsc(storeId)
        val closedDays = businessHours.filter { it.isClosed }.map { it.dayOfWeek }.toSet() +
            holidays.filter { it.holidayType == StoreHolidayType.REGULAR }.mapNotNull { it.dayOfWeek }
        val temporaryHolidayDates = holidays
            .filter { it.holidayType == StoreHolidayType.TEMPORARY }
            .mapNotNull { it.holidayDate }
            .toSet()
        val slotsByDay = timeSlots.groupBy { it.dayOfWeek }

        return StorePickupAvailabilityResponse(
            storeId = store.id,
            preparationDays = store.preparationDays,
            maxAdvanceDays = store.maxLeadDays,
            dates = datesBetween(startDate, endDate).map { date ->
                buildDateAvailability(
                    date = date,
                    today = today,
                    store = store,
                    closedDays = closedDays,
                    temporaryHolidayDates = temporaryHolidayDates,
                    slots = slotsByDay[date.dayOfWeek.value].orEmpty(),
                )
            },
        )
    }

    fun getPolicy(storeId: Long, userId: Long, role: UserRole): StorePickupPolicyResponse {
        val store = findStore(storeId)
        validatePolicyAccess(store.id, userId, role)
        return buildPolicy(store)
    }

    @Transactional
    fun updatePolicy(
        storeId: Long,
        userId: Long,
        role: UserRole,
        request: StorePickupPolicyUpdateRequest,
    ): StorePickupPolicyResponse {
        val store = findStore(storeId)
        validatePolicyAccess(store.id, userId, role)
        val source = policyHolidaySource(role)
        validatePolicy(request, source)

        store.preparationDays = request.preparationDays
        store.maxLeadDays = request.maxAdvanceDays

        storeBusinessHourRepository.deleteAllByStoreId(store.id)
        storeHolidayRepository.deleteAllByStoreIdAndSourceIn(
            store.id,
            listOf(StoreHolidaySource.ADMIN, StoreHolidaySource.OWNER),
        )
        storePickupTimeSlotRepository.deleteAllByStoreId(store.id)

        storeBusinessHourRepository.saveAll(
            request.businessHours.map { hour ->
                StoreBusinessHour(
                    store = store,
                    dayOfWeek = hour.dayOfWeek,
                    openTime = hour.openTime,
                    closeTime = hour.closeTime,
                    isClosed = hour.isClosed,
                )
            },
        )

        val regularHolidays = request.businessHours
            .filter { it.isClosed }
            .map { hour ->
                StoreHoliday(
                    store = store,
                    holidayType = StoreHolidayType.REGULAR,
                    dayOfWeek = hour.dayOfWeek,
                    source = source,
                )
            }
        val temporaryHolidays = request.holidays.map { holiday ->
            StoreHoliday(
                store = store,
                holidayType = StoreHolidayType.TEMPORARY,
                holidayDate = holiday.date,
                reason = holiday.reason,
                source = source,
            )
        }
        storeHolidayRepository.saveAll(regularHolidays + temporaryHolidays)

        storePickupTimeSlotRepository.saveAll(
            request.timeSlots.map { slot ->
                StorePickupTimeSlot(
                    store = store,
                    dayOfWeek = slot.dayOfWeek,
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    slotCapacity = slot.slotCapacity,
                    active = slot.active,
                )
            },
        )

        return buildPolicy(store)
    }

    private fun buildDateAvailability(
        date: LocalDate,
        today: LocalDate,
        store: Store,
        closedDays: Set<Int>,
        temporaryHolidayDates: Set<LocalDate>,
        slots: List<StorePickupTimeSlot>,
    ): StorePickupDateAvailabilityResponse {
        val unavailableReason = when {
            date.isBefore(today) -> StorePickupDateUnavailableReason.PAST_DATE
            date.isBefore(today.plusDays(store.preparationDays.toLong())) -> StorePickupDateUnavailableReason.PREPARATION_PERIOD
            date.isAfter(today.plusDays(store.maxLeadDays.toLong())) -> StorePickupDateUnavailableReason.MAX_LEAD_DAYS
            date.dayOfWeek.value in closedDays -> StorePickupDateUnavailableReason.REGULAR_HOLIDAY
            date in temporaryHolidayDates -> StorePickupDateUnavailableReason.TEMPORARY_HOLIDAY
            else -> null
        }

        if (unavailableReason != null) {
            val closedSlots = if (unavailableReason in setOf(
                    StorePickupDateUnavailableReason.REGULAR_HOLIDAY,
                    StorePickupDateUnavailableReason.TEMPORARY_HOLIDAY,
                )
            ) {
                slots.map { it.toUnavailableSlot(StorePickupSlotUnavailableReason.STORE_CLOSED) }
            } else {
                emptyList()
            }
            return StorePickupDateAvailabilityResponse(
                date = date,
                selectable = false,
                unavailableReason = unavailableReason,
                paymentDeadline = null,
                sameDayPaymentWarning = false,
                slots = closedSlots,
            )
        }

        val slotResponses = slots.map { slot ->
            val slotCapacity = slot.slotCapacity
            when {
                !slot.active -> slot.toUnavailableSlot(StorePickupSlotUnavailableReason.INACTIVE)
                slotCapacity != null && slotCapacity <= 0 -> slot.toUnavailableSlot(StorePickupSlotUnavailableReason.FULL)
                else -> StorePickupSlotAvailabilityResponse(
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    selectable = true,
                    unavailableReason = null,
                    remainingQuantity = slotCapacity,
                )
            }
        }
        val selectable = slotResponses.any { it.selectable }
        val dateReason = if (selectable) {
            null
        } else if (slotResponses.any { it.unavailableReason == StorePickupSlotUnavailableReason.FULL }) {
            StorePickupDateUnavailableReason.FULL
        } else {
            StorePickupDateUnavailableReason.NO_ACTIVE_SLOT
        }
        val paymentDeadline = if (selectable) paymentDeadline(date, store.preparationDays) else null

        return StorePickupDateAvailabilityResponse(
            date = date,
            selectable = selectable,
            unavailableReason = dateReason,
            paymentDeadline = paymentDeadline,
            sameDayPaymentWarning = paymentDeadline?.toLocalDate() == today,
            slots = slotResponses,
        )
    }

    private fun StorePickupTimeSlot.toUnavailableSlot(
        reason: StorePickupSlotUnavailableReason,
    ) = StorePickupSlotAvailabilityResponse(
        startTime = startTime,
        endTime = endTime,
        selectable = false,
        unavailableReason = reason,
        remainingQuantity = slotCapacity?.coerceAtLeast(0),
    )

    private fun buildPolicy(store: Store): StorePickupPolicyResponse {
        val hours = storeBusinessHourRepository.findAllByStoreIdOrderByDayOfWeekAsc(store.id)
        val holidays = storeHolidayRepository.findAllByStoreIdAndActiveTrue(store.id)
        val timeSlots = storePickupTimeSlotRepository.findAllByStoreIdOrderByDayOfWeekAscStartTimeAsc(store.id)
        val legacyClosedDays = holidays
            .filter { it.holidayType == StoreHolidayType.REGULAR }
            .mapNotNull { it.dayOfWeek }
            .toSet()
        val hoursByDay = hours.associateBy { it.dayOfWeek }

        return StorePickupPolicyResponse(
            storeId = store.id,
            preparationDays = store.preparationDays,
            maxAdvanceDays = store.maxLeadDays,
            businessHours = (1..7).map { dayOfWeek ->
                hoursByDay[dayOfWeek]?.toResponse()
                    ?: StoreBusinessHourResponse(
                        dayOfWeek = dayOfWeek,
                        isClosed = dayOfWeek in legacyClosedDays,
                        openTime = null,
                        closeTime = null,
                    )
            },
            holidays = holidays
                .filter { it.holidayType == StoreHolidayType.TEMPORARY }
                .mapNotNull { holiday ->
                    holiday.holidayDate?.let { date ->
                        StoreHolidayResponse(
                            date = date,
                            reason = holiday.reason,
                            source = holiday.source,
                        )
                    }
                },
            timeSlots = timeSlots.map { slot ->
                StorePickupTimeSlotResponse(
                    dayOfWeek = slot.dayOfWeek,
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    active = slot.active,
                    slotCapacity = slot.slotCapacity,
                )
            },
        )
    }

    private fun StoreBusinessHour.toResponse() = StoreBusinessHourResponse(
        dayOfWeek = dayOfWeek,
        isClosed = isClosed,
        openTime = openTime,
        closeTime = closeTime,
    )

    private fun validatePolicyAccess(storeId: Long, userId: Long, role: UserRole) {
        if (role == UserRole.ADMIN) return
        if (role != UserRole.SELLER || !storeStaffRepository.existsByUserIdAndStoreId(userId, storeId)) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }
    }

    private fun validatePolicy(
        request: StorePickupPolicyUpdateRequest,
        expectedHolidaySource: StoreHolidaySource,
    ) {
        if (request.preparationDays > request.maxAdvanceDays) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }
        val hoursByDay = request.businessHours.associateBy { it.dayOfWeek }
        if (hoursByDay.size != 7 || (1..7).any { it !in hoursByDay }) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }
        request.businessHours.forEach { hour -> validateBusinessHour(hour) }

        if (request.holidays.map { it.date }.distinct().size != request.holidays.size ||
            request.holidays.any { it.source != expectedHolidaySource }
        ) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }

        val slotsByDay = request.timeSlots.groupBy { it.dayOfWeek }
        if (request.timeSlots.map { Triple(it.dayOfWeek, it.startTime, it.endTime) }.distinct().size != request.timeSlots.size) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }
        request.timeSlots.forEach { slot ->
            val hour = hoursByDay.getValue(slot.dayOfWeek)
            validateTimeSlot(slot, hour)
        }
        slotsByDay.values.forEach { slots ->
            val sorted = slots.sortedBy { it.startTime }
            if (sorted.zipWithNext().any { (left, right) -> left.endTime > right.startTime }) {
                throw CustomException(ErrorCode.INVALID_INPUT)
            }
        }
    }

    private fun validateBusinessHour(hour: StoreBusinessHourRequest) {
        if (hour.isClosed) {
            if (hour.openTime != null || hour.closeTime != null) throw CustomException(ErrorCode.INVALID_INPUT)
            return
        }
        if (hour.openTime == null || hour.closeTime == null || !hour.openTime.isBefore(hour.closeTime)) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }
    }

    private fun validateTimeSlot(slot: StorePickupTimeSlotRequest, hour: StoreBusinessHourRequest) {
        if (hour.isClosed || !slot.startTime.isBefore(slot.endTime) ||
            Duration.between(slot.startTime, slot.endTime) != Duration.ofHours(1) ||
            hour.openTime == null || hour.closeTime == null ||
            slot.startTime < hour.openTime || slot.endTime > hour.closeTime
        ) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }
    }

    private fun policyHolidaySource(role: UserRole): StoreHolidaySource =
        if (role == UserRole.ADMIN) StoreHolidaySource.ADMIN else StoreHolidaySource.OWNER

    private fun findStore(storeId: Long): Store = storeRepository.findById(storeId)
        .orElseThrow { CustomException(ErrorCode.STORE_NOT_FOUND) }

    private fun validateDateRange(from: LocalDate, to: LocalDate) {
        if (to.isBefore(from)) throw CustomException(ErrorCode.INVALID_INPUT)
    }

    private fun paymentDeadline(pickupDate: LocalDate, preparationDays: Int): LocalDateTime =
        LocalDateTime.of(pickupDate.minusDays(preparationDays.toLong()), LocalTime.of(23, 59))

    private fun datesBetween(from: LocalDate, to: LocalDate): List<LocalDate> = buildList {
        var cursor = from
        while (!cursor.isAfter(to)) {
            add(cursor)
            cursor = cursor.plusDays(1)
        }
    }
}
