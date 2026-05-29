package com.moongchijang.domain.search.presentation

import com.moongchijang.domain.search.application.SearchService
import com.moongchijang.domain.search.application.dto.SearchResponse
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Search", description = "공구 검색 (AI 자연어)")
@Validated
class SearchController(
    private val searchService: SearchService
) {

    data class SearchRequest(@field:NotBlank val keyword: String)

    @PostMapping
    @Operation(summary = "검색어 입력 및 AI 분석 (1.1.4-1)")
    fun search(
        @Valid @RequestBody request: SearchRequest,
        @AuthenticationPrincipal principal: CustomUserPrincipal?
    ): ResponseEntity<ApiResponse<SearchResponse>> {
        val response = ResponseEntity.ok(ApiResponse.success(searchService.search(request.keyword, principal?.id)))
        return response
    }

    @GetMapping("/recent")
    @Operation(summary = "최근 검색어 목록 조회 (1.1.4-10)")
    fun getRecentSearches(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<ApiResponse<List<String>>> {
        val response = ResponseEntity.ok(ApiResponse.success(searchService.getHistory(principal.id)))
        return response
    }

    @DeleteMapping("/recent")
    @Operation(summary = "최근 검색어 전체 삭제")
    fun clearRecentSearches(
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<ApiResponse<Nothing>> {
        searchService.clearHistory(principal.id)
        val response = ResponseEntity.ok(ApiResponse.success())
        return response
    }

    @DeleteMapping("/recent/{keyword}")
    @Operation(summary = "최근 검색어 단건 삭제")
    fun deleteRecentSearch(
        @PathVariable keyword: String,
        @AuthenticationPrincipal principal: CustomUserPrincipal
    ): ResponseEntity<ApiResponse<Nothing>> {
        searchService.deleteHistory(principal.id, keyword)
        val response = ResponseEntity.ok(ApiResponse.success())
        return response
    }
}
