package com.moongchijang.domain.payment.application

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "monitoring.payment.synthetic")
data class PaymentSyntheticMetricsProperties(
    val orderSuccessCount: Int = 2,
    val orderFailureCount: Int = 1,
    val approvalSuccessCount: Int = 2,
    val approvalFailureCount: Int = 1,
    val webhookSuccessCount: Int = 1,
    val webhookFailureCount: Int = 1,
    val refundSuccessCount: Int = 1,
    val refundFailureCount: Int = 0,
    val cancelSuccessCount: Int = 1,
    val portoneSuccessCount: Int = 2,
    val portoneFailureCount: Int = 1,
    val portoneSuccessLatencyMs: Long = 180,
    val portoneFailureLatencyMs: Long = 850,
)
