package com.moongchijang.domain.image.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "이미지 Presigned URL 발급 응답")
data class ImagePresignedUploadResponse(
    @field:Schema(description = "발급 결과 목록")
    val items: List<ImagePresignedUploadItemResponse>,
)
