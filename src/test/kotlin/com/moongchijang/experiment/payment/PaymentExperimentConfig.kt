package com.moongchijang.experiment.payment

import com.moongchijang.domain.payment.experiment.PaymentExperimentOverrides

data class PaymentExperimentConfig(
    val name: String,
    val requestCount: Int,
    val appPorts: List<Int>,
    val distributedLockEnabled: Boolean,
    val dbLockEnabled: Boolean,
    val shortCircuitEnabled: Boolean,
    val lockWaitMs: Long,
    val lockLeaseMs: Long,
    val lockRetryCount: Int,
    val lockRetryDelayMs: Long,
    val sleepBeforeCommitMs: Long,
    val fakePgEnabled: Boolean,
    val fakePgStatus: String,
) {
    init {
        require(requestCount > 0) { "requestCount must be positive." }
        require(appPorts.isNotEmpty()) { "appPorts must not be empty." }
        require(lockWaitMs >= 0) { "lockWaitMs must be zero or positive." }
        require(lockLeaseMs >= 0) { "lockLeaseMs must be zero or positive." }
        require(lockRetryCount >= 0) { "lockRetryCount must be zero or positive." }
        require(lockRetryDelayMs >= 0) { "lockRetryDelayMs must be zero or positive." }
        require(sleepBeforeCommitMs >= 0) { "sleepBeforeCommitMs must be zero or positive." }
    }

    fun toOverrides(): PaymentExperimentOverrides {
        return PaymentExperimentOverrides(
            enabled = true,
            scenarioName = name,
            distributedLockEnabled = distributedLockEnabled,
            dbLockEnabled = dbLockEnabled,
            shortCircuitEnabled = shortCircuitEnabled,
            lockWaitMs = if (lockWaitMs > 0L) lockWaitMs else null,
            lockLeaseMs = if (lockLeaseMs > 0L) lockLeaseMs else null,
            lockRetryCount = lockRetryCount,
            lockRetryDelayMs = lockRetryDelayMs,
            sleepBeforeCommitMs = sleepBeforeCommitMs,
            fakePgEnabled = fakePgEnabled,
            fakePgStatus = fakePgStatus,
        )
    }

    companion object {
        val FULL_PROTECTION = PaymentExperimentConfig(
            name = "full-protection",
            requestCount = 200,
            appPorts = listOf(8081, 8082),
            distributedLockEnabled = true,
            dbLockEnabled = true,
            shortCircuitEnabled = true,
            lockWaitMs = 500,
            lockLeaseMs = 55_000,
            lockRetryCount = 0,
            lockRetryDelayMs = 0,
            sleepBeforeCommitMs = 0,
            fakePgEnabled = true,
            fakePgStatus = "PAID",
        )

        val DISTRIBUTED_LOCK_ONLY = PaymentExperimentConfig(
            name = "distributed-lock-only",
            requestCount = 200,
            appPorts = listOf(8081, 8082),
            distributedLockEnabled = true,
            dbLockEnabled = false,
            shortCircuitEnabled = true,
            lockWaitMs = 500,
            lockLeaseMs = 55_000,
            lockRetryCount = 0,
            lockRetryDelayMs = 0,
            sleepBeforeCommitMs = 0,
            fakePgEnabled = true,
            fakePgStatus = "PAID",
        )

        val CONDITIONAL_UPDATE_ONLY = PaymentExperimentConfig(
            name = "conditional-update-only",
            requestCount = 200,
            appPorts = listOf(8081, 8082),
            distributedLockEnabled = false,
            dbLockEnabled = false,
            shortCircuitEnabled = false,
            lockWaitMs = 500,
            lockLeaseMs = 0,
            lockRetryCount = 0,
            lockRetryDelayMs = 0,
            sleepBeforeCommitMs = 0,
            fakePgEnabled = true,
            fakePgStatus = "PAID",
        )

        val ALL_OFF = PaymentExperimentConfig(
            name = "all-off",
            requestCount = 200,
            appPorts = listOf(8081, 8082),
            distributedLockEnabled = false,
            dbLockEnabled = false,
            shortCircuitEnabled = false,
            lockWaitMs = 500,
            lockLeaseMs = 0,
            lockRetryCount = 0,
            lockRetryDelayMs = 0,
            sleepBeforeCommitMs = 0,
            fakePgEnabled = true,
            fakePgStatus = "PAID",
        )

        val LOCK_RELEASED_BEFORE_COMMIT = PaymentExperimentConfig(
            name = "lock-released-before-commit",
            requestCount = 200,
            appPorts = listOf(8081, 8082),
            distributedLockEnabled = true,
            dbLockEnabled = true,
            shortCircuitEnabled = true,
            lockWaitMs = 500,
            lockLeaseMs = 100,
            lockRetryCount = 0,
            lockRetryDelayMs = 0,
            sleepBeforeCommitMs = 500,
            fakePgEnabled = true,
            fakePgStatus = "PAID",
        )
    }
}
