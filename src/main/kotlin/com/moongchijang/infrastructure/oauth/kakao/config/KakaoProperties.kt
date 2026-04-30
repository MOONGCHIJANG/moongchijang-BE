package com.moongchijang.infrastructure.oauth.kakao.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth.kakao")
data class KakaoProperties(
    val clientId: String,
    val clientSecret: String,
    val tokenUri: String,
    val userInfoUri: String,
    val redirectUri: String,
)
