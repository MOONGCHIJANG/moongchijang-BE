package com.moongchijang.domain.together.domain.repository

import com.moongchijang.domain.together.domain.entity.PickupQrCode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface PickupQrCodeRepository : JpaRepository<PickupQrCode, Long> {

    @Query(
        """
        SELECT qr.room.id
        FROM PickupQrCode qr
        WHERE qr.room.id IN :roomIds
          AND qr.usedAt IS NULL
          AND qr.activeFrom <= :now
          AND qr.expiresAt > :now
        """
    )
    fun findActiveRoomIds(
        @Param("roomIds") roomIds: Collection<Long>,
        @Param("now") now: LocalDateTime,
    ): Set<Long>
}
