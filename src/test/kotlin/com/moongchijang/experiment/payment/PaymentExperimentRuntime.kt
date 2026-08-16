package com.moongchijang.experiment.payment

object PaymentExperimentRuntime {
    @Volatile
    var currentConfig: PaymentExperimentConfig? = null

    fun use(config: PaymentExperimentConfig) {
        currentConfig = config
    }

    fun clear() {
        currentConfig = null
    }
}
