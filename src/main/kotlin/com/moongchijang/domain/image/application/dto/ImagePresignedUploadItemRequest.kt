package com.moongchijang.domain.image.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "Presigned URL 발급 대상 파일")
data class ImagePresignedUploadItemRequest(
    @field:NotNull
    @field:Schema(description = "업로드 카테고리", example = "THUMBNAIL")
    val category: ImageUploadCategory,

    @field:NotBlank
    @field:Schema(description = "원본 파일명", example = "cake.jpg")
    val fileName: String,

    @field:NotBlank
    @field:Schema(description = "파일 Content-Type", example = "image/jpeg")
    val contentType: String,
)
