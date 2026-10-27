package com.moongchijang.domain.together.domain.repository

import com.moongchijang.domain.together.domain.entity.TogetherRoomMember
import com.moongchijang.domain.together.domain.entity.TogetherRoomMemberStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TogetherRoomMemberRepository : JpaRepository<TogetherRoomMember, Long> {

    @Query(
        """
        SELECT m
        FROM TogetherRoomMember m
        JOIN FETCH m.room r
        JOIN FETCH r.product
        JOIN FETCH r.store
        WHERE m.user.id = :userId
          AND m.status <> :cancelledStatus
        """
    )
    fun findAllActiveWithRoomByUserId(
        @Param("userId") userId: Long,
        @Param("cancelledStatus") cancelledStatus: TogetherRoomMemberStatus = TogetherRoomMemberStatus.CANCELLED,
    ): List<TogetherRoomMember>
}
