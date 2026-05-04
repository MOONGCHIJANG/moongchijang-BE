package com.moongchijang.domain.groupbuy.presentation

import com.moongchijang.domain.groupbuy.application.GroupBuyService
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedFilter
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedPageResponse
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedRequest
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/group-buys")
@Tag(name = "GroupBuy", description = "공구 피드/상세/달성률")
class GroupBuyController(
    private val groupBuyService: GroupBuyService
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
        @RequestParam(required = false)
        keyword: String?,
        pageable: Pageable
    ): ResponseEntity<ApiResponse<GroupBuyFeedPageResponse>> {
        log.info("[GroupBuyController] 공구 피드 조회 요청 수신: filter={}, districts={}, keyword={}", filter, districts, keyword)

        val request = GroupBuyFeedRequest(
            filter = filter,
            districts = districts ?: emptyList(),
            keyword = keyword
        )

        val response = groupBuyService.getFeed(request, pageable)
        log.info("[GroupBuyController] 공구 피드 조회 응답 완료: totalElements={}", response.totalElements)

        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
