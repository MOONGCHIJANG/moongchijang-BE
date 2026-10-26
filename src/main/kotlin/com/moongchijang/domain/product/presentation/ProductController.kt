package com.moongchijang.domain.product.presentation

import com.moongchijang.domain.product.application.ProductQueryService
import com.moongchijang.domain.product.application.dto.ProductDetailResponse
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
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product", description = "상품 조회")
class ProductController(
    private val productQueryService: ProductQueryService,
) {

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
