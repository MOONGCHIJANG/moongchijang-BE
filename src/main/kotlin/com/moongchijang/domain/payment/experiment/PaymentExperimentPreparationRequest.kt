package com.moongchijang.domain.payment.experiment

data class PaymentExperimentPreparationRequest(
    val groupBuyId: Long,
    val userCount: Int,
    val quantityPerOrder: Int = 1,
    val emailPrefix: String = "experiment-buyer",
    val emailDomain: String = "moongchijang.local",
    val password: String = "abc12345",
)
