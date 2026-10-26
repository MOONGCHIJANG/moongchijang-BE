package com.moongchijang.domain.product.presentation

import com.moongchijang.domain.product.application.ProductFeedQueryService
import com.moongchijang.domain.product.application.ProductQueryService
import com.moongchijang.domain.product.application.dto.ProductDetailResponse
import com.moongchijang.domain.product.application.dto.ProductFeedRequest
import com.moongchijang.domain.product.application.dto.ProductFeedResponse
import com.moongchijang.domain.product.domain.repository.ProductFeedSort
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product", description = "상품 조회")
class ProductController(
    private val productFeedQueryService: ProductFeedQueryService,
    private val productQueryService: ProductQueryService,
) {

    @GetMapping
    @Operation(summary = "홈 피드 상품 목록 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "상품 목록 조회 성공"),
            SwaggerApiResponse(
                responseCode = "400",
                description = "잘못된 필터 또는 커서",
                content = [Content(schema = Schema(implementation = ApiResponse::class))],
            ),
        ]
    )
    fun getFeed(
        @RequestParam(required = false) categoryCodes: Set<String>?,
        @RequestParam(required = false) districts: Set<DistrictType>?,
        @RequestParam(defaultValue = "false") pickupThisWeek: Boolean,
        @RequestParam(defaultValue = "false") smallQuantity: Boolean,
        @RequestParam(defaultValue = "false") newOnly: Boolean,
        @RequestParam(defaultValue = "LATEST") sort: ProductFeedSort,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") limit: Int,
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
    ): ResponseEntity<ApiResponse<ProductFeedResponse>> {
        val response = productFeedQueryService.getFeed(
            request = ProductFeedRequest(
                categoryCodes = categoryCodes.orEmpty(),
                districts = districts.orEmpty(),
                pickupThisWeek = pickupThisWeek,
                smallQuantity = smallQuantity,
                newOnly = newOnly,
                sort = sort,
                cursor = cursor,
                limit = limit,
            ),
            userId = principal?.id,
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{productId}")
    @Operation(summary = "상품 상세 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "상품 상세 조회 성공"),
            SwaggerApiResponse(
                responseCode = "404",
                description = "상품을 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ApiResponse::class))],
            ),
        ]
    )
    fun getDetail(
        @PathVariable productId: Long,
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
    ): ResponseEntity<ApiResponse<ProductDetailResponse>> {
        return ResponseEntity.ok(ApiResponse.success(productQueryService.getDetail(productId, principal?.id)))
    }
}
