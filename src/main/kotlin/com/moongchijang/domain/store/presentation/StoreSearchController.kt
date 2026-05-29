package com.moongchijang.domain.store.presentation

import com.moongchijang.domain.store.application.StoreSearchService
import com.moongchijang.domain.store.application.dto.StoreSearchResponse
import com.moongchijang.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/stores")
@Tag(name = "Store", description = "매장 검색 API")
class StoreSearchController(
    private val storeSearchService: StoreSearchService
) {
    private val log = LoggerFactory.getLogger(StoreSearchController::class.java)

    @GetMapping("/search")
    @Operation(summary = "매장 검색 자동완성 (네이버 Local Search API)")
    fun search(
        @RequestParam keyword: String
    ): ResponseEntity<ApiResponse<StoreSearchResponse>> {
        log.info("[StoreSearchController] 매장 검색 요청: keywordLength={}", keyword.length)
        val response = ResponseEntity.ok(ApiResponse.success(storeSearchService.search(keyword)))
        log.info("[StoreSearchController] 매장 검색 응답 완료")
        return response
    }
}
