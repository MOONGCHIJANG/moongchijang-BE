package com.moongchijang.domain.user.application

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class PersonalInfoBackfillRunner(
    private val properties: PersonalInfoBackfillProperties,
    private val personalInfoBackfillService: PersonalInfoBackfillService,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        if (!properties.enabled) {
            return
        }

        log.warn(
            "[PersonalInfoBackfillRunner] 개인정보 백필 실행 시작: batchSize={}",
            properties.batchSize,
        )
        val updatedCount = personalInfoBackfillService.backfill(properties.batchSize)
        log.warn(
            "[PersonalInfoBackfillRunner] 개인정보 백필 실행 완료: updatedCount={}",
            updatedCount,
        )
    }
}
