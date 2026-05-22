package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GroupBuyStatusTransitionService(
    private val groupBuyRepository: GroupBuyRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun transitionExpiredGroupBuys() {
        transitionExpiredGroupBuysAt(LocalDateTime.now())
    }

    @Transactional
    fun transitionExpiredGroupBuysAt(now: LocalDateTime) {
        val targets = groupBuyRepository.findAllByStatusInAndDeadlineLessThanEqual(
            statuses = listOf(GroupBuyStatus.IN_PROGRESS, GroupBuyStatus.ACHIEVED),
            deadline = now
        )

        var inProgressToFailed = 0
        var achievedToCompleted = 0

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

        log.info(
            "[GroupBuyStatusTransitionService] deadline 자동 전이 완료: total={}, inProgressToFailed={}, achievedToCompleted={}",
            targets.size, inProgressToFailed, achievedToCompleted
        )
    }
}
