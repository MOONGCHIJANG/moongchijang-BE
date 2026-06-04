package com.moongchijang.domain.user.domain.repository

import com.moongchijang.domain.user.domain.entity.SellerBusinessProfile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SellerBusinessProfileRepository : JpaRepository<SellerBusinessProfile, Long> {
    fun findByUserId(userId: Long): SellerBusinessProfile?
    fun existsByUserId(userId: Long): Boolean
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM SellerBusinessProfile p WHERE p.user.id = :userId")
    fun deleteByUserId(@Param("userId") userId: Long): Long

    fun existsByBusinessRegistrationNumber(businessRegistrationNumber: String): Boolean
}
