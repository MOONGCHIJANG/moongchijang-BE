package com.moongchijang.domain.favorite.presentation

import com.moongchijang.domain.favorite.application.WishlistCommandService
import com.moongchijang.domain.favorite.application.WishlistQueryService
import com.moongchijang.domain.favorite.application.dto.WishFilterType
import com.moongchijang.domain.favorite.application.dto.WishSortType
import com.moongchijang.domain.favorite.application.dto.WishlistPageResponse
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
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
@RequireCurrentRole(UserRole.BUYER)
@Tag(name = "Wishlist", description = "찜 추가 · 해제 · 목록")
class WishlistController(
    private val wishlistQueryService: WishlistQueryService,
    private val wishlistCommandService: WishlistCommandService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/wishlists")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "찜 목록 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ]
    )
    fun getWishlists(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @RequestParam(required = false, defaultValue = "ALL") filter: WishFilterType,
        @RequestParam(required = false, defaultValue = "false") excludeClosed: Boolean,
        @RequestParam(required = false, defaultValue = "LATEST") sort: WishSortType,
        pageable: Pageable,
    ): ResponseEntity<ApiResponse<WishlistPageResponse>> {
        val userId = principal.id
        log.info(
            "[WishlistController] 찜 목록 조회 요청 수신: userId={}, filter={}, excludeClosed={}, sort={}, page={}, size={}",
            userId, filter, excludeClosed, sort, pageable.pageNumber, pageable.pageSize
        )

        val response = wishlistQueryService.getWishlist(
            userId = userId,
            filter = filter,
            excludeClosed = excludeClosed,
            sort = sort,
            pageable = pageable,
        )

        log.info(
            "[WishlistController] 찜 목록 조회 응답 완료: userId={}, totalElements={}, totalPages={}",
            userId, response.totalElements, response.totalPages
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/group-buys/{groupBuyId}/wishlist")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "찜 추가")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "201", description = "찜 추가 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "404", description = "공구를 찾을 수 없음", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ]
    )
    fun addWishlist(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @PathVariable groupBuyId: Long,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId = principal.id
        log.info("[WishlistController] 찜 추가 요청 수신: userId={}, groupBuyId={}", userId, groupBuyId)

        wishlistCommandService.addWishlist(userId, groupBuyId)

        log.info("[WishlistController] 찜 추가 응답 완료: userId={}, groupBuyId={}", userId, groupBuyId)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success())
    }

    @DeleteMapping("/group-buys/{groupBuyId}/wishlist")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "찜 해제")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "찜 해제 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "404", description = "공구를 찾을 수 없음", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ]
    )
    fun removeWishlist(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @PathVariable groupBuyId: Long,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId = principal.id
        log.info("[WishlistController] 찜 해제 요청 수신: userId={}, groupBuyId={}", userId, groupBuyId)

        wishlistCommandService.removeWishlist(userId, groupBuyId)

        log.info("[WishlistController] 찜 해제 응답 완료: userId={}, groupBuyId={}", userId, groupBuyId)
        return ResponseEntity.ok(ApiResponse.success())
    }
}
