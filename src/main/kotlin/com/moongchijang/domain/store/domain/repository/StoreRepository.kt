package com.moongchijang.domain.store.domain.repository

import com.moongchijang.domain.store.domain.entity.Store
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface StoreRepository : JpaRepository<Store, Long> {
    @Query("SELECT s FROM Store s WHERE LOWER(s.name) IN :names")
    fun findByNormalizedNameIn(@Param("names") names: Collection<String>): List<Store>

    @Query("SELECT s FROM Store s WHERE LOWER(s.address) IN :addresses")
    fun findByNormalizedAddressIn(@Param("addresses") addresses: Collection<String>): List<Store>
}
