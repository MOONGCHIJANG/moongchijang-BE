package com.moongchijang.domain.payment.presentation

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.moongchijang.domain.payment.application.PaymentService
import com.moongchijang.domain.payment.application.PortOneWebhookSignatureVerifier
import com.moongchijang.domain.payment.application.dto.CancelParticipationRequest
import com.moongchijang.domain.payment.application.dto.CancelParticipationResponse
import com.moongchijang.domain.payment.application.dto.CheckoutInfoResponse
import com.moongchijang.domain.payment.application.dto.CompletePortOnePaymentRequest
import com.moongchijang.domain.payment.application.dto.ConfirmPaymentResponse
import com.moongchijang.domain.payment.application.dto.CreatePaymentOrderRequest
import com.moongchijang.domain.payment.application.dto.CreatePaymentOrderResponse
import com.moongchijang.domain.payment.application.dto.PortOneWebhookRequest
import com.moongchijang.domain.payment.application.dto.PortOneWebhookResponse
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.authorization.RequireCurrentRole
import com.moongchijang.security.principal.CustomUserPrincipal
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class PaymentController(
    private val paymentService: PaymentService,
    private val portOneWebhookSignatureVerifier: PortOneWebhookSignatureVerifier,
    private val objectMapper: ObjectMapper,
) {

    @GetMapping("/group-buys/{groupBuyId}/checkout")
    @RequireCurrentRole(UserRole.BUYER)
    fun getCheckoutInfo(
        @PathVariable groupBuyId: Long,
        @RequestParam quantity: Int,
    ): ResponseEntity<ApiResponse<CheckoutInfoResponse>> {
        val response = ResponseEntity.ok(ApiResponse.success(paymentService.getCheckoutInfo(groupBuyId, quantity)))
        return response
    }

    @PostMapping("/group-buys/{groupBuyId}/payment-orders")
    @PreAuthorize("isAuthenticated()")
    @RequireCurrentRole(UserRole.BUYER)
    fun createPaymentOrder(
        @PathVariable groupBuyId: Long,
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: CreatePaymentOrderRequest,
    ): ResponseEntity<ApiResponse<CreatePaymentOrderResponse>> {
        val response = ResponseEntity.ok(ApiResponse.success(paymentService.createPaymentOrder(groupBuyId, principal.id, request)))
        return response
    }

    @PostMapping("/payments/portone/complete")
    @PreAuthorize("isAuthenticated()")
    @RequireCurrentRole(UserRole.BUYER)
    fun completePortOnePayment(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: CompletePortOnePaymentRequest,
    ): ResponseEntity<ApiResponse<ConfirmPaymentResponse>> {
        val response = ResponseEntity.ok(ApiResponse.success(paymentService.completePortOnePayment(request, principal.id)))
        return response
    }

    @PostMapping("/payments/portone/webhook")
    fun handlePortOneWebhook(
        @RequestBody rawPayload: String,
        @RequestHeader headers: HttpHeaders,
    ): ResponseEntity<ApiResponse<PortOneWebhookResponse>> {
        portOneWebhookSignatureVerifier.verify(headers, rawPayload)
        val request = try {
            objectMapper.readValue(rawPayload, PortOneWebhookRequest::class.java)
        } catch (e: JsonProcessingException) {
            throw CustomException(ErrorCode.PAYMENT_WEBHOOK_INVALID)
        }
        paymentService.handlePortOneWebhook(request, rawPayload)
        val response = ResponseEntity.ok(ApiResponse.success(PortOneWebhookResponse()))
        return response
    }

    @PostMapping("/participations/{participationId}/cancel")
    @PreAuthorize("isAuthenticated()")
    @RequireCurrentRole(UserRole.BUYER)
    fun cancelParticipation(
        @PathVariable participationId: Long,
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: CancelParticipationRequest,
    ): ResponseEntity<ApiResponse<CancelParticipationResponse>> {
        val response = ResponseEntity.ok(ApiResponse.success(paymentService.cancelParticipation(participationId, principal.id, request)))
        return response
    }
}
