package com.moongchijang.domain.payment.experiment

object PaymentExperimentRuntime {
    @Volatile
    var overrides: PaymentExperimentOverrides = PaymentExperimentOverrides()

    fun use(overrides: PaymentExperimentOverrides) {
        this.overrides = overrides
    }

    fun clear() {
        overrides = PaymentExperimentOverrides()
    }
}
