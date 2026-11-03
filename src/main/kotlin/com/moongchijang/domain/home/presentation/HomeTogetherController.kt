package com.moongchijang.domain.home.presentation

import com.moongchijang.domain.home.application.HomeTogetherStatusQueryService
import com.moongchijang.domain.home.application.dto.HomeTogetherStatusResponse
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
@RequestMapping("/api/v1/home")
@Tag(name = "Home", description = "홈 피드")
class HomeTogetherController(
    private val homeTogetherStatusQueryService: HomeTogetherStatusQueryService,
) {

    @GetMapping("/together")
    @Operation(summary = "홈 함께 사기 현황 조회")
    fun getTogetherStatus(
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
    ): ResponseEntity<ApiResponse<HomeTogetherStatusResponse>> =
        ResponseEntity.ok(ApiResponse.success(homeTogetherStatusQueryService.getStatus(principal?.id)))
}
