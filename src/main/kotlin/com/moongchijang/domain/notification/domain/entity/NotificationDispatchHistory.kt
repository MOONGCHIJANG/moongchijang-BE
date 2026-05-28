package com.moongchijang.domain.notification.domain.entity

import com.moongchijang.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "notification_dispatch_histories",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_notification_dispatch_dedup",
            columnNames = ["user_id", "trigger_type", "target_id", "schedule_key"]
        )
    ],
    indexes = [
        Index(
            name = "idx_notification_dispatch_status_next_retry_at",
            columnList = "status,next_retry_at"
        ),
        Index(
            name = "idx_notification_dispatch_user_trigger_processed_at",
            columnList = "user_id,trigger_type,processed_at"
        )
    ]
)
class NotificationDispatchHistory(

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 60)
    var triggerType: NotificationTriggerType,

    @Column(name = "target_id", nullable = false)
    var targetId: Long,

    @Column(name = "schedule_key", nullable = false, length = 100)
    var scheduleKey: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: NotificationDispatchStatus = NotificationDispatchStatus.PENDING,

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,

    @Column(name = "next_retry_at")
    var nextRetryAt: LocalDateTime? = null,

    @Column(name = "processed_at")
    var processedAt: LocalDateTime? = null,

    @Column(name = "last_error", length = 1000)
    var lastError: String? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
) : BaseEntity() {
    fun markSuccess(processedAt: LocalDateTime = LocalDateTime.now()): NotificationDispatchHistory {
        status = NotificationDispatchStatus.SUCCESS
        this.processedAt = processedAt
        nextRetryAt = null
        lastError = null
        return this
    }

    fun markFailed(errorMessage: String, nextRetryAt: LocalDateTime?): NotificationDispatchHistory {
        status = NotificationDispatchStatus.FAILED
        retryCount += 1
        lastError = errorMessage
        this.nextRetryAt = nextRetryAt
        return this
    }
}
