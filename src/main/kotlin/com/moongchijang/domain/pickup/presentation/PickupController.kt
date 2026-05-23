package com.moongchijang.domain.pickup.presentation

import com.moongchijang.domain.pickup.application.PickupService
import com.moongchijang.domain.pickup.application.dto.NearestPickupQrResponse
import com.moongchijang.domain.pickup.application.dto.PickupGuideResponse
import com.moongchijang.domain.pickup.application.dto.PickupQrResponse
import com.moongchijang.domain.pickup.application.dto.PickupVerifyResponse
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
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

    @GetMapping("/participations/{participationId}/pickup")
    fun getPickupGuide(
        @PathVariable participationId: Long,
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
    ): ResponseEntity<ApiResponse<PickupGuideResponse>> {
        val userId = principal?.id ?: throw CustomException(ErrorCode.INVALID_LOGIN)
        return ResponseEntity.ok(ApiResponse.success(pickupService.getPickupGuide(participationId, userId)))
    }

    @GetMapping("/participations/{participationId}/qr")
    fun getPickupQr(
        @PathVariable participationId: Long,
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
    ): ResponseEntity<ApiResponse<PickupQrResponse>> {
        val userId = principal?.id ?: throw CustomException(ErrorCode.INVALID_LOGIN)
        return ResponseEntity.ok(ApiResponse.success(pickupService.getPickupQr(participationId, userId)))
    }

    @GetMapping("/pickups/me/nearest-qr")
    fun getNearestPickupQr(
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
    ): ResponseEntity<ApiResponse<NearestPickupQrResponse>> {
        val userId = principal?.id ?: throw CustomException(ErrorCode.INVALID_LOGIN)
        return ResponseEntity.ok(ApiResponse.success(pickupService.getNearestPickupQr(userId)))
    }

    @PostMapping("/pickups/{qrCode}/verify")
    fun verifyPickup(
        @PathVariable qrCode: String,
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
    ): ResponseEntity<ApiResponse<PickupVerifyResponse>> {
        val userId = principal.requirePickupVerifierUserId()
        return ResponseEntity.ok(ApiResponse.success(pickupService.verifyPickup(qrCode, userId)))
    }

    private fun CustomUserPrincipal?.requirePickupVerifierUserId(): Long {
        val authenticated = this ?: throw CustomException(ErrorCode.INVALID_LOGIN)
        if (authenticated.role !in PICKUP_VERIFIER_ROLES) {
            throw AccessDeniedException("Pickup verification requires ADMIN or SELLER role")
        }
        return authenticated.id
    }

    companion object {
        private val PICKUP_VERIFIER_ROLES = setOf(UserRole.ADMIN, UserRole.SELLER)
    }
}
