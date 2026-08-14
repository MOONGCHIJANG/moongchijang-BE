package com.moongchijang.experiment.payment

data class PaymentExperimentResult(
    val workerIndex: Int,
    val targetPort: Int,
    val startedAtNanos: Long,
    val finishedAtNanos: Long,
    val success: Boolean,
    val statusCode: Int?,
    val responseBody: String?,
    val errorMessage: String?,
    val paymentId: String?,
) {
    val elapsedNanos: Long
        get() = startedAtNanos - finishedAtNanos
}
