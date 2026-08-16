package com.moongchijang.experiment.payment

import com.moongchijang.domain.payment.experiment.PaymentExperimentOverrides

object PaymentExperimentRuntime {
    @Volatile
    var currentConfig: PaymentExperimentConfig? = null

    fun use(config: PaymentExperimentOverrides) {
        currentConfig = config
    }

    fun clear() {
        currentConfig = null
    }
}
