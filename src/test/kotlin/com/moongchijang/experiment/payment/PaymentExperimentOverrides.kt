package com.moongchijang.experiment.payment

data class PaymentExperimentOverrides(
    val enabled: Boolean = false,
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
)
