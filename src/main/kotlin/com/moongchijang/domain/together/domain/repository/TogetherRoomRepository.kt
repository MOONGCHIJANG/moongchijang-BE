package com.moongchijang.domain.together.domain.repository

import com.moongchijang.domain.together.domain.entity.TogetherRoom
import com.moongchijang.domain.together.domain.entity.TogetherRoomStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TogetherRoomRepository : JpaRepository<TogetherRoom, Long> {

    @Query(
        """
        SELECT r
        FROM TogetherRoom r
        JOIN FETCH r.product
        JOIN FETCH r.store
        WHERE r.leader.id = :userId
          AND r.status NOT IN :terminalStatuses
        """
    )
    fun findActiveByLeaderId(
        @Param("userId") userId: Long,
        @Param("terminalStatuses") terminalStatuses: Set<TogetherRoomStatus>,
    ): List<TogetherRoom>

    @Query(
        """
        SELECT r.product.id AS productId, COUNT(r.id) AS roomCount
        FROM TogetherRoom r
        WHERE r.product.id IN :productIds
          AND r.status = :status
        GROUP BY r.product.id
        """
    )
    fun countByProductIdInAndStatus(
        @Param("productIds") productIds: Collection<Long>,
        @Param("status") status: TogetherRoomStatus = TogetherRoomStatus.RECRUITING,
    ): List<ProductActiveRoomCount>
}

interface ProductActiveRoomCount {
    val productId: Long
    val roomCount: Long
}
