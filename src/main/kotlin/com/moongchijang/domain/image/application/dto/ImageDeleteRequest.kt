package com.moongchijang.domain.image.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

@Schema(description = "이미지 삭제 요청")
data class ImageDeleteRequest(
    @field:NotEmpty
    @field:Schema(description = "삭제할 S3 key 목록")
    val keys: List<String>,
)
