package com.moongchijang.experiment.payment

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class PaymentConcurrencyExperimentTest {

    private val experiment = PaymentConcurrencyExperiment()
    private val experimentClient = PaymentExperimentClient()

    @Disabled("로컬 MySQL/Redis, 앱 2개, override 주입 및 reset.sql 실행 후 수동 실행")
    @Test
    fun `결제 완료 동시성 실험을 수동 실행한다`() {
        val config = PaymentExperimentConfig.FULL_PROTECTION.copy(requestCount = 20)
        val groupBuyId = 1L

        val requests = experimentClient.preparePaymentRequests(
            port = 8081,
            groupBuyId = groupBuyId,
            userCount = config.requestCount,
            quantityPerOrder = 1,
        )

        val results = experiment.runCompletePaymentExperiment(
            config = config,
            requests = requests,
        )

        println(results.joinToString(separator = "\n"))
    }

    @Disabled("로컬 앱 기동, override 주입 및 테스트용 paymentId 준비 후 수동 실행")
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
