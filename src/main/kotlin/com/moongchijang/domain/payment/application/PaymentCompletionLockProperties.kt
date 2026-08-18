package com.moongchijang.domain.payment.application

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "payment.completion.lock")
data class PaymentCompletionLockProperties(
    val waitMs: Long = 500,
    val leaseMs: Long = 10_000,
    val retryCount: Int = 0,
    val retryDelayMs: Long = 0,
)
