package com.moongchijang.domain.owner.domain.repository

import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OwnerGroupBuyRequestImageRepository : JpaRepository<OwnerGroupBuyRequestImage, Long> {

    @Query("SELECT i FROM OwnerGroupBuyRequestImage i WHERE i.request.id = :requestId ORDER BY i.sortOrder ASC")
    fun findAllByRequestIdOrderBySortOrderAsc(@Param("requestId") requestId: Long): List<OwnerGroupBuyRequestImage>
}
