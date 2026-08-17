package com.moongchijang.experiment.payment

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.net.HttpURLConnection
import java.net.URI

class PaymentExperimentClient(
    private val connectTimeoutMs: Int = 3_000,
    private val readTimeoutMs: Int = 10_000,
) {
    private val objectMapper = jacksonObjectMapper()

    fun completePayment(
        port: Int,
        accessToken: String,
        paymentId: String,
        amount: Int,
        workerIndex: Int,
        startedAtNanos: Long,
    ): PaymentExperimentResult {
        val connection = createJsonPostConnection(
            url = "http://localhost:$port/api/v1/payments/portone/complete",
            accessToken = accessToken,
        )

        return try {
            val requestBody = """
                {
                  "paymentId": "$paymentId",
                  "amount": $amount
                }
            """.trimIndent()

            connection.outputStream.use { output ->
                output.write(requestBody.toByteArray(Charsets.UTF_8))
            }

            val statusCode = connection.responseCode
            val responseBody = readResponseBody(connection, statusCode)
            PaymentExperimentResult(
                workerIndex = workerIndex,
                targetPort = port,
                startedAtNanos = startedAtNanos,
                finishedAtNanos = System.nanoTime(),
                success = statusCode in 200..299,
                statusCode = statusCode,
                responseBody = responseBody,
                errorMessage = null,
                paymentId = paymentId,
            )
        } catch (e: Exception) {
            PaymentExperimentResult(
                workerIndex = workerIndex,
                targetPort = port,
                startedAtNanos = startedAtNanos,
                finishedAtNanos = System.nanoTime(),
                success = false,
                statusCode = null,
                responseBody = null,
                errorMessage = e.message,
                paymentId = paymentId,
            )
        } finally {
            connection.disconnect()
        }
    }

    fun preparePaymentRequests(
        port: Int,
        groupBuyId: Long,
        userCount: Int,
        quantityPerOrder: Int = 1,
    ): List<PreparedPaymentRequest> {
        val connection = createJsonPostConnection(
            url = "http://localhost:$port/internal/experiments/payment-preparation",
            accessToken = null,
        )

        return try {
            val requestBody = """
                {
                  "groupBuyId": $groupBuyId,
                  "userCount": $userCount,
                  "quantityPerOrder": $quantityPerOrder
                }
            """.trimIndent()

            connection.outputStream.use { output ->
                output.write(requestBody.toByteArray(Charsets.UTF_8))
            }

            val statusCode = connection.responseCode
            val responseBody = readResponseBody(connection, statusCode)

            if (statusCode !in 200..299) {
                error("preparePaymentRequests failed: status=$statusCode body=$responseBody")
            }

            val response: ExperimentApiResponse<PaymentExperimentPreparationResponseDto> =
                objectMapper.readValue(responseBody)

            response.data.requests.map {
                PreparedPaymentRequest(
                    accessToken = it.accessToken,
                    paymentId = it.paymentId,
                    amount = it.amount,
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    fun triggerWebhook(
        port: Int,
        paymentId: String,
        workerIndex: Int,
        startedAtNanos: Long,
        rawPayload: String = defaultWebhookPayload(paymentId),
    ): PaymentExperimentResult {
        val connection = createJsonPostConnection(
            url = "http://localhost:$port/api/v1/payments/portone/webhook",
            accessToken = null,
        )

        return try {
            connection.outputStream.use { output ->
                output.write(rawPayload.toByteArray(Charsets.UTF_8))
            }

            val statusCode = connection.responseCode
            val responseBody = readResponseBody(connection, statusCode)
            PaymentExperimentResult(
                workerIndex = workerIndex,
                targetPort = port,
                startedAtNanos = startedAtNanos,
                finishedAtNanos = System.nanoTime(),
                success = statusCode in 200..299,
                statusCode = statusCode,
                responseBody = responseBody,
                errorMessage = null,
                paymentId = paymentId,
            )
        } catch (e: Exception) {
            PaymentExperimentResult(
                workerIndex = workerIndex,
                targetPort = port,
                startedAtNanos = startedAtNanos,
                finishedAtNanos = System.nanoTime(),
                success = false,
                statusCode = null,
                responseBody = null,
                errorMessage = e.message,
                paymentId = paymentId,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun createJsonPostConnection(
        url: String,
        accessToken: String?,
    ): HttpURLConnection {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = connectTimeoutMs
        connection.readTimeout = readTimeoutMs
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.setRequestProperty("Accept", "application/json")

        if (!accessToken.isNullOrBlank()) {
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
        }

        return connection
    }

    private fun readResponseBody(
        connection: HttpURLConnection,
        statusCode: Int,
    ): String {
        val stream = if (statusCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: return ""
        }

        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun defaultWebhookPayload(paymentId: String): String {
        return """
        {
            "type": "Transaction.Paid",
            "storeId": "store-dummy",
            "paymentId": "$paymentId"
        }
        """.trimIndent()
    }
}

data class ExperimentApiResponse<T>(
    val success: Boolean,
    val data: T,
    val error: Any? = null,
)

data class PaymentExperimentPreparationResponseDto(
    val requests: List<PreparedPaymentRequestDto>,
)

data class PreparedPaymentRequestDto(
    val userId: Long,
    val email: String,
    val accessToken: String,
    val paymentId: String,
    val amount: Int,
)
