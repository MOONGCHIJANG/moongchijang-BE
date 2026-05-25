package com.moongchijang.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "business-registration.api")
data class BusinessRegistrationApiProperties(
    val enabled: Boolean = false,
    val url: String = "",
    val serviceKey: String = "",
)
