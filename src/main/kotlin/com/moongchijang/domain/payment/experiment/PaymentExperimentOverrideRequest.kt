package com.moongchijang.domain.payment.experiment

data class PaymentExperimentOverrideRequest(
    val enabled: Boolean = true,
    val scenarioName: String = "default",
    val distributedLockEnabled: Boolean = true,
    val dbLockEnabled: Boolean = true,
    val shortCircuitEnabled: Boolean = true,
    val lockWaitMs: Long? = null,
    val lockLeaseMs: Long? = null,
    val lockRetryCount: Int = 0,
    val lockRetryDelayMs: Long = 0,
    val sleepBeforeCommitMs: Long = 0,
    val fakePgEnabled: Boolean = false,
    val fakePgStatus: String = "PAID",
    val fakePgMethod: String? = "CARD",
) {
    fun toOverrides(): PaymentExperimentOverrides {
        return PaymentExperimentOverrides(
            enabled = enabled,
            scenarioName = scenarioName,
            distributedLockEnabled = distributedLockEnabled,
            dbLockEnabled = dbLockEnabled,
            shortCircuitEnabled = shortCircuitEnabled,
            lockWaitMs = lockWaitMs,
            lockLeaseMs = lockLeaseMs,
            lockRetryCount = lockRetryCount,
            lockRetryDelayMs = lockRetryDelayMs,
            sleepBeforeCommitMs = sleepBeforeCommitMs,
            fakePgEnabled = fakePgEnabled,
            fakePgStatus = fakePgStatus,
            fakePgMethod = fakePgMethod,
        )
    }
}
