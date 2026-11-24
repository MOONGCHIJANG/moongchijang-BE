package com.moongchijang.domain.store.presentation

import com.moongchijang.domain.store.application.StorePickupPolicyService
import com.moongchijang.domain.store.application.dto.StorePickupAvailabilityResponse
import com.moongchijang.domain.store.application.dto.StorePickupPolicyResponse
import com.moongchijang.domain.store.application.dto.StorePickupPolicyUpdateRequest
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.authorization.RequireCurrentRole
import com.moongchijang.security.principal.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/stores")
@Tag(name = "Store", description = "매장 픽업 정책 API")
class StorePickupPolicyController(
    private val storePickupPolicyService: StorePickupPolicyService,
) {

    @GetMapping("/{storeId}/pickup-availability")
    @Operation(summary = "방 개설용 픽업 가능 날짜·시간대 조회")
    fun getPickupAvailability(
        @PathVariable storeId: Long,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
    ): ResponseEntity<ApiResponse<StorePickupAvailabilityResponse>> {
        val response = storePickupPolicyService.getAvailability(storeId, from, to)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{storeId}/pickup-policy")
    @RequireCurrentRole(UserRole.SELLER, UserRole.ADMIN)
    @Operation(summary = "매장 픽업 정책 조회")
    fun getPickupPolicy(
        @PathVariable storeId: Long,
        @AuthenticationPrincipal principal: CustomUserPrincipal,
    ): ResponseEntity<ApiResponse<StorePickupPolicyResponse>> {
        val response = storePickupPolicyService.getPolicy(storeId, principal.id, principal.role)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PatchMapping("/{storeId}/pickup-policy")
    @RequireCurrentRole(UserRole.SELLER, UserRole.ADMIN)
    @Operation(summary = "매장 픽업 정책 변경")
    fun updatePickupPolicy(
        @PathVariable storeId: Long,
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: StorePickupPolicyUpdateRequest,
    ): ResponseEntity<ApiResponse<StorePickupPolicyResponse>> {
        val response = storePickupPolicyService.updatePolicy(storeId, principal.id, principal.role, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
