package com.moongchijang.domain.payment.experiment

import com.moongchijang.global.response.ApiResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/experiments/payment-preparation")
class PaymentExperimentPreparationController(
    private val paymentExperimentPreparationService: PaymentExperimentPreparationService,
) {
    @PostMapping
    fun prepare(
        @RequestBody request: PaymentExperimentPreparationRequest,
    ): ApiResponse<PaymentExperimentPreparationResponse> {
        return ApiResponse.success(paymentExperimentPreparationService.prepare(request))
    }
}
