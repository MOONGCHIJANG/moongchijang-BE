package com.moongchijang.domain.image.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "파일별 Presigned URL 발급 결과")
data class ImagePresignedUploadItemResponse(
    @field:Schema(description = "업로드 카테고리", example = "PRODUCT")
    val category: ImageUploadCategory,

    @field:Schema(description = "S3 Object Key", example = "dev/group-buys/960004/products/uuid.jpg")
    val key: String,

    @field:Schema(description = "S3 업로드용 Presigned URL")
    val uploadUrl: String,

    @field:Schema(description = "업로드에 사용할 HTTP Method", example = "PUT")
    val method: String = "PUT",
)
