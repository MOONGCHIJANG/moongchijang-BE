package com.moongchijang.domain.image.presentation

import com.moongchijang.domain.image.application.S3ImageUploadService
import com.moongchijang.domain.image.application.dto.ImageDeleteRequest
import com.moongchijang.domain.image.application.dto.ImageDeleteResponse
import com.moongchijang.domain.image.application.dto.ImagePresignedUploadRequest
import com.moongchijang.domain.image.application.dto.ImagePresignedUploadResponse
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
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/images")
@RequireCurrentRole(UserRole.BUYER, UserRole.SELLER, UserRole.ADMIN)
@Tag(name = "ImageUpload", description = "이미지 업로드")
class ImageUploadController(
    private val s3ImageUploadService: S3ImageUploadService,
) {
    @PostMapping("/presigned-urls")
    @Operation(summary = "S3 이미지 업로드용 Presigned URL 발급")
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "Presigned URL 발급 성공",
                content = [Content(schema = Schema(implementation = ImagePresignedUploadResponse::class))],
            ),
            SwaggerApiResponse(responseCode = "400", description = "요청값 검증 실패"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요"),
        ]
    )
    fun issuePresignedUrls(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: ImagePresignedUploadRequest,
    ): ResponseEntity<ApiResponse<ImagePresignedUploadResponse>> {
        val response = s3ImageUploadService.issuePresignedUrls(principal.id, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping
    @Operation(summary = "S3 업로드 이미지 삭제")
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "이미지 삭제 성공",
                content = [Content(schema = Schema(implementation = ImageDeleteResponse::class))],
            ),
            SwaggerApiResponse(responseCode = "400", description = "요청값 검증 실패"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요"),
        ]
    )
    fun deleteImages(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: ImageDeleteRequest,
    ): ResponseEntity<ApiResponse<ImageDeleteResponse>> {
        val response = s3ImageUploadService.deleteImages(principal.id, request.keys)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
