package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.notification.application.NotificationEventPublisher
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

@Service
class GroupBuyStatusTransitionService(
    private val groupBuyRepository: GroupBuyRepository,
    private val participationRepository: ParticipationRepository,
    private val notificationEventPublisher: NotificationEventPublisher,
    private val transactionManager: PlatformTransactionManager,
    @Value("\${groupbuy.status-transition.batch-size:500}")
    private val batchSize: Int
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun transitionExpiredGroupBuys() {
        transitionExpiredGroupBuysAt(LocalDateTime.now())
    }

    fun transitionExpiredGroupBuysAt(now: LocalDateTime) {
        val effectiveBatchSize = batchSize.coerceAtLeast(1)
        val pageable = PageRequest.of(0, effectiveBatchSize, Sort.by(Sort.Order.asc("deadline"), Sort.Order.asc("id")))
        var total = 0
        var inProgressToFailed = 0
        var achievedToCompleted = 0
        var batchCount = 0

        while (true) {
            val result = requiresNewTransactionTemplate().execute {
                transitionOneBatch(now, pageable)
            } ?: BatchTransitionResult.EMPTY

            if (result.total == 0) {
                break
            }

            batchCount++
            total += result.total
            inProgressToFailed += result.inProgressToFailed
            achievedToCompleted += result.achievedToCompleted

            if (result.total < effectiveBatchSize) {
                break
            }
        }

        log.info(
            "[GroupBuyStatusTransitionService] deadline 자동 전이 완료: total={}, inProgressToFailed={}, achievedToCompleted={}, batchSize={}, batchCount={}",
            total, inProgressToFailed, achievedToCompleted, effectiveBatchSize, batchCount
        )
    }

    private fun transitionOneBatch(now: LocalDateTime, pageable: PageRequest): BatchTransitionResult {
        val targets = groupBuyRepository.findByStatusInAndDeadlineLessThanEqual(
            statuses = listOf(GroupBuyStatus.IN_PROGRESS, GroupBuyStatus.ACHIEVED),
            deadline = now,
            pageable = pageable
        )
        if (targets.isEmpty()) {
            return BatchTransitionResult.EMPTY
        }

        var inProgressToFailed = 0
        var achievedToCompleted = 0

        targets.forEach { groupBuy ->
            when (groupBuy.status) {
                GroupBuyStatus.IN_PROGRESS -> {
                    groupBuy.transitionToFailedByDeadline(now)
                    publishApplyGroupBuyFailedEvent(groupBuy.id, now)
                    inProgressToFailed++
                }
                GroupBuyStatus.ACHIEVED -> {
                    groupBuy.transitionToCompletedByDeadline(now)
                    achievedToCompleted++
                }
                else -> Unit
            }
        }
        groupBuyRepository.flush()
        return BatchTransitionResult(
            total = targets.size,
            inProgressToFailed = inProgressToFailed,
            achievedToCompleted = achievedToCompleted
        )
    }

    private fun requiresNewTransactionTemplate(): TransactionTemplate =
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

    private fun publishApplyGroupBuyFailedEvent(groupBuyId: Long, occurredAt: LocalDateTime) {
        val participantUserIds = participationRepository.findDistinctUserIdsByGroupBuyId(groupBuyId)
        if (participantUserIds.isEmpty()) return

        notificationEventPublisher.publishApplyGroupBuyFailed(
            groupBuyId = groupBuyId,
            participantUserIds = participantUserIds,
            occurredAt = occurredAt
        )
    }

    private data class BatchTransitionResult(
        val total: Int,
        val inProgressToFailed: Int,
        val achievedToCompleted: Int
    ) {
        companion object {
            val EMPTY = BatchTransitionResult(0, 0, 0)
        }
    }
}
