package com.moongchijang.domain.mypage.presentation

import com.moongchijang.domain.mypage.application.MypageService
import com.moongchijang.domain.mypage.application.dto.MypageGroupBuyRequestResponse
import com.moongchijang.domain.mypage.application.dto.MypageParticipationResponse
import com.moongchijang.domain.mypage.application.dto.MypageParticipationStatusFilter
import com.moongchijang.domain.mypage.application.dto.MypageRefundResponse
import com.moongchijang.domain.mypage.application.dto.MypageSummaryResponse
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.authorization.RequireCurrentRole
import com.moongchijang.security.principal.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
@RequireCurrentRole(UserRole.BUYER)
@Tag(name = "MyPage", description = "마이페이지")
class MypageController(
    private val mypageService: MypageService
) {

    @GetMapping("/mypage/summary")
    @Operation(summary = "마이페이지 탭별 건수 조회")
    fun getSummary(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<ApiResponse<MypageSummaryResponse>> {
        val response = ResponseEntity.ok(ApiResponse.success(mypageService.getSummary(principal.id)))
        return response
    }

    @GetMapping("/users/me/tabs/counts")
    @Operation(summary = "내 탭별 건수 조회")
    fun getUserTabCounts(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<ApiResponse<MypageSummaryResponse>> {
        val response = ResponseEntity.ok(ApiResponse.success(mypageService.getSummary(principal.id)))
        return response
    }

    @GetMapping("/users/me/participations")
    @Operation(summary = "내 참여 내역 상태별 조회")
    fun getUserParticipations(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @RequestParam status: MypageParticipationStatusFilter
    ): ResponseEntity<ApiResponse<List<MypageParticipationResponse>>> {
        val response = ResponseEntity.ok(ApiResponse.success(mypageService.getParticipations(principal.id, status)))
        return response
    }

    @GetMapping("/users/me/group-buy-requests")
    @Operation(summary = "내 공구 개설 요청 내역 조회")
    fun getUserGroupBuyRequests(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<ApiResponse<List<MypageGroupBuyRequestResponse>>> {
        val response = ResponseEntity.ok(ApiResponse.success(mypageService.getGroupBuyRequests(principal.id)))
        return response
    }

    @GetMapping("/mypage/refunds")
    @Operation(summary = "내 환불 내역 조회")
    fun getRefunds(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<ApiResponse<List<MypageRefundResponse>>> {
        val response = ResponseEntity.ok(ApiResponse.success(mypageService.getRefunds(principal.id)))
        return response
    }

    @GetMapping("/users/me/refunds")
    @Operation(summary = "내 환불 내역 조회 (마이페이지)")
    fun getUserRefunds(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<ApiResponse<List<MypageRefundResponse>>> {
        val response = ResponseEntity.ok(ApiResponse.success(mypageService.getRefunds(principal.id)))
        return response
    }

    @GetMapping("/mypage/group-buy-requests")
    @Operation(summary = "내 공구 개설 요청 내역 조회")
    fun getGroupBuyRequests(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<ApiResponse<List<MypageGroupBuyRequestResponse>>> {
        val response = ResponseEntity.ok(ApiResponse.success(mypageService.getGroupBuyRequests(principal.id)))
        return response
    }
}
