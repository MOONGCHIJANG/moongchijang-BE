package com.moongchijang.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "indexing")
data class IndexingProperties(
    val publisher: String = "spring",
    val consumer: String = "none",
    val sqs: Sqs = Sqs()
) {
    data class Sqs(
        val queueUrl: String = "",
        val messageGroupId: String = "group-buy-indexing",
        val maxMessages: Int = 5,
        val waitTimeSeconds: Int = 10,
        val visibilityTimeoutSeconds: Int = 60,
        val pollingFixedDelayMillis: Long = 5_000
    )
}
