package com.moongchijang.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "business-registration.api")
data class BusinessRegistrationApiProperties(
    val enabled: Boolean = false,
    val baseUrl: String = "https://api.odcloud.kr",
    val statusPath: String = "/api/nts-businessman/v1/status",
    val serviceKey: String = "",
)
