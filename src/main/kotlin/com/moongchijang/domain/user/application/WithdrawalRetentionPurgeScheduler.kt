package com.moongchijang.domain.user.application

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class WithdrawalRetentionPurgeScheduler(
    private val withdrawalRetentionPurgeService: WithdrawalRetentionPurgeService,
) {
    @Scheduled(cron = "\${withdrawal.retention-purge.cron:0 30 3 * * *}", zone = "Asia/Seoul")
    fun purgeExpiredRetentionData() {
        withdrawalRetentionPurgeService.purgeExpired()
    }
}
