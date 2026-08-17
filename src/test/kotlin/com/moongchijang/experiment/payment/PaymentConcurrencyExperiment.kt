package com.moongchijang.experiment.payment

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class PaymentConcurrencyExperiment(
    private val client: PaymentExperimentClient = PaymentExperimentClient(),
) {
    fun runCompletePaymentExperiment(
        config: PaymentExperimentConfig,
        requests: List<PreparedPaymentRequest>,
    ): List<PaymentExperimentResult> {
        require(requests.size == config.requestCount) {
            "requests size must match requestCount. requests=${requests.size}, requestCount=${config.requestCount}"
        }

        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(config.requestCount)
        val executor = Executors.newFixedThreadPool(config.requestCount)

        return try {
            val futures = buildCompletePaymentFutures(
                config = config,
                requests = requests,
                startLatch = startLatch,
                doneLatch = doneLatch,
                executor = executor,
            )

            println("[PaymentConcurrencyExperiment] start config=${config.name} requestCount=${config.requestCount}")
            startLatch.countDown()
            doneLatch.await()

            futures.map { it.get() }
                .also { printSummary(config, it) }
        } finally {
            executor.shutdown()
        }
    }

    fun runWebhookReplayExperiment(
        config: PaymentExperimentConfig,
        paymentId: String,
        requestCount: Int,
    ): List<PaymentExperimentResult> {
        require(requestCount > 0) { "requestCount must be positive" }

        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(requestCount)
        val executor = Executors.newFixedThreadPool(requestCount)

        return try {
            val futures = (0 until requestCount).map { workerIndex ->
                val targetPort = resolveTargetPort(config.appPorts, workerIndex)
                executor.submit(
                    Callable {
                        startLatch.await()
                        val requestStartedAt = System.nanoTime()
                        println(
                            "[PaymentConcurrencyExperiment] webhook worker=$workerIndex port=$targetPort startedAt=$requestStartedAt paymentId=$paymentId"
                        )
                        try {
                            client.triggerWebhook(
                                port = targetPort,
                                paymentId = paymentId,
                                workerIndex = workerIndex,
                                startedAtNanos = requestStartedAt,
                            )
                        } finally {
                            doneLatch.countDown()
                        }
                    }
                )
            }

            println("[PaymentConcurrencyExperiment] start webhook replay config=${config.name} requestCount=$requestCount")
            startLatch.countDown()
            doneLatch.await()

            futures.map { it.get() }
                .also { printSummary(config, it) }
        } finally {
            executor.shutdown()
        }
    }

    private fun buildCompletePaymentFutures(
        config: PaymentExperimentConfig,
        requests: List<PreparedPaymentRequest>,
        startLatch: CountDownLatch,
        doneLatch: CountDownLatch,
        executor: ExecutorService
    ): List<Future<PaymentExperimentResult>> {
        return requests.mapIndexed { workerIndex, request ->
            val targetPort = resolveTargetPort(config.appPorts, workerIndex)
            executor.submit(
                Callable {
                    startLatch.await()
                    val requestStartedAt = System.nanoTime()
                    println(
                        "[PaymentConcurrencyExperiment] complete worker=$workerIndex port=$targetPort startedAt=$requestStartedAt paymentId=${request.paymentId}"
                    )
                    try {
                        client.completePayment(
                            port = targetPort,
                            accessToken = request.accessToken,
                            paymentId = request.paymentId,
                            amount = request.amount,
                            workerIndex = workerIndex,
                            startedAtNanos = requestStartedAt,
                        )
                    } finally {
                        doneLatch.countDown()
                    }
                }
            )
        }
    }

    private fun resolveTargetPort(appPorts: List<Int>, workerIndex: Int): Int {
        return appPorts[workerIndex % appPorts.size]
    }

    private fun printSummary(
        config: PaymentExperimentConfig,
        results: List<PaymentExperimentResult>,
    ) {
        val successCount = results.count { it.success }
        val failureCount = results.size - successCount
        val statusSummary = results.groupingBy { it.statusCode ?: -1 }.eachCount()
        val slowest = results.maxByOrNull { it.elapsedNanos }

        println("========== PAYMENT EXPERIMENT SUMMARY ==========")
        println("config=${config.name}")
        println("total=${results.size}")
        println("success=$successCount")
        println("failure=$failureCount")
        println("statusSummary=$statusSummary")
        if (slowest != null) {
            println(
                "slowest worker=${slowest.workerIndex} port=${slowest.targetPort} elapsedMs=${slowest.elapsedNanos / 1_000_000.0}"
            )
        }
        println("===============================================")
    }
}
