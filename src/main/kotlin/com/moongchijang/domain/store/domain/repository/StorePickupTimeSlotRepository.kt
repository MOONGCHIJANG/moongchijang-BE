package com.moongchijang.domain.store.domain.repository

import com.moongchijang.domain.store.domain.entity.StorePickupTimeSlot
import org.springframework.data.jpa.repository.JpaRepository

interface StorePickupTimeSlotRepository : JpaRepository<StorePickupTimeSlot, Long> {
    fun findAllByStoreIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(
        storeId: Long,
    ): List<StorePickupTimeSlot>

    fun findAllByStoreIdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(
        storeId: Long,
        dayOfWeek: Int,
    ): List<StorePickupTimeSlot>
}
