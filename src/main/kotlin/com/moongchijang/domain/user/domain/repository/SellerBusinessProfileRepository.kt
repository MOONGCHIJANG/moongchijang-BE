package com.moongchijang.domain.user.domain.repository

import com.moongchijang.domain.user.domain.entity.SellerBusinessProfile
import org.springframework.data.jpa.repository.JpaRepository

interface SellerBusinessProfileRepository : JpaRepository<SellerBusinessProfile, Long> {
    fun findByUserId(userId: Long): SellerBusinessProfile?
    fun existsByUserId(userId: Long): Boolean
    fun deleteByUserId(userId: Long): Long

    fun existsByBusinessRegistrationNumber(businessRegistrationNumber: String): Boolean
}
