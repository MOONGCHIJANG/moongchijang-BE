package com.moongchijang.domain.notification.domain.repository

import com.moongchijang.domain.notification.domain.entity.NotificationDispatchHistory
import com.moongchijang.domain.notification.domain.entity.NotificationDispatchStatus
import com.moongchijang.domain.notification.domain.entity.NotificationTriggerType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.Optional

interface NotificationDispatchHistoryRepository : JpaRepository<NotificationDispatchHistory, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM NotificationDispatchHistory h WHERE h.userId = :userId")
    fun deleteByUserId(@Param("userId") userId: Long): Long

    fun findByUserIdAndTriggerTypeAndTargetIdAndScheduleKey(
        userId: Long,
        triggerType: NotificationTriggerType,
        targetId: Long,
        scheduleKey: String
    ): Optional<NotificationDispatchHistory>

    fun findByStatusInAndNextRetryAtLessThanEqual(
        statuses: Collection<NotificationDispatchStatus>,
        nextRetryAt: LocalDateTime
    ): List<NotificationDispatchHistory>
}
