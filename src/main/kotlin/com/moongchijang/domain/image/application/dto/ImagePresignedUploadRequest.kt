package com.moongchijang.domain.image.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

@Schema(description = "이미지 Presigned URL 발급 요청")
data class ImagePresignedUploadRequest(
    @field:Schema(description = "공구 ID. 생성 전 단계면 null", example = "960004")
    val groupBuyId: Long? = null,

    @field:NotEmpty
    @field:Valid
    @field:Schema(description = "발급 대상 파일 목록")
    val files: List<ImagePresignedUploadItemRequest>,
)
