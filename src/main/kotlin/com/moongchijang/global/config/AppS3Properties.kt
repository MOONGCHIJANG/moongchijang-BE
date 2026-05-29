package com.moongchijang.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.s3")
data class AppS3Properties(
    val bucket: String = "",
    val prefix: String = "",
    val region: String = "ap-northeast-2",
    val presignExpirationSeconds: Long = 900,
    val publicBaseUrl: String = "",
)
