package com.moongchijang.experiment.payment

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class PaymentConcurrencyExperimentTest {

    private val experiment = PaymentConcurrencyExperiment()

    @Disabled("로컬 MySQL/Redis, 앱 2개, 테스트용 access token/paymentIds 준비 후 수동 실행")
    @Test
    fun `결제 완료 동시성 실험을 수동 실행한다`() {
        val config = PaymentExperimentConfig.FULL_PROTECTION

        val accessToken = "PUT_REAL_ACCESS_TOKEN_HERE"
        val amount = 12_000

        val paymentIds = List(config.requestCount) { index ->
            "PUT_REAL_PAYMENT_ID_$index"
        }

        val results = experiment.runCompletePaymentExperiment(
            config = config,
            accessToken = accessToken,
            paymentIds = paymentIds,
            amount = amount,
        )

        println(results.joinToString(separator = "\n"))
    }

    @Disabled("로컬 앱 기동 및 테스트용 paymentId 준비 후 수동 실행")
    @Test
    fun `웹훅 재전송 실험을 수동 실행한다`() {
        val config = PaymentExperimentConfig.FULL_PROTECTION
        val paymentId = "PUT_REAL_PAYMENT_ID_HERE"

        val results = experiment.runWebhookReplayExperiment(
            config = config,
            paymentId = paymentId,
            requestCount = 3,
        )

        println(results.joinToString(separator = "\n"))
    }
}
