package com.moongchijang.domain.pickup.presentation

import com.moongchijang.domain.pickup.application.PickupService
import com.moongchijang.domain.pickup.application.dto.NearestPickupQrResponse
import com.moongchijang.domain.pickup.application.dto.PickupGuideResponse
import com.moongchijang.domain.pickup.application.dto.PickupQrResponse
import com.moongchijang.domain.pickup.application.dto.PickupVerifyResponse
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class PickupController(
    private val pickupService: PickupService,
) {
    private val log = LoggerFactory.getLogger(PickupController::class.java)

    @GetMapping("/participations/{participationId}/pickup")
    fun getPickupGuide(
        @PathVariable participationId: Long,
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
    ): ResponseEntity<ApiResponse<PickupGuideResponse>> {
        val userId = principal?.id ?: throw CustomException(ErrorCode.INVALID_LOGIN)
        log.info("[PickupController] 픽업 가이드 조회 요청: participationId={}, userId={}", participationId, userId)
        val response = ResponseEntity.ok(ApiResponse.success(pickupService.getPickupGuide(participationId, userId)))
        log.info("[PickupController] 픽업 가이드 조회 응답 완료: participationId={}, userId={}", participationId, userId)
        return response
    }

    @GetMapping("/participations/{participationId}/qr")
    fun getPickupQr(
        @PathVariable participationId: Long,
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
    ): ResponseEntity<ApiResponse<PickupQrResponse>> {
        val userId = principal?.id ?: throw CustomException(ErrorCode.INVALID_LOGIN)
        log.info("[PickupController] 픽업 QR 조회 요청: participationId={}, userId={}", participationId, userId)
        val response = ResponseEntity.ok(ApiResponse.success(pickupService.getPickupQr(participationId, userId)))
        log.info("[PickupController] 픽업 QR 조회 응답 완료: participationId={}, userId={}", participationId, userId)
        return response
    }

    @GetMapping("/pickups/me/nearest-qr")
    fun getNearestPickupQr(
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
    ): ResponseEntity<ApiResponse<NearestPickupQrResponse>> {
        val userId = principal?.id ?: throw CustomException(ErrorCode.INVALID_LOGIN)
        log.info("[PickupController] 가장 가까운 픽업 QR 조회 요청: userId={}", userId)
        val response = ResponseEntity.ok(ApiResponse.success(pickupService.getNearestPickupQr(userId)))
        log.info("[PickupController] 가장 가까운 픽업 QR 조회 응답 완료: userId={}", userId)
        return response
    }

    @PostMapping("/pickups/{qrCode}/verify")
    fun verifyPickup(
        @PathVariable qrCode: String,
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
    ): ResponseEntity<ApiResponse<PickupVerifyResponse>> {
        val userId = principal.requirePickupVerifierUserId()
        log.info("[PickupController] 픽업 검증 요청: userId={}", userId)
        val response = ResponseEntity.ok(ApiResponse.success(pickupService.verifyPickup(qrCode, userId)))
        log.info("[PickupController] 픽업 검증 응답 완료: userId={}", userId)
        return response
    }

    private fun CustomUserPrincipal?.requirePickupVerifierUserId(): Long {
        return this?.id ?: throw CustomException(ErrorCode.INVALID_LOGIN)
    }
}
