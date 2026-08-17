package com.moongchijang.domain.payment.experiment

import com.moongchijang.global.response.ApiResponse
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/experiments/payment-overrides")
class PaymentExperimentController {

    @PostMapping
    fun applyOverrides(
        @RequestBody request: PaymentExperimentOverrideRequest,
    ): ApiResponse<PaymentExperimentOverrides> {
        val overrides = request.toOverrides()
        PaymentExperimentRuntime.use(overrides)
        return ApiResponse.success(overrides)
    }

    @DeleteMapping
    fun clearOverrides(): ApiResponse<Nothing> {
        PaymentExperimentRuntime.clear()
        return ApiResponse.success()
    }
}
