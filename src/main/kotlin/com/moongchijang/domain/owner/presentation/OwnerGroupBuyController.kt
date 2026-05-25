package com.moongchijang.domain.owner.presentation

import com.moongchijang.domain.owner.application.OwnerGroupBuyService
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyListItemResponse
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/owner/group-buys")
@Tag(name = "OwnerGroupBuy", description = "사장님 공구 조회")
class OwnerGroupBuyController(
    private val ownerGroupBuyService: OwnerGroupBuyService
) {

    @GetMapping
    @Operation(summary = "사장님 진행 중인 공구 목록 조회")
    fun getMyGroupBuys(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<ApiResponse<List<OwnerGroupBuyListItemResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(ownerGroupBuyService.getMyGroupBuys(principal.id)))
    }
}
