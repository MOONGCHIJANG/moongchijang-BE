package com.moongchijang.presentation.store

import com.moongchijang.application.store.StoreSearchService
import com.moongchijang.application.store.dto.StoreSearchResponse
import com.moongchijang.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/stores")
@Tag(name = "GroupBuyRequest", description = "공구 개설 요청 (소비자)")
class StoreSearchController(
    private val storeSearchService: StoreSearchService
) {

    @GetMapping("/search")
    @Operation(summary = "매장 검색 자동완성 (네이버 Local Search API)")
    fun search(
        @RequestParam keyword: String
    ): ResponseEntity<ApiResponse<StoreSearchResponse>> {
        return ResponseEntity.ok(ApiResponse.success(storeSearchService.search(keyword)))
    }
}
