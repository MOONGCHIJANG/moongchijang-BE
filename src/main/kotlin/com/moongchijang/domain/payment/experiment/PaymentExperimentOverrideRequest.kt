package com.moongchijang.domain.payment.experiment

data class PaymentExperimentOverrideRequest(
    val enabled: Boolean = true,
    val scenarioName: String = "default",
    val distributedLockEnabled: Boolean = true,
    val dbLockEnabled: Boolean = true,
    val shortCircuitEnabled: Boolean = true,
    val lockLeaseMs: Long? = null,
    val sleepBeforeCommitMs: Long = 0,
) {
    fun toOverrides(): PaymentExperimentOverrides {
        return PaymentExperimentOverrides(
            enabled = enabled,
            scenarioName = scenarioName,
            distributedLockEnabled = distributedLockEnabled,
            dbLockEnabled = dbLockEnabled,
            shortCircuitEnabled = shortCircuitEnabled,
            lockLeaseMs = lockLeaseMs,
            sleepBeforeCommitMs = sleepBeforeCommitMs,
        )
    }
}
