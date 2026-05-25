package com.moongchijang.domain.mypage.presentation

import com.moongchijang.domain.mypage.application.MyPageParticipationQueryService
import com.moongchijang.domain.participation.application.dto.CancelledOrRefundedParticipationPageResponse
import com.moongchijang.domain.participation.application.dto.InProgressParticipationPageResponse
import com.moongchijang.domain.participation.application.dto.MyPageTabCountsResponse
import com.moongchijang.domain.participation.application.dto.PickupCompletedParticipationPageResponse
import com.moongchijang.domain.participation.application.dto.PickupWaitingParticipationPageResponse
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "MyPage", description = "마이페이지 API")
class MyPageParticipationController(
    private val myPageParticipationQueryService: MyPageParticipationQueryService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/tabs/counts")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "마이페이지 탭별 건수 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))])
        ]
    )
    fun getTabCounts(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<ApiResponse<MyPageTabCountsResponse>> {
        val userId = principal.id
        val response = myPageParticipationQueryService.getTabCounts(userId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/participations/in-progress")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "진행 중 탭 참여 공구 목록 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))])
        ]
    )
    fun getInProgressParticipations(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<InProgressParticipationPageResponse>> {
        val userId = principal.id
        log.info(
            "[MyPageParticipationController] 진행 중 참여 내역 조회 요청 수신: userId={}, page={}, size={}",
            userId,
            pageable.pageNumber,
            pageable.pageSize
        )

        val response = myPageParticipationQueryService.getInProgressParticipations(userId, pageable)

        log.info(
            "[MyPageParticipationController] 진행 중 참여 내역 조회 응답 완료: userId={}, totalElements={}",
            userId,
            response.totalElements
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/participations/pickup-waiting")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "픽업 대기 탭 참여 완료 공구 이력 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))])
        ]
    )
    fun getPickupWaitingParticipations(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<PickupWaitingParticipationPageResponse>> {
        val userId = principal.id
        log.info(
            "[MyPageParticipationController] 픽업 대기 참여 내역 조회 요청 수신: userId={}, page={}, size={}",
            userId,
            pageable.pageNumber,
            pageable.pageSize
        )

        val response = myPageParticipationQueryService.getPickupWaitingParticipations(userId, pageable)

        log.info(
            "[MyPageParticipationController] 픽업 대기 참여 내역 조회 응답 완료: userId={}, totalElements={}",
            userId,
            response.totalElements
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/participations/pickup-completed")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "픽업 완료 탭 참여 공구 목록 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))])
        ]
    )
    fun getPickupCompletedParticipations(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<PickupCompletedParticipationPageResponse>> {
        val userId = principal.id
        val response = myPageParticipationQueryService.getPickupCompletedParticipations(userId, pageable)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/participations/cancelled-or-refunded")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "환불/취소 탭 참여 공구 목록 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))])
        ]
    )
    fun getCancelledOrRefundedParticipations(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<CancelledOrRefundedParticipationPageResponse>> {
        val userId = principal.id
        val response = myPageParticipationQueryService.getCancelledOrRefundedParticipations(userId, pageable)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
