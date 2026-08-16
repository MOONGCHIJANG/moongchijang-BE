package com.moongchijang.domain.payment.experiment

data class PaymentExperimentOverrides(
    val enabled: Boolean = false,
    val scenarioName: String = "default",
    val distributedLockEnabled: Boolean = true,
    val dbLockEnabled: Boolean = true,
    val shortCircuitEnabled: Boolean = true,
    val lockLeaseMs: Long? = null,
    val sleepBeforeCommitMs: Long = 0,
)
