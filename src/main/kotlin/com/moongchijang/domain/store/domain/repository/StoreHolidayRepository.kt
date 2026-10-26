package com.moongchijang.domain.store.domain.repository

import com.moongchijang.domain.store.domain.entity.StoreHoliday
import org.springframework.data.jpa.repository.JpaRepository

interface StoreHolidayRepository : JpaRepository<StoreHoliday, Long> {
    fun findAllByStoreIdAndActiveTrue(storeId: Long): List<StoreHoliday>
}
