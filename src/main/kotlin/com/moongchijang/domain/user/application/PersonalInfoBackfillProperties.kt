package com.moongchijang.domain.user.application

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "maintenance.personal-info-backfill")
data class PersonalInfoBackfillProperties(
    val enabled: Boolean = false,
    val batchSize: Int = 500,
)
