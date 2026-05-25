package com.moongchijang.domain.owner.presentation

import com.moongchijang.domain.owner.application.OwnerGroupBuyRequestService
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyRequestCreateRequest
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyRequestCreateResponse
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/owner/group-buy-requests")
@Tag(name = "OwnerGroupBuyRequest", description = "사장님 공구 개설 요청")
class OwnerGroupBuyRequestController(
    private val ownerGroupBuyRequestService: OwnerGroupBuyRequestService
) {

    @PostMapping
    @Operation(summary = "사장님 공구 개설 요청 제출")
    fun create(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: OwnerGroupBuyRequestCreateRequest
    ): ResponseEntity<ApiResponse<OwnerGroupBuyRequestCreateResponse>> {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(ownerGroupBuyRequestService.create(principal.id, request)))
    }
}
