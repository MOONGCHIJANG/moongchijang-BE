package com.moongchijang.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "search.index")
data class SearchIndexProperties(
    val versionKey: String = "search:index:version",
    val defaultVersion: String = "v1"
)
