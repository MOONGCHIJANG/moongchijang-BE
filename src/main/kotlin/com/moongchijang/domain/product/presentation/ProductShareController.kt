package com.moongchijang.domain.product.presentation

import com.moongchijang.domain.product.application.ProductShareService
import com.moongchijang.domain.product.application.dto.ProductShareResponse
import com.moongchijang.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product", description = "상품 조회")
class ProductShareController(
    private val productShareService: ProductShareService,
) {

    @GetMapping("/{productId}/share")
    @Operation(summary = "상품 공유 메타데이터 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "상품 공유 메타데이터 조회 성공"),
            SwaggerApiResponse(
                responseCode = "404",
                description = "상품을 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ApiResponse::class))],
            ),
        ]
    )
    fun getShareMeta(
        @PathVariable productId: Long,
    ): ResponseEntity<ApiResponse<ProductShareResponse>> {
        return ResponseEntity.ok(ApiResponse.success(productShareService.getShareMeta(productId)))
    }
}
