package com.moongchijang.domain.notification.domain.entity

import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "notifications",
    indexes = [
        Index(name = "idx_notifications_user_occurred_at", columnList = "user_id,occurred_at"),
        Index(name = "idx_notifications_user_type_occurred_at", columnList = "user_id,type,occurred_at"),
        Index(name = "idx_notifications_user_is_read_occurred_at", columnList = "user_id,is_read,occurred_at")
    ]
)
class Notification(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: NotificationType,

    @Column(nullable = false, length = 120)
    var title: String,

    @Column(nullable = false, length = 500)
    var body: String,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "occurred_at", nullable = false)
    var occurredAt: LocalDateTime,

    @Column(name = "target_id")
    var targetId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "deeplink_type", nullable = false, length = 30)
    var deeplinkType: NotificationDeeplinkType,

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", length = 50)
    var triggerType: NotificationTriggerType? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
) : BaseEntity() {
    fun markAsRead() {
        if (!isRead) {
            isRead = true
        }
    }
}
