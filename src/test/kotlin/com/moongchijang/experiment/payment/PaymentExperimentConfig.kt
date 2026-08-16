package com.moongchijang.experiment.payment

import com.moongchijang.domain.payment.experiment.PaymentExperimentOverrides

data class PaymentExperimentConfig(
    val name: String,
    val requestCount: Int,
    val appPorts: List<Int>,
    val distributedLockEnabled: Boolean,
    val dbLockEnabled: Boolean,
    val shortCircuitEnabled: Boolean,
    val lockLeaseMs: Long,
    val sleepBeforeCommitMs: Long,
) {
    init {
        require(requestCount > 0) { "requestCount must be positive." }
        require(appPorts.isNotEmpty()) { "appPorts must not be empty." }
        require(lockLeaseMs >= 0) { "lockLeaseMs must be zero or positive." }
        require(sleepBeforeCommitMs >= 0) { "sleepBeforeCommitMs must be zero or positive." }
    }

    fun toOverrides(): PaymentExperimentOverrides {
        return PaymentExperimentOverrides(
            enabled = true,
            scenarioName = name,
            distributedLockEnabled = distributedLockEnabled,
            dbLockEnabled = dbLockEnabled,
            shortCircuitEnabled = shortCircuitEnabled,
            lockLeaseMs = if (lockLeaseMs > 0L) lockLeaseMs else null,
            sleepBeforeCommitMs = sleepBeforeCommitMs,
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
            lockLeaseMs = 55_000,
            sleepBeforeCommitMs = 0,
        )

        val DISTRIBUTED_LOCK_ONLY = PaymentExperimentConfig(
            name = "distributed-lock-only",
            requestCount = 200,
            appPorts = listOf(8081, 8082),
            distributedLockEnabled = true,
            dbLockEnabled = false,
            shortCircuitEnabled = true,
            lockLeaseMs = 55_000,
            sleepBeforeCommitMs = 0,
        )

        val CONDITIONAL_UPDATE_ONLY = PaymentExperimentConfig(
            name = "conditional-update-only",
            requestCount = 200,
            appPorts = listOf(8081, 8082),
            distributedLockEnabled = false,
            dbLockEnabled = false,
            shortCircuitEnabled = false,
            lockLeaseMs = 0,
            sleepBeforeCommitMs = 0,
        )

        val ALL_OFF = PaymentExperimentConfig(
            name = "all-off",
            requestCount = 200,
            appPorts = listOf(8081, 8082),
            distributedLockEnabled = false,
            dbLockEnabled = false,
            shortCircuitEnabled = false,
            lockLeaseMs = 0,
            sleepBeforeCommitMs = 0,
        )

        val LOCK_RELEASED_BEFORE_COMMIT = PaymentExperimentConfig(
            name = "lock-released-before-commit",
            requestCount = 200,
            appPorts = listOf(8081, 8082),
            distributedLockEnabled = true,
            dbLockEnabled = true,
            shortCircuitEnabled = true,
            lockLeaseMs = 100,
            sleepBeforeCommitMs = 500,
        )
    }
}
