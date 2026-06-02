package com.moongchijang.domain.notification.application

import com.moongchijang.domain.notification.application.dto.NotificationCategory
import com.moongchijang.domain.notification.application.dto.NotificationCursor
import com.moongchijang.domain.notification.application.dto.NotificationItemResponse
import com.moongchijang.domain.notification.application.dto.NotificationListResponse
import com.moongchijang.domain.notification.domain.repository.NotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

@Service
class NotificationQueryService(
    private val notificationRepository: NotificationRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getNotifications(
        userId: Long,
        category: NotificationCategory,
        cursor: String?,
        limit: Int
    ): NotificationListResponse {
        val safeLimit = limit.coerceIn(MIN_LIMIT, MAX_LIMIT)
        val decodedCursor = cursor?.let { NotificationCursor.decode(it) }
        log.info(
            "[NotificationQueryService] 알림 목록 조회 시작: userId={}, category={}, hasCursor={}, requestedLimit={}, safeLimit={}",
            userId,
            category,
            cursor != null,
            limit,
            safeLimit
        )

        val pageable = PageRequest.of(0, safeLimit + 1)
        val notifications = category.ownerTriggerTypesOrNull()?.let { ownerTriggerTypes ->
            notificationRepository.findForListByTriggerTypes(
                userId = userId,
                triggerTypes = ownerTriggerTypes,
                cursorOccurredAt = decodedCursor?.occurredAt,
                cursorId = decodedCursor?.id,
                pageable = pageable
            )
        } ?: notificationRepository.findForList(
            userId = userId,
            type = category.toNotificationTypeOrNull(),
            cursorOccurredAt = decodedCursor?.occurredAt,
            cursorId = decodedCursor?.id,
            pageable = pageable
        )

        val hasNext = notifications.size > safeLimit
        val content = if (hasNext) notifications.dropLast(1) else notifications
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
        val items = content.map { NotificationItemResponse.from(it, today) }
        val nextCursor = content.lastOrNull()?.let { NotificationCursor(it.occurredAt, it.id).encode() }
        log.info(
            "[NotificationQueryService] 알림 목록 조회 완료: userId={}, category={}, contentSize={}, hasNext={}",
            userId,
            category,
            items.size,
            hasNext
        )

        return NotificationListResponse(
            items = items,
            nextCursor = if (hasNext) nextCursor else null,
            hasNext = hasNext
        )
    }

    companion object {
        private const val MIN_LIMIT = 1
        private const val MAX_LIMIT = 100
    }
}
