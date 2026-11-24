package com.moongchijang.domain.store.domain.repository

import com.moongchijang.domain.store.domain.entity.StoreBusinessHour
import org.springframework.data.jpa.repository.JpaRepository

interface StoreBusinessHourRepository : JpaRepository<StoreBusinessHour, Long> {
    fun findAllByStoreIdOrderByDayOfWeekAsc(storeId: Long): List<StoreBusinessHour>

    fun deleteAllByStoreId(storeId: Long)
}
