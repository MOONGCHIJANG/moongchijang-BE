package com.moongchijang.domain.participation.presentation

import com.moongchijang.domain.participation.application.ParticipationService
import com.moongchijang.domain.participation.application.dto.ParticipationCreateRequest
import com.moongchijang.domain.participation.application.dto.ParticipationCreatedResponse
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/group-buys")
@Tag(name = "Participation", description = "공구 참여")
class ParticipationController(
    private val participationService: ParticipationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/{groupBuyId}/participations")
    @Operation(summary = "공구 참여")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "201", description = "공구 참여 성공"),
            SwaggerApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (수량, 마감, 상태 등)",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "공구/사용자 없음",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            SwaggerApiResponse(
                responseCode = "409",
                description = "중복 참여/수량 부족/락 획득 실패",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            )
        ]
    )
    fun createParticipation(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @PathVariable groupBuyId: Long,
        @Valid @RequestBody request: ParticipationCreateRequest
    ): ResponseEntity<ApiResponse<ParticipationCreatedResponse>> {
        log.info(
            "[ParticipationController] 공구 참여 요청 수신: userId={}, groupBuyId={}, quantity={}",
            principal.id, groupBuyId, request.quantity
        )

        val response = participationService.createParticipation(
            userId = principal.id,
            groupBuyId = groupBuyId,
            request = request
        )

        log.info(
            "[ParticipationController] 공구 참여 응답 완료: userId={}, groupBuyId={}, participationId={}",
            principal.id, groupBuyId, response.participationId
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }
}
