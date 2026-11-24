package com.moongchijang.domain.store.domain.repository

import com.moongchijang.domain.store.domain.entity.StoreHoliday
import com.moongchijang.domain.store.domain.entity.StoreHolidaySource
import org.springframework.data.jpa.repository.JpaRepository

interface StoreHolidayRepository : JpaRepository<StoreHoliday, Long> {
    fun findAllByStoreIdAndActiveTrue(storeId: Long): List<StoreHoliday>

    fun findAllByStoreIdInAndActiveTrue(storeIds: Collection<Long>): List<StoreHoliday>

    fun deleteAllByStoreIdAndSourceIn(storeId: Long, sources: Collection<StoreHolidaySource>)
}
