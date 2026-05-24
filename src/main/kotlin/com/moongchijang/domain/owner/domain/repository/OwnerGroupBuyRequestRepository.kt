package com.moongchijang.domain.owner.domain.repository

import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequest
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OwnerGroupBuyRequestRepository : JpaRepository<OwnerGroupBuyRequest, Long> {

    @Query("SELECT r FROM OwnerGroupBuyRequest r WHERE r.owner.id = :ownerId ORDER BY r.createdAt DESC")
    fun findByOwnerIdOrderByCreatedAtDesc(@Param("ownerId") ownerId: Long): List<OwnerGroupBuyRequest>

    fun findByStatusOrderByCreatedAtAsc(status: OwnerGroupBuyRequestStatus): List<OwnerGroupBuyRequest>
}
