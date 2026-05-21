package com.moongchijang.domain.groupbuy.presentation

import com.moongchijang.domain.groupbuy.application.GroupBuyService
import com.moongchijang.domain.groupbuy.application.GroupBuyViewerService
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyDetailResponse
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedFilter
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedPageResponse
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedRequest
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyProgressItem
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyProgressResponse
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyViewerCountResponse
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyViewerHeartbeatRequest
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
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
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/group-buys")
@Tag(name = "GroupBuy", description = "공구 피드/상세/달성률")
class GroupBuyController(
    private val groupBuyService: GroupBuyService,
    private val groupBuyViewerService: GroupBuyViewerService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    @Operation(summary = "공구 탐색/목록 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "목록 조회 성공"),
            SwaggerApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (지역 필터 10개 초과, ALL+하위 동시 선택 등)",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            )
        ]
    )
    fun getFeed(
        @RequestParam(required = false, defaultValue = "ALL")
        filter: GroupBuyFeedFilter,
        @RequestParam(required = false)
        districts: List<DistrictType>?,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<GroupBuyFeedPageResponse>> {
        log.info("[GroupBuyController] 공구 피드 조회 요청 수신: filter={}, districts={}", filter, districts)

        val request = GroupBuyFeedRequest(
            filter = filter,
            districts = districts ?: emptyList(),
        )

        val response = groupBuyService.getFeed(request, pageable)
        log.info("[GroupBuyController] 공구 피드 조회 응답 완료: totalElements={}", response.totalElements)

        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{groupBuyId}")
    @Operation(summary = "공구 상세 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "공구 상세 조회 성공"),
            SwaggerApiResponse(
                responseCode = "404",
                description = "공구를 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            )
        ]
    )
    fun getDetail(
        @PathVariable groupBuyId: Long,
        @AuthenticationPrincipal principal: CustomUserPrincipal?
    ): ResponseEntity<ApiResponse<GroupBuyDetailResponse>> {
        log.info("[GroupBuyController] 공구 상세 조회 요청 수신: groupBuyId={}, userId={}", groupBuyId, principal?.id)

        val response = groupBuyService.getDetail(
            groupBuyId = groupBuyId,
            userId = principal?.id,
        )

        log.info("[GroupBuyController] 공구 상세 조회 응답 완료: groupBuyId={}", groupBuyId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{groupBuyId}/progress")
    @Operation(summary = "단일 공구 달성률 조회 (폴링용)")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "단일 공구 달성률 조회 성공"),
            SwaggerApiResponse(
                responseCode = "404",
                description = "공구를 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            )
        ]
    )
    fun getProgress(
        @PathVariable groupBuyId: Long
    ): ResponseEntity<ApiResponse<GroupBuyProgressResponse>> {
        log.info("[GroupBuyController] 공구 progress 단건 조회 요청 수신: groupBuyId={}", groupBuyId)

        val response = groupBuyService.getProgress(groupBuyId)

        log.info("[GroupBuyController] 공구 progress 단건 조회 응답 완료: groupBuyId={}", groupBuyId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/progress")
    @Operation(summary = "다건 공구 달성률 조회 (피드 갱신용)")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "다건 공구 달성률 조회 성공"),
            SwaggerApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (ids 누락/빈 목록/최대 20개 초과)",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            )
        ]
    )
    fun getProgresses(
        @RequestParam ids: List<Long>
    ): ResponseEntity<ApiResponse<List<GroupBuyProgressItem>>> {
        validateProgressIds(ids)
        val distinctIds = ids.distinct()

        log.info(
            "[GroupBuyController] 공구 progress 다건 조회 요청 수신: requestedSize={}, distinctSize={}",
            ids.size,
            distinctIds.size
        )

        val response = groupBuyService.getProgresses(distinctIds)

        log.info(
            "[GroupBuyController] 공구 progress 다건 조회 응답 완료: requestedSize={}, returnedSize={}",
            distinctIds.size,
            response.size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/{groupBuyId}/viewers/heartbeat")
    @Operation(summary = "공구 상세 조회자 heartbeat 조회/갱신")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회자 heartbeat 조회/갱신 성공"),
            SwaggerApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (viewerSessionId 누락/형식 오류)",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "공구를 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            )
        ]
    )
    fun heartbeatViewer(
        @PathVariable groupBuyId: Long,
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
        @Valid @RequestBody request: GroupBuyViewerHeartbeatRequest
    ): ResponseEntity<ApiResponse<GroupBuyViewerCountResponse>> {
        log.info(
            "[GroupBuyController] 조회자 heartbeat 요청 수신: groupBuyId={}, userId={}, viewerSessionId={}",
            groupBuyId,
            principal?.id,
            request.viewerSessionId
        )

        val response = groupBuyViewerService.heartbeat(
            groupBuyId = groupBuyId,
            userId = principal?.id,
            viewerSessionId = request.viewerSessionId
        )

        log.info(
            "[GroupBuyController] 조회자 heartbeat 응답 완료: groupBuyId={}, activeViewerCount={}",
            groupBuyId,
            response.activeViewerCount
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    private fun validateProgressIds(ids: List<Long>) {
        if (ids.isEmpty() || ids.size > MAX_PROGRESS_IDS) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }
    }

    companion object {
        private const val MAX_PROGRESS_IDS = 20
    }
}
