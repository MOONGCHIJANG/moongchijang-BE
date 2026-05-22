package com.moongchijang.domain.notification.presentation

import com.moongchijang.domain.notification.application.NotificationQueryService
import com.moongchijang.domain.notification.application.dto.NotificationCategory
import com.moongchijang.domain.notification.application.dto.NotificationListResponse
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notification", description = "알림 목록 조회")
class NotificationController(
    private val notificationQueryService: NotificationQueryService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    @Operation(summary = "알림 목록 조회 (폴링용)")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "알림 목록 조회 성공"),
            SwaggerApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (category/cursor/limit 형식 오류)",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "로그인 필요",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            )
        ]
    )
    fun getNotifications(
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
        @RequestParam(required = false, defaultValue = "ALL") category: String,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false, defaultValue = "20") limit: Int
    ): ResponseEntity<ApiResponse<NotificationListResponse>> {
        val userId = principal?.id ?: throw CustomException(ErrorCode.INVALID_LOGIN)
        log.info(
            "[NotificationController] 알림 목록 조회 요청 수신: userId={}, category={}, hasCursor={}, limit={}",
            userId,
            category,
            cursor != null,
            limit
        )
        val response = notificationQueryService.getNotifications(
            userId = userId,
            category = NotificationCategory.from(category),
            cursor = cursor,
            limit = limit
        )
        log.info(
            "[NotificationController] 알림 목록 조회 응답 완료: userId={}, category={}, size={}, hasNext={}",
            userId,
            category,
            response.items.size,
            response.hasNext
        )

        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
