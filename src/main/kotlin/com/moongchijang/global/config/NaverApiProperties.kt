package com.moongchijang.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "naver.api")
data class NaverApiProperties(
    val clientId: String,
    val clientSecret: String,
    val localSearchUrl: String = "https://openapi.naver.com/v1/search/local.json"
)
