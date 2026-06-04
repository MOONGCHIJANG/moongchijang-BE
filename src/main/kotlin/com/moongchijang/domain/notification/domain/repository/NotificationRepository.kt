package com.moongchijang.domain.notification.domain.repository

import com.moongchijang.domain.notification.domain.entity.Notification
import com.moongchijang.domain.notification.domain.entity.NotificationScope
import com.moongchijang.domain.notification.domain.entity.NotificationTriggerType
import com.moongchijang.domain.notification.domain.entity.NotificationType
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface NotificationRepository : JpaRepository<Notification, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId")
    fun deleteByUserId(@Param("userId") userId: Long): Long

    @Query(
        """
        SELECT n FROM Notification n
        WHERE n.user.id = :userId
          AND n.scope = :scope
          AND (:type IS NULL OR n.type = :type)
          AND (
                :cursorOccurredAt IS NULL
                OR n.occurredAt < :cursorOccurredAt
                OR (n.occurredAt = :cursorOccurredAt AND n.id < :cursorId)
          )
        ORDER BY n.occurredAt DESC, n.id DESC
        """
    )
    fun findForListByScope(
        @Param("userId") userId: Long,
        @Param("scope") scope: NotificationScope,
        @Param("type") type: NotificationType?,
        @Param("cursorOccurredAt") cursorOccurredAt: LocalDateTime?,
        @Param("cursorId") cursorId: Long?,
        pageable: Pageable
    ): List<Notification>

    @Query(
        """
        SELECT n FROM Notification n
        WHERE n.user.id = :userId
          AND n.scope = :scope
          AND n.triggerType IN :triggerTypes
          AND (
                :cursorOccurredAt IS NULL
                OR n.occurredAt < :cursorOccurredAt
                OR (n.occurredAt = :cursorOccurredAt AND n.id < :cursorId)
          )
        ORDER BY n.occurredAt DESC, n.id DESC
        """
    )
    fun findForListByScopeAndTriggerTypes(
        @Param("userId") userId: Long,
        @Param("scope") scope: NotificationScope,
        @Param("triggerTypes") triggerTypes: Collection<NotificationTriggerType>,
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
          AND n.scope = :scope
          AND n.isRead = false
        """
    )
    fun markAllAsReadByUserIdAndScope(
        @Param("userId") userId: Long,
        @Param("scope") scope: NotificationScope,
    ): Int

    @Query(
        """
        SELECT COUNT(n)
        FROM Notification n
        WHERE n.user.id = :userId
          AND n.scope = :scope
          AND n.isRead = false
        """
    )
    fun countUnreadByUserIdAndScope(
        @Param("userId") userId: Long,
        @Param("scope") scope: NotificationScope,
    ): Long
}
