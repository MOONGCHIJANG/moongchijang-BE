package com.moongchijang.domain.notification.domain.repository

import com.moongchijang.domain.notification.domain.entity.Notification
import com.moongchijang.domain.notification.domain.entity.NotificationType
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface NotificationRepository : JpaRepository<Notification, Long> {

    @Query(
        """
        SELECT n FROM Notification n
        WHERE n.user.id = :userId
          AND (:type IS NULL OR n.type = :type)
          AND (
                :cursorOccurredAt IS NULL
                OR n.occurredAt < :cursorOccurredAt
                OR (n.occurredAt = :cursorOccurredAt AND n.id < :cursorId)
          )
        ORDER BY n.occurredAt DESC, n.id DESC
        """
    )
    fun findForList(
        @Param("userId") userId: Long,
        @Param("type") type: NotificationType?,
        @Param("cursorOccurredAt") cursorOccurredAt: LocalDateTime?,
        @Param("cursorId") cursorId: Long?,
        pageable: Pageable
    ): List<Notification>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE Notification n
        SET n.isRead = true
        WHERE n.user.id = :userId
          AND n.isRead = false
        """
    )
    fun markAllAsReadByUserId(@Param("userId") userId: Long): Int

    @Query(
        """
        SELECT COUNT(n)
        FROM Notification n
        WHERE n.user.id = :userId
          AND n.isRead = false
        """
    )
    fun countUnreadByUserId(@Param("userId") userId: Long): Long
}
