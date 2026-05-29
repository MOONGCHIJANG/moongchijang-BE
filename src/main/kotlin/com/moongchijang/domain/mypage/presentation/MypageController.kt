package com.moongchijang.domain.mypage.presentation

import com.moongchijang.domain.mypage.application.MypageService
import com.moongchijang.domain.mypage.application.dto.MypageGroupBuyRequestResponse
import com.moongchijang.domain.mypage.application.dto.MypageParticipationResponse
import com.moongchijang.domain.mypage.application.dto.MypageParticipationStatusFilter
import com.moongchijang.domain.mypage.application.dto.MypageRefundResponse
import com.moongchijang.domain.mypage.application.dto.MypageSummaryResponse
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
@Tag(name = "MyPage", description = "마이페이지")
class MypageController(
    private val mypageService: MypageService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/mypage/summary")
    @Operation(summary = "마이페이지 탭별 건수 조회")
    fun getSummary(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<ApiResponse<MypageSummaryResponse>> {
        log.info("[MypageController] 마이페이지 요약 조회 요청: userId={}", principal.id)
        val response = ResponseEntity.ok(ApiResponse.success(mypageService.getSummary(principal.id)))
        log.info("[MypageController] 마이페이지 요약 조회 응답 완료: userId={}", principal.id)
        return response
    }

    @GetMapping("/users/me/tabs/counts")
    @Operation(summary = "내 탭별 건수 조회")
    fun getUserTabCounts(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<ApiResponse<MypageSummaryResponse>> {
        log.info("[MypageController] 내 탭별 건수 조회 요청: userId={}", principal.id)
        val response = ResponseEntity.ok(ApiResponse.success(mypageService.getSummary(principal.id)))
        log.info("[MypageController] 내 탭별 건수 조회 응답 완료: userId={}", principal.id)
        return response
    }

    @GetMapping("/users/me/participations")
    @Operation(summary = "내 참여 내역 상태별 조회")
    fun getUserParticipations(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @RequestParam status: MypageParticipationStatusFilter
    ): ResponseEntity<ApiResponse<List<MypageParticipationResponse>>> {
        log.info("[MypageController] 내 참여 내역 조회 요청: userId={}, status={}", principal.id, status)
        val response = ResponseEntity.ok(ApiResponse.success(mypageService.getParticipations(principal.id, status)))
        log.info("[MypageController] 내 참여 내역 조회 응답 완료: userId={}, status={}", principal.id, status)
        return response
    }

    @GetMapping("/users/me/group-buy-requests")
    @Operation(summary = "내 공구 개설 요청 내역 조회")
    fun getUserGroupBuyRequests(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<ApiResponse<List<MypageGroupBuyRequestResponse>>> {
        log.info("[MypageController] 내 공구 개설 요청 조회 요청: userId={}", principal.id)
        val response = ResponseEntity.ok(ApiResponse.success(mypageService.getGroupBuyRequests(principal.id)))
        log.info("[MypageController] 내 공구 개설 요청 조회 응답 완료: userId={}", principal.id)
        return response
    }

    @GetMapping("/mypage/refunds")
    @Operation(summary = "내 환불 내역 조회")
    fun getRefunds(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<ApiResponse<List<MypageRefundResponse>>> {
        log.info("[MypageController] 내 환불 내역 조회 요청: userId={}", principal.id)
        val response = ResponseEntity.ok(ApiResponse.success(mypageService.getRefunds(principal.id)))
        log.info("[MypageController] 내 환불 내역 조회 응답 완료: userId={}", principal.id)
        return response
    }

    @GetMapping("/users/me/refunds")
    @Operation(summary = "내 환불 내역 조회 (마이페이지)")
    fun getUserRefunds(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<ApiResponse<List<MypageRefundResponse>>> {
        log.info("[MypageController] 내 환불 내역 조회(마이페이지) 요청: userId={}", principal.id)
        val response = ResponseEntity.ok(ApiResponse.success(mypageService.getRefunds(principal.id)))
        log.info("[MypageController] 내 환불 내역 조회(마이페이지) 응답 완료: userId={}", principal.id)
        return response
    }

    @GetMapping("/mypage/group-buy-requests")
    @Operation(summary = "내 공구 개설 요청 내역 조회")
    fun getGroupBuyRequests(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<ApiResponse<List<MypageGroupBuyRequestResponse>>> {
        log.info("[MypageController] 내 공구 개설 요청 조회(legacy) 요청: userId={}", principal.id)
        val response = ResponseEntity.ok(ApiResponse.success(mypageService.getGroupBuyRequests(principal.id)))
        log.info("[MypageController] 내 공구 개설 요청 조회(legacy) 응답 완료: userId={}", principal.id)
        return response
    }
}
