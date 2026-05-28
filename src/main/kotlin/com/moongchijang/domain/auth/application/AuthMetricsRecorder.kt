package com.moongchijang.domain.auth.application

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class AuthMetricsRecorder(
    private val meterRegistry: MeterRegistry,
) {
    fun recordLogin(provider: String, result: String) {
        meterRegistry.counter(
            "mcj_auth_login_total",
            "provider",
            provider,
            "result",
            result,
        ).increment()
    }

    fun recordSignup(method: String, result: String) {
        meterRegistry.counter(
            "mcj_auth_signup_total",
            "method",
            method,
            "result",
            result,
        ).increment()
    }

    fun recordTokenReissue(result: String) {
        meterRegistry.counter(
            "mcj_auth_token_reissue_total",
            "result",
            result,
        ).increment()
    }
}

