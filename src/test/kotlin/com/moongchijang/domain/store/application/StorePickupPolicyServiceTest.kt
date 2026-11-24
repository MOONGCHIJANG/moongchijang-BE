package com.moongchijang.domain.store.application

import com.moongchijang.domain.store.application.dto.StoreBusinessHourRequest
import com.moongchijang.domain.store.application.dto.StoreHolidayRequest
import com.moongchijang.domain.store.application.dto.StorePickupDateUnavailableReason
import com.moongchijang.domain.store.application.dto.StorePickupPolicyUpdateRequest
import com.moongchijang.domain.store.application.dto.StorePickupTimeSlotRequest
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Optional

class StorePickupPolicyServiceTest {

    private val storeRepository: StoreRepository = Mockito.mock(StoreRepository::class.java)
    private val storeBusinessHourRepository: StoreBusinessHourRepository = Mockito.mock(StoreBusinessHourRepository::class.java)
    private val storeHolidayRepository: StoreHolidayRepository = Mockito.mock(StoreHolidayRepository::class.java)
    private val storePickupTimeSlotRepository: StorePickupTimeSlotRepository = Mockito.mock(StorePickupTimeSlotRepository::class.java)
    private val storeStaffRepository: StoreStaffRepository = Mockito.mock(StoreStaffRepository::class.java)
    private val clock: Clock = Clock.fixed(Instant.parse("2026-06-01T01:00:00Z"), ZoneOffset.UTC)
    private val service = StorePickupPolicyService(
        storeRepository,
        storeBusinessHourRepository,
        storeHolidayRepository,
        storePickupTimeSlotRepository,
        storeStaffRepository,
        clock,
    )

    @Test
    fun `availability returns preparation, regular holiday, temporary holiday and selectable pickup dates`() {
        val store = store(preparationDays = 2, maxLeadDays = 14)
        Mockito.`when`(storeRepository.findById(store.id)).thenReturn(Optional.of(store))
        Mockito.`when`(storeBusinessHourRepository.findAllByStoreIdOrderByDayOfWeekAsc(store.id))
            .thenReturn(listOf(StoreBusinessHour(store = store, dayOfWeek = 4, isClosed = true)))
        Mockito.`when`(storeHolidayRepository.findAllByStoreIdAndActiveTrue(store.id))
            .thenReturn(
                listOf(
                    StoreHoliday(
                        store = store,
                        holidayType = StoreHolidayType.TEMPORARY,
                        holidayDate = LocalDate.of(2026, 6, 5),
                        source = StoreHolidaySource.ADMIN,
                    ),
                ),
            )
        Mockito.`when`(storePickupTimeSlotRepository.findAllByStoreIdOrderByDayOfWeekAscStartTimeAsc(store.id))
            .thenReturn(
                listOf(
                    StorePickupTimeSlot(
                        store = store,
                        dayOfWeek = 3,
                        startTime = LocalTime.of(14, 0),
                        endTime = LocalTime.of(15, 0),
                    ),
                ),
            )

        val result = service.getAvailability(
            store.id,
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 5),
        )

        val dates = result.dates.associateBy { it.date }
        assertThat(dates.getValue(LocalDate.of(2026, 6, 1)).unavailableReason)
            .isEqualTo(StorePickupDateUnavailableReason.PREPARATION_PERIOD)
        val selectableDate = dates.getValue(LocalDate.of(2026, 6, 3))
        assertThat(selectableDate.selectable).isTrue()
        assertThat(selectableDate.paymentDeadline).isEqualTo(LocalDateTime.of(2026, 6, 1, 23, 59))
        assertThat(selectableDate.sameDayPaymentWarning).isTrue()
        assertThat(selectableDate.slots.single().selectable).isTrue()
        assertThat(dates.getValue(LocalDate.of(2026, 6, 4)).unavailableReason)
            .isEqualTo(StorePickupDateUnavailableReason.REGULAR_HOLIDAY)
        assertThat(dates.getValue(LocalDate.of(2026, 6, 5)).unavailableReason)
            .isEqualTo(StorePickupDateUnavailableReason.TEMPORARY_HOLIDAY)
    }

    @Test
    fun `seller who is not assigned to the store cannot read its pickup policy`() {
        val store = store()
        Mockito.`when`(storeRepository.findById(store.id)).thenReturn(Optional.of(store))
        Mockito.`when`(storeStaffRepository.existsByUserIdAndStoreId(7L, store.id)).thenReturn(false)

        val exception = assertThrows<CustomException> {
            service.getPolicy(store.id, 7L, UserRole.SELLER)
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.FORBIDDEN)
    }

    @Test
    fun `seller update only replaces owner managed holidays and keeps external holiday source intact`() {
        val store = store()
        Mockito.`when`(storeRepository.findById(store.id)).thenReturn(Optional.of(store))
        Mockito.`when`(storeStaffRepository.existsByUserIdAndStoreId(7L, store.id)).thenReturn(true)
        Mockito.`when`(storeBusinessHourRepository.findAllByStoreIdOrderByDayOfWeekAsc(store.id)).thenReturn(emptyList())
        Mockito.`when`(storeHolidayRepository.findAllByStoreIdAndActiveTrue(store.id)).thenReturn(emptyList())
        Mockito.`when`(storePickupTimeSlotRepository.findAllByStoreIdOrderByDayOfWeekAscStartTimeAsc(store.id))
            .thenReturn(emptyList())

        service.updatePolicy(store.id, 7L, UserRole.SELLER, validUpdateRequest())

        assertThat(store.preparationDays).isEqualTo(2)
        assertThat(store.maxLeadDays).isEqualTo(10)
        Mockito.verify(storeHolidayRepository).deleteAllByStoreIdAndSourceIn(
            store.id,
            listOf(StoreHolidaySource.ADMIN, StoreHolidaySource.OWNER),
        )
        Mockito.verify(storeBusinessHourRepository).saveAll(Mockito.anyList())
        Mockito.verify(storeHolidayRepository).saveAll(Mockito.anyList())
        Mockito.verify(storePickupTimeSlotRepository).saveAll(Mockito.anyList())
    }

    @Test
    fun `seller cannot submit an admin or external holiday source`() {
        val store = store()
        Mockito.`when`(storeRepository.findById(store.id)).thenReturn(Optional.of(store))
        Mockito.`when`(storeStaffRepository.existsByUserIdAndStoreId(7L, store.id)).thenReturn(true)

        val request = validUpdateRequest(
            holidays = listOf(
                StoreHolidayRequest(
                    date = LocalDate.of(2026, 6, 10),
                    source = StoreHolidaySource.ADMIN,
                ),
            ),
        )

        val exception = assertThrows<CustomException> {
            service.updatePolicy(store.id, 7L, UserRole.SELLER, request)
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.INVALID_INPUT)
    }

    private fun validUpdateRequest(
        holidays: List<StoreHolidayRequest> = listOf(
            StoreHolidayRequest(
                date = LocalDate.of(2026, 6, 10),
                reason = "임시 휴무",
                source = StoreHolidaySource.OWNER,
            ),
        ),
    ) = StorePickupPolicyUpdateRequest(
        preparationDays = 2,
        maxAdvanceDays = 10,
        businessHours = (1..7).map { dayOfWeek ->
            StoreBusinessHourRequest(
                dayOfWeek = dayOfWeek,
                isClosed = false,
                openTime = LocalTime.of(9, 0),
                closeTime = LocalTime.of(18, 0),
            )
        },
        holidays = holidays,
        timeSlots = listOf(
            StorePickupTimeSlotRequest(
                dayOfWeek = 1,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(10, 0),
                active = true,
            ),
        ),
    )

    private fun store(
        preparationDays: Int = 3,
        maxLeadDays: Int = 14,
    ) = Store(
        name = "테스트 베이커리",
        address = "서울 성동구",
        region = RegionType.SEOUL,
        district = DistrictType.SEOUL_ALL,
        preparationDays = preparationDays,
        maxLeadDays = maxLeadDays,
    ).also { it.id = 10L }
}
