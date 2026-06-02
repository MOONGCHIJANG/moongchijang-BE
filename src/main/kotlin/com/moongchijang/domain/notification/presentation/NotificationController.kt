package com.moongchijang.domain.notification.presentation

import com.moongchijang.domain.notification.application.NotificationCommandService
import com.moongchijang.domain.notification.application.NotificationQueryService
import com.moongchijang.domain.notification.application.dto.NotificationCategory
import com.moongchijang.domain.notification.application.dto.NotificationListResponse
import com.moongchijang.domain.notification.application.dto.NotificationUnreadCountResponse
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
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notification", description = "알림 목록 조회 및 읽음 처리")
class NotificationController(
    private val notificationQueryService: NotificationQueryService,
    private val notificationCommandService: NotificationCommandService
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
            currentRole = principal.role,
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

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "알림 단건 읽음 처리")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "알림 단건 읽음 처리 성공"),
            SwaggerApiResponse(
                responseCode = "401",
                description = "로그인 필요",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "권한 없음 (본인 알림 아님)",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "알림을 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            )
        ]
    )
    fun markNotificationAsRead(
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
        @PathVariable notificationId: Long
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId = principal?.id ?: throw CustomException(ErrorCode.INVALID_LOGIN)
        log.info(
            "[NotificationController] 알림 단건 읽음 처리 요청 수신: userId={}, notificationId={}",
            userId,
            notificationId
        )

        notificationCommandService.markAsRead(userId = userId, notificationId = notificationId)

        log.info(
            "[NotificationController] 알림 단건 읽음 처리 응답 완료: userId={}, notificationId={}",
            userId,
            notificationId
        )
        return ResponseEntity.ok(ApiResponse.success())
    }

    @PatchMapping("/read-all")
    @Operation(summary = "알림 전체 읽음 처리")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "알림 전체 읽음 처리 성공"),
            SwaggerApiResponse(
                responseCode = "401",
                description = "로그인 필요",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            )
        ]
    )
    fun markAllNotificationsAsRead(
        @AuthenticationPrincipal principal: CustomUserPrincipal?
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId = principal?.id ?: throw CustomException(ErrorCode.INVALID_LOGIN)
        log.info("[NotificationController] 알림 전체 읽음 처리 요청 수신: userId={}", userId)

        val updatedCount = notificationCommandService.markAllAsRead(userId = userId, currentRole = principal.role)

        log.info(
            "[NotificationController] 알림 전체 읽음 처리 응답 완료: userId={}, updatedCount={}",
            userId,
            updatedCount
        )
        return ResponseEntity.ok(ApiResponse.success())
    }

    @GetMapping("/unread-count")
    @Operation(summary = "미읽음 알림 개수 조회")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "미읽음 알림 개수 조회 성공"),
            SwaggerApiResponse(
                responseCode = "401",
                description = "로그인 필요",
                content = [Content(schema = Schema(implementation = ApiResponse::class))]
            )
        ]
    )
    fun getUnreadNotificationCount(
        @AuthenticationPrincipal principal: CustomUserPrincipal?
    ): ResponseEntity<ApiResponse<NotificationUnreadCountResponse>> {
        val userId = principal?.id ?: throw CustomException(ErrorCode.INVALID_LOGIN)
        log.info("[NotificationController] 미읽음 알림 개수 조회 요청 수신: userId={}", userId)

        val response = notificationCommandService.getUnreadCount(userId = userId, currentRole = principal.role)

        log.info(
            "[NotificationController] 미읽음 알림 개수 조회 응답 완료: userId={}, count={}",
            userId,
            response.count
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
