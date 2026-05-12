package com.moongchijang.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "qdrant")
data class QdrantProperties(
    val enabled: Boolean = false,
    val url: String = "http://localhost:6333",
    val apiKey: String? = null,
    val collectionName: String = "group_buys",
    val timeoutSeconds: Long = 2,
    val initializeCollection: Boolean = false,
    val vectorSize: Int = 768,
    val distance: String = "Cosine"
)
