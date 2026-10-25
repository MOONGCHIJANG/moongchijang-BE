package com.moongchijang.domain.favorite.presentation

import com.moongchijang.domain.favorite.application.ProductWishlistCommandService
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.authorization.RequireCurrentRole
import com.moongchijang.security.principal.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
@RequireCurrentRole(UserRole.BUYER)
@Tag(name = "Wishlist", description = "찜 추가 · 해제 · 목록")
class ProductWishlistController(
    private val productWishlistCommandService: ProductWishlistCommandService,
) {

    @PostMapping("/{productId}/wishlist")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "상품 찜 추가")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "201", description = "상품 찜 추가 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "404", description = "상품을 찾을 수 없음", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ]
    )
    fun addWishlist(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @PathVariable productId: Long,
    ): ResponseEntity<ApiResponse<Nothing>> {
        productWishlistCommandService.addWishlist(principal.id, productId)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success())
    }

    @DeleteMapping("/{productId}/wishlist")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "상품 찜 해제")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "상품 찜 해제 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "404", description = "상품을 찾을 수 없음", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ]
    )
    fun removeWishlist(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @PathVariable productId: Long,
    ): ResponseEntity<ApiResponse<Nothing>> {
        productWishlistCommandService.removeWishlist(principal.id, productId)
        return ResponseEntity.ok(ApiResponse.success())
    }
}
