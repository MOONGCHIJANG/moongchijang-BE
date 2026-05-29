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
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.access.prepost.PreAuthorize
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
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/group-buys/{groupBuyId}/checkout")
    fun getCheckoutInfo(
        @PathVariable groupBuyId: Long,
        @RequestParam quantity: Int,
    ): ResponseEntity<ApiResponse<CheckoutInfoResponse>> {
        log.info("[PaymentController] 결제 체크아웃 정보 조회 요청: groupBuyId={}, quantity={}", groupBuyId, quantity)
        val response = ResponseEntity.ok(ApiResponse.success(paymentService.getCheckoutInfo(groupBuyId, quantity)))
        log.info("[PaymentController] 결제 체크아웃 정보 조회 응답 완료: groupBuyId={}", groupBuyId)
        return response
    }

    @PostMapping("/group-buys/{groupBuyId}/payment-orders")
    @PreAuthorize("isAuthenticated()")
    fun createPaymentOrder(
        @PathVariable groupBuyId: Long,
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: CreatePaymentOrderRequest,
    ): ResponseEntity<ApiResponse<CreatePaymentOrderResponse>> {
        log.info("[PaymentController] 결제 주문 생성 요청: groupBuyId={}, userId={}", groupBuyId, principal.id)
        val response = ResponseEntity.ok(ApiResponse.success(paymentService.createPaymentOrder(groupBuyId, principal.id, request)))
        log.info("[PaymentController] 결제 주문 생성 응답 완료: groupBuyId={}, userId={}", groupBuyId, principal.id)
        return response
    }

    @PostMapping("/payments/portone/complete")
    @PreAuthorize("isAuthenticated()")
    fun completePortOnePayment(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: CompletePortOnePaymentRequest,
    ): ResponseEntity<ApiResponse<ConfirmPaymentResponse>> {
        log.info("[PaymentController] 결제 완료 확인 요청: userId={}", principal.id)
        val response = ResponseEntity.ok(ApiResponse.success(paymentService.completePortOnePayment(request, principal.id)))
        log.info("[PaymentController] 결제 완료 확인 응답 완료: userId={}", principal.id)
        return response
    }

    @PostMapping("/payments/portone/webhook")
    fun handlePortOneWebhook(
        @RequestBody request: PortOneWebhookRequest,
    ): ResponseEntity<ApiResponse<PortOneWebhookResponse>> {
        log.info("[PaymentController] PortOne 웹훅 처리 요청 수신")
        paymentService.handlePortOneWebhook(request)
        val response = ResponseEntity.ok(ApiResponse.success(PortOneWebhookResponse()))
        log.info("[PaymentController] PortOne 웹훅 처리 응답 완료")
        return response
    }

    @PostMapping("/participations/{participationId}/cancel")
    @PreAuthorize("isAuthenticated()")
    fun cancelParticipation(
        @PathVariable participationId: Long,
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: CancelParticipationRequest,
    ): ResponseEntity<ApiResponse<CancelParticipationResponse>> {
        log.info("[PaymentController] 참여 취소 요청: participationId={}, userId={}", participationId, principal.id)
        val response = ResponseEntity.ok(ApiResponse.success(paymentService.cancelParticipation(participationId, principal.id, request)))
        log.info("[PaymentController] 참여 취소 응답 완료: participationId={}, userId={}", participationId, principal.id)
        return response
    }
}
