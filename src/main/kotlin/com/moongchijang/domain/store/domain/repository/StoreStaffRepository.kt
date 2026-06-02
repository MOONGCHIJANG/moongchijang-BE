package com.moongchijang.domain.store.domain.repository

import com.moongchijang.domain.store.domain.entity.StoreStaff
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface StoreStaffRepository : JpaRepository<StoreStaff, Long> {

    fun existsByUserIdAndStoreId(userId: Long, storeId: Long): Boolean

    @Query("SELECT ss FROM StoreStaff ss JOIN FETCH ss.store WHERE ss.user.id = :userId")
    fun findAllByUserId(@Param("userId") userId: Long): List<StoreStaff>

    @Query("SELECT ss.store.id FROM StoreStaff ss WHERE ss.user.id = :userId")
    fun findStoreIdsByUserId(@Param("userId") userId: Long): List<Long>

    @Query("SELECT ss.user.id FROM StoreStaff ss WHERE ss.store.id = :storeId")
    fun findUserIdsByStoreId(@Param("storeId") storeId: Long): List<Long>

    @Query("SELECT ss.store.id as storeId, ss.user.id as userId FROM StoreStaff ss WHERE ss.store.id IN :storeIds")
    fun findStoreStaffMappingsByStoreIdIn(@Param("storeIds") storeIds: Collection<Long>): List<StoreStaffUserMapping>
}

interface StoreStaffUserMapping {
    val storeId: Long
    val userId: Long
}
