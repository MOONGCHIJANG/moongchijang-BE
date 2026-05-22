package com.moongchijang.domain.favorite.presentation

import com.moongchijang.domain.favorite.application.WishlistQueryService
import com.moongchijang.domain.favorite.application.dto.WishFilterType
import com.moongchijang.domain.favorite.application.dto.WishSortType
import com.moongchijang.domain.favorite.application.dto.WishlistPageResponse
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/wishlists")
@Tag(name = "Wishlist", description = "찜 목록 조회")
class WishlistController(
    private val wishlistQueryService: WishlistQueryService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
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
        @RequestParam(required = false, defaultValue = "LATEST") sort: WishSortType,
        pageable: Pageable,
    ): ResponseEntity<ApiResponse<WishlistPageResponse>> {
        val userId = principal.id
        log.info(
            "[WishlistController] 찜 목록 조회 요청 수신: userId={}, filter={}, sort={}, page={}, size={}",
            userId, filter, sort, pageable.pageNumber, pageable.pageSize
        )

        val response = wishlistQueryService.getWishlist(
            userId = userId,
            filter = filter,
            sort = sort,
            pageable = pageable,
        )

        log.info(
            "[WishlistController] 찜 목록 조회 응답 완료: userId={}, totalElements={}, totalPages={}",
            userId, response.totalElements, response.totalPages
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
