package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GroupBuyStatusTransitionService(
    private val groupBuyRepository: GroupBuyRepository,
    @Value("\${groupbuy.status-transition.batch-size:500}")
    private val batchSize: Int
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun transitionExpiredGroupBuys() {
        transitionExpiredGroupBuysAt(LocalDateTime.now())
    }

    @Transactional
    fun transitionExpiredGroupBuysAt(now: LocalDateTime) {
        val effectiveBatchSize = batchSize.coerceAtLeast(1)
        val pageable = PageRequest.of(0, effectiveBatchSize, Sort.by(Sort.Order.asc("deadline"), Sort.Order.asc("id")))
        var total = 0
        var inProgressToFailed = 0
        var achievedToCompleted = 0
        var batchCount = 0

        while (true) {
            val targets = groupBuyRepository.findByStatusInAndDeadlineLessThanEqual(
                statuses = listOf(GroupBuyStatus.IN_PROGRESS, GroupBuyStatus.ACHIEVED),
                deadline = now,
                pageable = pageable
            )
            if (targets.isEmpty()) {
                break
            }
            batchCount++

            targets.forEach { groupBuy ->
                when (groupBuy.status) {
                    GroupBuyStatus.IN_PROGRESS -> {
                        groupBuy.transitionToFailedByDeadline(now)
                        inProgressToFailed++
                    }
                    GroupBuyStatus.ACHIEVED -> {
                        groupBuy.transitionToCompletedByDeadline(now)
                        achievedToCompleted++
                    }
                    else -> Unit
                }
            }
            total += targets.size
            groupBuyRepository.flush()

            if (targets.size < effectiveBatchSize) {
                break
            }
        }

        log.info(
            "[GroupBuyStatusTransitionService] deadline 자동 전이 완료: total={}, inProgressToFailed={}, achievedToCompleted={}, batchSize={}, batchCount={}",
            total, inProgressToFailed, achievedToCompleted, effectiveBatchSize, batchCount
        )
    }
}
