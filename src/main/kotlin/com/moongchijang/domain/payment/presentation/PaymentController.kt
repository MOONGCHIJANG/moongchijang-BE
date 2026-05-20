package com.moongchijang.domain.payment.presentation

import com.moongchijang.domain.payment.application.PaymentService
import com.moongchijang.domain.payment.application.dto.CancelParticipationRequest
import com.moongchijang.domain.payment.application.dto.CancelParticipationResponse
import com.moongchijang.domain.payment.application.dto.CheckoutInfoResponse
import com.moongchijang.domain.payment.application.dto.CompletePortOnePaymentRequest
import com.moongchijang.domain.payment.application.dto.ConfirmPaymentResponse
import com.moongchijang.domain.payment.application.dto.CreatePaymentOrderRequest
import com.moongchijang.domain.payment.application.dto.CreatePaymentOrderResponse
import com.moongchijang.domain.payment.application.dto.PortOneWebhookRequest
import com.moongchijang.domain.payment.application.dto.PortOneWebhookResponse
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class PaymentController(
    private val paymentService: PaymentService,
) {

    @GetMapping("/group-buys/{groupBuyId}/checkout")
    fun getCheckoutInfo(
        @PathVariable groupBuyId: Long,
        @RequestParam quantity: Int,
    ): ResponseEntity<ApiResponse<CheckoutInfoResponse>> {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getCheckoutInfo(groupBuyId, quantity)))
    }

    @PostMapping("/group-buys/{groupBuyId}/payment-orders")
    fun createPaymentOrder(
        @PathVariable groupBuyId: Long,
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
        @Valid @RequestBody request: CreatePaymentOrderRequest,
    ): ResponseEntity<ApiResponse<CreatePaymentOrderResponse>> {
        val userId = principal?.id ?: throw CustomException(ErrorCode.INVALID_LOGIN)
        return ResponseEntity.ok(ApiResponse.success(paymentService.createPaymentOrder(groupBuyId, userId, request)))
    }

    @PostMapping("/payments/portone/complete")
    fun completePortOnePayment(
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
        @Valid @RequestBody request: CompletePortOnePaymentRequest,
    ): ResponseEntity<ApiResponse<ConfirmPaymentResponse>> {
        val userId = principal?.id ?: throw CustomException(ErrorCode.INVALID_LOGIN)
        return ResponseEntity.ok(ApiResponse.success(paymentService.completePortOnePayment(request, userId)))
    }

    @PostMapping("/payments/portone/webhook")
    fun handlePortOneWebhook(
        @RequestBody request: PortOneWebhookRequest,
    ): ResponseEntity<ApiResponse<PortOneWebhookResponse>> {
        paymentService.handlePortOneWebhook(request)
        return ResponseEntity.ok(ApiResponse.success(PortOneWebhookResponse()))
    }

    @PostMapping("/participations/{participationId}/cancel")
    fun cancelParticipation(
        @PathVariable participationId: Long,
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
        @Valid @RequestBody request: CancelParticipationRequest,
    ): ResponseEntity<ApiResponse<CancelParticipationResponse>> {
        val userId = principal?.id ?: throw CustomException(ErrorCode.INVALID_LOGIN)
        return ResponseEntity.ok(ApiResponse.success(paymentService.cancelParticipation(participationId, userId, request)))
    }
}
